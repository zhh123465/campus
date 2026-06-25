package com.campusforum.ai.workspace;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.ai.service.AiService;
import com.campusforum.ai.workspace.domain.*;
import com.campusforum.ai.workspace.mapper.*;
import com.campusforum.common.BusinessException;
import com.campusforum.infra.StorageService;
import com.campusforum.infra.security.MimeTypeValidator;
import com.campusforum.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AiWorkspaceServiceSecurityTest {

    private AiWorkspaceService service;
    private MockedStatic<StpUtil> stpUtilMock;
    private long currentUserId;

    private Map<String, AiPluginInstall> pluginInstalls;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);

        Map<String, AiAgent> agents = new LinkedHashMap<>();
        Map<String, AiPlugin> plugins = new LinkedHashMap<>();
        Map<String, AiKnowledgeBase> knowledgeBases = new LinkedHashMap<>();
        Map<String, AiKnowledgeDocument> documents = new LinkedHashMap<>();
        Map<String, AiKnowledgeChunk> chunks = new LinkedHashMap<>();
        Map<String, AiIngestTask> tasks = new LinkedHashMap<>();
        Map<String, AiConversation> conversations = new LinkedHashMap<>();
        Map<String, AiConversationMessage> messages = new LinkedHashMap<>();
        Map<String, AiAgentFavorite> agentFavorites = new LinkedHashMap<>();
        pluginInstalls = new LinkedHashMap<>();
        Map<String, AiKbFavorite> kbFavorites = new LinkedHashMap<>();
        Map<String, AiKbQaPair> qaPairs = new LinkedHashMap<>();

        AiAgentMapper agentMapper = mapper(AiAgentMapper.class, agents);
        AiPluginMapper pluginMapper = mapper(AiPluginMapper.class, plugins);
        AiKnowledgeBaseMapper knowledgeBaseMapper = mapper(AiKnowledgeBaseMapper.class, knowledgeBases);
        AiKnowledgeDocumentMapper documentMapper = mapper(AiKnowledgeDocumentMapper.class, documents);
        AiKnowledgeChunkMapper chunkMapper = mapper(AiKnowledgeChunkMapper.class, chunks);
        AiIngestTaskMapper taskMapper = mapper(AiIngestTaskMapper.class, tasks);
        AiConversationMapper conversationMapper = mapper(AiConversationMapper.class, conversations);
        AiConversationMessageMapper messageMapper = mapper(AiConversationMessageMapper.class, messages);
        AiAgentFavoriteMapper agentFavoriteMapper = mapper(AiAgentFavoriteMapper.class, agentFavorites);
        AiPluginInstallMapper pluginInstallMapper = mapper(AiPluginInstallMapper.class, pluginInstalls);
        AiKbFavoriteMapper kbFavoriteMapper = mapper(AiKbFavoriteMapper.class, kbFavorites);
        AiKbQaPairMapper qaPairMapper = mapper(AiKbQaPairMapper.class, qaPairs);

        when(pluginInstallMapper.deleteFlag(anyLong(), anyLong(), anyString())).thenAnswer(inv -> {
            long userId = inv.getArgument(1);
            String pluginId = inv.getArgument(2);
            pluginInstalls.values().removeIf(row -> row.getUserId().equals(userId) && row.getPluginId().equals(pluginId));
            return 1;
        });
        when(kbFavoriteMapper.deleteFlag(anyLong(), anyLong(), anyString())).thenAnswer(inv -> {
            long userId = inv.getArgument(1);
            String kbId = inv.getArgument(2);
            kbFavorites.values().removeIf(row -> row.getUserId().equals(userId) && row.getKnowledgeBaseId().equals(kbId));
            return 1;
        });
        when(agentFavoriteMapper.deleteFlag(anyLong(), anyLong(), anyString())).thenAnswer(inv -> {
            long userId = inv.getArgument(1);
            String agentId = inv.getArgument(2);
            agentFavorites.values().removeIf(row -> row.getUserId().equals(userId) && row.getAgentId().equals(agentId));
            return 1;
        });

        AiService aiService = mock(AiService.class);
        when(aiService.chat(anyList(), any(), any())).thenReturn("ok");

        AiKnowledgeSearchClient searchClient = mock(AiKnowledgeSearchClient.class);
        when(searchClient.search(anyString(), anyLong(), anyList(), anyInt())).thenReturn(List.of(
                AiKnowledgeSearchClient.Citation.builder()
                        .knowledgeBaseId("kb_001")
                        .documentId("doc_001")
                        .chunkId("chunk_001")
                        .title("Public note")
                        .snippet("Relevant public note")
                        .score(0.91)
                        .build()
        ));

        AiWorkspaceRagProperties ragProperties = new AiWorkspaceRagProperties();
        ragProperties.setTopK(3);
        ragProperties.setMaxContextChars(4000);

        service = new AiWorkspaceService(
                aiService,
                mock(StorageService.class),
                mock(MimeTypeValidator.class),
                searchClient,
                ragProperties,
                new ObjectMapper(),
                agentMapper,
                pluginMapper,
                knowledgeBaseMapper,
                documentMapper,
                chunkMapper,
                taskMapper,
                conversationMapper,
                messageMapper,
                agentFavoriteMapper,
                pluginInstallMapper,
                kbFavoriteMapper,
                qaPairMapper,
                "pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif,webp,md,markdown"
        );
        service.seedDefaults();

        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::isLogin).thenAnswer(inv -> currentUserId > 0);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenAnswer(inv -> currentUserId);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
        TenantContext.clear();
    }

    @Test
    void shouldHidePrivateKnowledgeBaseFromOtherUsers() {
        loginAs(10L);
        String privateKbId = id(service.createKnowledgeBase(Map.of(
                "name", "Private KB",
                "visibility", "private"
        )));

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
    void shouldReturnRagCitationsOnWorkspaceMessage() {
        loginAs(10L);
        String conversationId = id(service.createConversation(Map.of(
                "title", "rag chat",
                "knowledgeBaseIds", List.of("kb_001")
        )));

        Map<String, Object> result = service.sendMessage(conversationId, Map.of("content", "hello"));

        @SuppressWarnings("unchecked")
        Map<String, Object> assistant = (Map<String, Object>) result.get("assistantMessage");
        assertThat(assistant.get("content")).isEqualTo("ok");
        assertThat((List<?>) assistant.get("citations")).hasSize(1);
        assertThat(((Map<?, ?>) ((List<?>) assistant.get("citations")).get(0)).get("chunkId")).isEqualTo("chunk_001");
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

    private void loginAs(long userId) {
        currentUserId = userId;
    }

    private void logout() {
        currentUserId = 0;
    }

    private <T, M extends BaseMapper<T>> M mapper(Class<M> mapperType, Map<String, T> store) {
        M mapper = mock(mapperType);
        when(mapper.insert(org.mockito.ArgumentMatchers.<T>any())).thenAnswer(inv -> {
            T row = inv.getArgument(0);
            store.put(key(row), row);
            return 1;
        });
        when(mapper.updateById(org.mockito.ArgumentMatchers.<T>any())).thenAnswer(inv -> {
            T row = inv.getArgument(0);
            store.put(key(row), row);
            return 1;
        });
        when(mapper.selectById(any())).thenAnswer(inv -> {
            Object id = inv.getArgument(0);
            return store.get(String.valueOf(id));
        });
        when(mapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(store.values()));
        when(mapper.selectCount(any())).thenAnswer(inv -> (long) store.size());
        when(mapper.delete(any())).thenReturn(1);
        return mapper;
    }

    private static String key(Object row) {
        Object id = invoke(row, "getId");
        if (id != null) return String.valueOf(id);
        Object tenantId = invoke(row, "getTenantId");
        Object userId = invoke(row, "getUserId");
        Object agentId = invoke(row, "getAgentId");
        Object pluginId = invoke(row, "getPluginId");
        Object kbId = invoke(row, "getKnowledgeBaseId");
        return String.join(":", String.valueOf(tenantId), String.valueOf(userId),
                String.valueOf(agentId), String.valueOf(pluginId), String.valueOf(kbId));
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
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
