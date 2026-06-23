package com.campusforum.ai.workspace;

import cn.dev33.satoken.stp.StpUtil;
import com.campusforum.ai.service.AiService;
import com.campusforum.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AiWorkspaceServiceSecurityTest {

    @TempDir
    Path tempDir;

    private AiWorkspaceService service;
    private MockedStatic<StpUtil> stpUtilMock;
    private long currentUserId;

    @BeforeEach
    void setUp() {
        AiService aiService = mock(AiService.class);
        when(aiService.chat(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("ok");
        service = new AiWorkspaceService(aiService, tempDir.resolve("workspace.json").toString());

        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::isLogin).thenAnswer(inv -> currentUserId > 0);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenAnswer(inv -> currentUserId);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void shouldHidePrivateKnowledgeBaseFromOtherUsers() {
        loginAs(10L);
        String privateKbId = id(service.createKnowledgeBase(Map.of(
                "name", "Private KB",
                "visibility", "private"
        )));
        service.uploadDocuments(privateKbId, new MockMultipartFile[]{
                new MockMultipartFile("files", "secret.md", "text/markdown", "secret".getBytes())
        }, "secret", "auto");

        loginAs(20L);

        assertThat(items(service.listKnowledgeBases(null, null, "all", null, "recent", 1, 20)))
                .extracting(item -> item.get("id"))
                .doesNotContain(privateKbId);
        assertThatThrownBy(() -> service.listDocuments(privateKbId))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.knowledgeUsage(privateKbId))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.favoriteKnowledgeBase(privateKbId, true))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.shareKnowledgeBase(privateKbId, Map.of()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldOnlyExposeReadableKnowledgeBasesToAgentAndConversation() {
        loginAs(10L);
        String privateKbId = id(service.createKnowledgeBase(Map.of(
                "name", "Owner KB",
                "visibility", "private"
        )));
        String agentId = id(service.createAgent(Map.of(
                "name", "Owner Agent",
                "prompt", "private prompt",
                "knowledgeBaseIds", List.of(privateKbId)
        )));

        loginAs(20L);

        assertThatThrownBy(() -> service.getAgent(agentId))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.createConversation(Map.of(
                "title", "bad",
                "knowledgeBaseIds", List.of(privateKbId)
        ))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.createConversation(Map.of(
                "title", "bad",
                "agentId", agentId
        ))).isInstanceOf(BusinessException.class);
        assertThat(items(service.listAgents(null, null, "recommend", null, null, 1, 20)))
                .extracting(item -> item.get("id"))
                .doesNotContain(agentId);
    }

    @Test
    void shouldKeepSeededSharedKnowledgeBasePublicButNotOwnedByGuest() {
        logout();

        List<Map<String, Object>> visible = items(service.listKnowledgeBases(null, null, "all", null, "recent", 1, 20));

        assertThat(visible)
                .extracting(item -> item.get("id"))
                .contains("kb_001")
                .doesNotContain("kb_002");
        assertThat(visible.stream()
                .filter(item -> "kb_001".equals(item.get("id")))
                .findFirst()
                .orElseThrow()
                .get("isMine")).isEqualTo(false);
    }

    @Test
    void shouldHidePendingPluginFromOtherUsersAndBlockInstall() {
        loginAs(10L);
        String pluginId = id(service.publishPlugin(Map.of(
                "name", "Owner Plugin",
                "endpoint", "https://example.test/plugin"
        )));

        loginAs(20L);

        assertThat(items(service.listPlugins(null, null, "all", "comprehensive", 1, 20)))
                .extracting(item -> item.get("id"))
                .doesNotContain(pluginId);
        assertThat(service.pluginRankings())
                .extracting(item -> item.get("id"))
                .doesNotContain(pluginId);
        assertThat(service.latestPlugins())
                .extracting(item -> item.get("id"))
                .doesNotContain(pluginId);
        assertThatThrownBy(() -> service.installPlugin(pluginId, true))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.createConversation(Map.of(
                "title", "bad",
                "pluginIds", List.of(pluginId)
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldUpdatePluginInstallCountIdempotently() {
        loginAs(10L);

        long initialCount = number(firstPlugin("plugin_weather").get("installCount"));

        service.installPlugin("plugin_weather", true);
        assertThat(number(firstPlugin("plugin_weather").get("installCount"))).isEqualTo(initialCount + 1);

        service.installPlugin("plugin_weather", true);
        assertThat(number(firstPlugin("plugin_weather").get("installCount"))).isEqualTo(initialCount + 1);

        service.installPlugin("plugin_weather", false);
        assertThat(number(firstPlugin("plugin_weather").get("installCount"))).isEqualTo(initialCount);

        service.installPlugin("plugin_weather", false);
        assertThat(number(firstPlugin("plugin_weather").get("installCount"))).isEqualTo(initialCount);
    }

    @Test
    void shouldRejectFeedbackForOtherUsersMessages() {
        loginAs(10L);
        String conversationId = id(service.createConversation(Map.of("title", "owner chat")));
        String messageId = id(service.sendMessage(conversationId, Map.of("content", "hello")).get("assistantMessage"));

        loginAs(20L);

        assertThatThrownBy(() -> service.feedback(messageId, Map.of("helpful", true)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldKeepConversationOwnershipAfterJsonReload() {
        loginAs(10L);
        String conversationId = id(service.createConversation(Map.of("title", "persisted chat")));

        stpUtilMock.close();
        AiService aiService = mock(AiService.class);
        when(aiService.chat(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("ok");
        service = new AiWorkspaceService(aiService, tempDir.resolve("workspace.json").toString());
        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::isLogin).thenAnswer(inv -> currentUserId > 0);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenAnswer(inv -> currentUserId);
        loginAs(10L);

        assertThat(items(service.listConversations(1, 20)))
                .extracting(item -> item.get("id"))
                .contains(conversationId);
    }

    private void loginAs(long userId) {
        currentUserId = userId;
    }

    private void logout() {
        currentUserId = 0;
    }

    private static String id(Map<String, Object> item) {
        return String.valueOf(item.get("id"));
    }

    @SuppressWarnings("unchecked")
    private static String id(Object item) {
        return id((Map<String, Object>) item);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("items");
    }

    private Map<String, Object> firstPlugin(String pluginId) {
        return items(service.listPlugins(null, null, "all", "comprehensive", 1, 20)).stream()
                .filter(item -> pluginId.equals(item.get("id")))
                .findFirst()
                .orElseThrow();
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }
}
