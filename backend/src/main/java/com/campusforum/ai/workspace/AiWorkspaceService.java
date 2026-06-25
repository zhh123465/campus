package com.campusforum.ai.workspace;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusforum.ai.service.AiService;
import com.campusforum.ai.workspace.domain.*;
import com.campusforum.ai.workspace.mapper.*;
import com.campusforum.common.BusinessException;
import com.campusforum.common.ErrorCode;
import com.campusforum.infra.StorageService;
import com.campusforum.infra.security.MimeTypeValidator;
import com.campusforum.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiWorkspaceService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final AiService aiService;
    private final StorageService storageService;
    private final MimeTypeValidator mimeTypeValidator;
    private final AiKnowledgeSearchClient searchClient;
    private final AiWorkspaceRagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final Set<String> allowedExtensions;

    private final AiAgentMapper agentMapper;
    private final AiPluginMapper pluginMapper;
    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiIngestTaskMapper taskMapper;
    private final AiConversationMapper conversationMapper;
    private final AiConversationMessageMapper messageMapper;
    private final AiAgentFavoriteMapper agentFavoriteMapper;
    private final AiPluginInstallMapper pluginInstallMapper;
    private final AiKbFavoriteMapper kbFavoriteMapper;
    private final AiKbQaPairMapper qaPairMapper;

    public AiWorkspaceService(AiService aiService,
                              StorageService storageService,
                              MimeTypeValidator mimeTypeValidator,
                              AiKnowledgeSearchClient searchClient,
                              AiWorkspaceRagProperties ragProperties,
                              ObjectMapper objectMapper,
                              AiAgentMapper agentMapper,
                              AiPluginMapper pluginMapper,
                              AiKnowledgeBaseMapper knowledgeBaseMapper,
                              AiKnowledgeDocumentMapper documentMapper,
                              AiKnowledgeChunkMapper chunkMapper,
                              AiIngestTaskMapper taskMapper,
                              AiConversationMapper conversationMapper,
                              AiConversationMessageMapper messageMapper,
                              AiAgentFavoriteMapper agentFavoriteMapper,
                              AiPluginInstallMapper pluginInstallMapper,
                              AiKbFavoriteMapper kbFavoriteMapper,
                              AiKbQaPairMapper qaPairMapper,
                              @Value("${upload.allowed-extensions:pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif,webp,md,markdown}") String allowedExtensionsConfig) {
        this.aiService = aiService;
        this.storageService = storageService;
        this.mimeTypeValidator = mimeTypeValidator;
        this.searchClient = searchClient;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.agentMapper = agentMapper;
        this.pluginMapper = pluginMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.taskMapper = taskMapper;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.agentFavoriteMapper = agentFavoriteMapper;
        this.pluginInstallMapper = pluginInstallMapper;
        this.kbFavoriteMapper = kbFavoriteMapper;
        this.qaPairMapper = qaPairMapper;
        this.allowedExtensions = Arrays.stream(allowedExtensionsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostConstruct
    public void seedDefaults() {
        try {
            seedAgents();
            seedPlugins();
            seedKnowledgeBases();
        } catch (Exception e) {
            log.warn("AI workspace default seed skipped: {}", e.getMessage());
        }
    }

    public Map<String, Object> listAgents(String keyword, String category, String sort,
                                          Boolean mine, Boolean favorite, int page, int pageSize) {
        long userId = userIdOrGuest();
        List<AiAgent> rows = agentMapper.selectList(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getTenantId, tenantId())
                .eq(AiAgent::getDeleted, 0));
        Set<String> favorites = favoriteAgentIds(userId);
        List<Map<String, Object>> list = rows.stream()
                .filter(item -> canReadAgent(item, userId))
                .filter(item -> matches(keyword, item.getName(), item.getDescription(), item.getCategory()))
                .filter(item -> blank(category) || Objects.equals(item.getCategory(), category))
                .filter(item -> !Boolean.TRUE.equals(mine) || isOwner(item.getOwnerId(), userId))
                .filter(item -> !Boolean.TRUE.equals(favorite) || favorites.contains(item.getId()))
                .map(item -> agentView(item, userId, favorites))
                .collect(Collectors.toCollection(ArrayList::new));
        sortAgents(list, sort);
        return page(list, page, pageSize);
    }

    @Transactional
    public Map<String, Object> createAgent(Map<String, Object> body) {
        long userId = requireUser();
        List<String> kbIds = requireReadableKnowledgeBases(list(body.get("knowledgeBaseIds")), userId);
        List<String> pluginIds = requireReadablePlugins(list(body.get("pluginIds")), userId);
        AiAgent item = new AiAgent();
        item.setId("agent_" + shortId());
        item.setTenantId(tenantId());
        item.setOwnerId(userId);
        item.setName(str(body.get("name"), "Untitled Agent"));
        item.setDescription(str(body.get("description"), ""));
        item.setCategory(str(body.get("category"), "General"));
        item.setModel(str(body.get("model"), "deepseek-v4-flash"));
        item.setPrompt(str(body.get("prompt"), ""));
        item.setAbilities(json(list(body.get("abilities"))));
        item.setKnowledgeBaseIds(json(kbIds));
        item.setPluginIds(json(pluginIds));
        item.setTags(json(list(body.get("tags"))));
        item.setAvatar(str(body.get("avatar"), ""));
        item.setColor(str(body.get("color"), "#18c7a7"));
        item.setUserCount(0L);
        item.setRating(BigDecimal.valueOf(5.0));
        stampNew(item);
        agentMapper.insert(item);
        return agentView(item, userId, Set.of());
    }

    public Map<String, Object> getAgent(String agentId) {
        long userId = userIdOrGuest();
        return agentView(requireReadableAgent(agentId, userId), userId, favoriteAgentIds(userId));
    }

    @Transactional
    public Map<String, Object> updateAgent(String agentId, Map<String, Object> body) {
        long userId = requireUser();
        AiAgent item = requireReadableAgent(agentId, userId);
        requireOwner(item.getOwnerId(), userId);
        if (body.containsKey("name")) item.setName(str(body.get("name"), item.getName()));
        if (body.containsKey("description")) item.setDescription(str(body.get("description"), ""));
        if (body.containsKey("category")) item.setCategory(str(body.get("category"), item.getCategory()));
        if (body.containsKey("model")) item.setModel(str(body.get("model"), item.getModel()));
        if (body.containsKey("prompt")) item.setPrompt(str(body.get("prompt"), ""));
        if (body.containsKey("abilities")) item.setAbilities(json(list(body.get("abilities"))));
        if (body.containsKey("knowledgeBaseIds")) item.setKnowledgeBaseIds(json(requireReadableKnowledgeBases(list(body.get("knowledgeBaseIds")), userId)));
        if (body.containsKey("pluginIds")) item.setPluginIds(json(requireReadablePlugins(list(body.get("pluginIds")), userId)));
        if (body.containsKey("tags")) item.setTags(json(list(body.get("tags"))));
        if (body.containsKey("avatar")) item.setAvatar(str(body.get("avatar"), ""));
        if (body.containsKey("color")) item.setColor(str(body.get("color"), item.getColor()));
        item.setUpdatedAt(now());
        agentMapper.updateById(item);
        return agentView(item, userId, favoriteAgentIds(userId));
    }

    @Transactional
    public Map<String, Object> favoriteAgent(String agentId, boolean favorite) {
        long userId = requireUser();
        requireReadableAgent(agentId, userId);
        boolean exists = agentFavoriteMapper.selectCount(new LambdaQueryWrapper<AiAgentFavorite>()
                .eq(AiAgentFavorite::getTenantId, tenantId())
                .eq(AiAgentFavorite::getUserId, userId)
                .eq(AiAgentFavorite::getAgentId, agentId)) > 0;
        if (favorite && !exists) {
            AiAgentFavorite row = new AiAgentFavorite();
            row.setTenantId(tenantId());
            row.setUserId(userId);
            row.setAgentId(agentId);
            agentFavoriteMapper.insert(row);
        } else if (!favorite && exists) {
            agentFavoriteMapper.deleteFlag(tenantId(), userId, agentId);
        }
        return Map.of("id", agentId, "isFavorite", favorite);
    }

    @Transactional
    public Map<String, Object> useAgent(String agentId) {
        long userId = requireUser();
        AiAgent item = requireReadableAgent(agentId, userId);
        item.setUserCount(nvl(item.getUserCount()) + 1);
        item.setUpdatedAt(now());
        agentMapper.updateById(item);
        return Map.of(
                "agent", agentView(item, userId, favoriteAgentIds(userId)),
                "context", Map.of(
                        "model", str(item.getModel(), "deepseek-v4-flash"),
                        "prompt", str(item.getPrompt(), ""),
                        "abilities", jsonList(item.getAbilities()),
                        "knowledgeBaseIds", readableKnowledgeBases(jsonList(item.getKnowledgeBaseIds()), userId),
                        "pluginIds", readablePlugins(jsonList(item.getPluginIds()), userId)
                )
        );
    }

    public Map<String, Object> listPlugins(String keyword, String category, String tab,
                                           String sort, int page, int pageSize) {
        long userId = userIdOrGuest();
        Set<String> installs = installedPluginIds(userId);
        List<Map<String, Object>> list = pluginMapper.selectList(new LambdaQueryWrapper<AiPlugin>()
                        .eq(AiPlugin::getTenantId, tenantId())
                        .eq(AiPlugin::getDeleted, 0)).stream()
                .filter(item -> canReadPlugin(item, userId))
                .filter(item -> matches(keyword, item.getName(), item.getDescription(), item.getCategory()))
                .filter(item -> blank(category) || Objects.equals(item.getCategory(), category))
                .filter(item -> pluginTab(item, tab))
                .map(item -> pluginView(item, installs))
                .collect(Collectors.toCollection(ArrayList::new));
        sortPlugins(list, sort);
        return page(list, page, pageSize);
    }

    public List<Map<String, Object>> pluginRankings() {
        long userId = userIdOrGuest();
        Set<String> installs = installedPluginIds(userId);
        return pluginMapper.selectList(new LambdaQueryWrapper<AiPlugin>()
                        .eq(AiPlugin::getTenantId, tenantId())
                        .eq(AiPlugin::getDeleted, 0)).stream()
                .filter(item -> canReadPlugin(item, userId))
                .map(item -> pluginView(item, installs))
                .sorted(Comparator.comparingLong(item -> -number(item.get("usageCount"))))
                .limit(10)
                .toList();
    }

    public List<Map<String, Object>> latestPlugins() {
        long userId = userIdOrGuest();
        Set<String> installs = installedPluginIds(userId);
        return pluginMapper.selectList(new LambdaQueryWrapper<AiPlugin>()
                        .eq(AiPlugin::getTenantId, tenantId())
                        .eq(AiPlugin::getDeleted, 0)).stream()
                .filter(item -> canReadPlugin(item, userId))
                .map(item -> pluginView(item, installs))
                .sorted(Comparator.comparing(item -> str(item.get("createdAt"), ""), Comparator.reverseOrder()))
                .limit(10)
                .toList();
    }

    @Transactional
    public Map<String, Object> installPlugin(String pluginId, boolean installed) {
        long userId = requireUser();
        AiPlugin plugin = requireReadablePlugin(pluginId, userId);
        boolean exists = pluginInstallMapper.selectCount(new LambdaQueryWrapper<AiPluginInstall>()
                .eq(AiPluginInstall::getTenantId, tenantId())
                .eq(AiPluginInstall::getUserId, userId)
                .eq(AiPluginInstall::getPluginId, pluginId)) > 0;
        if (installed && !exists) {
            AiPluginInstall row = new AiPluginInstall();
            row.setTenantId(tenantId());
            row.setUserId(userId);
            row.setPluginId(pluginId);
            pluginInstallMapper.insert(row);
            plugin.setInstallCount(nvl(plugin.getInstallCount()) + 1);
            pluginMapper.updateById(plugin);
        } else if (!installed && exists) {
            pluginInstallMapper.deleteFlag(tenantId(), userId, pluginId);
            plugin.setInstallCount(Math.max(0L, nvl(plugin.getInstallCount()) - 1));
            pluginMapper.updateById(plugin);
        }
        return Map.of("id", pluginId, "isInstalled", installed);
    }

    @Transactional
    public Map<String, Object> invokePlugin(String pluginId, Map<String, Object> body) {
        long userId = requireUser();
        AiPlugin plugin = requireReadablePlugin(pluginId, userId);
        if (!installedPluginIds(userId).contains(pluginId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        plugin.setUsageCount(nvl(plugin.getUsageCount()) + 1);
        pluginMapper.updateById(plugin);
        return Map.of(
                "pluginId", pluginId,
                "conversationId", str(body.get("conversationId"), ""),
                "agentId", str(body.get("agentId"), ""),
                "output", pluginOutput(pluginId, objectMap(body.get("input"))),
                "invokedAt", OffsetDateTime.now().toString()
        );
    }

    public Map<String, Object> applyPluginDeveloper(Map<String, Object> body) {
        return Map.of(
                "applicationId", "dev_app_" + shortId(),
                "status", "pending",
                "developerName", str(body.get("developerName"), ""),
                "submittedAt", OffsetDateTime.now().toString()
        );
    }

    @Transactional
    public Map<String, Object> publishPlugin(Map<String, Object> body) {
        long userId = requireUser();
        AiPlugin item = new AiPlugin();
        item.setId("plugin_" + shortId());
        item.setTenantId(tenantId());
        item.setOwnerId(userId);
        item.setName(str(body.get("name"), "Untitled Plugin"));
        item.setDescription(str(body.get("description"), ""));
        item.setCategory(str(body.get("category"), "Productivity"));
        item.setIcon(str(body.get("icon"), ""));
        item.setColor(str(body.get("color"), "#38bdf8"));
        item.setUsageCount(0L);
        item.setInstallCount(0L);
        item.setRating(BigDecimal.valueOf(5.0));
        item.setIsOfficial(0);
        item.setIsFeatured(0);
        item.setPermissions(json(list(body.get("permissions"))));
        item.setInputSchema(json(objectMap(body.get("inputSchema"))));
        item.setOutputSchema(json(objectMap(body.get("outputSchema"))));
        item.setEndpoint(str(body.get("endpoint"), ""));
        item.setReviewStatus("pending");
        stampNew(item);
        pluginMapper.insert(item);
        return pluginView(item, Set.of());
    }

    public Map<String, Object> listKnowledgeBases(String keyword, String category, String tab,
                                                  String type, String sort, int page, int pageSize) {
        long userId = userIdOrGuest();
        Set<String> favorites = favoriteKbIds(userId);
        List<Map<String, Object>> list = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<AiKnowledgeBase>()
                        .eq(AiKnowledgeBase::getTenantId, tenantId())
                        .eq(AiKnowledgeBase::getDeleted, 0)).stream()
                .filter(item -> canReadKnowledgeBase(item, userId))
                .filter(item -> matches(keyword, item.getName(), item.getDescription(), item.getCategory(), item.getType()))
                .filter(item -> blank(category) || Objects.equals(item.getCategory(), category))
                .filter(item -> blank(type) || Objects.equals(item.getType(), type))
                .filter(item -> kbTab(item, tab, userId, favorites))
                .map(item -> kbView(item, userId, favorites))
                .collect(Collectors.toCollection(ArrayList::new));
        sortKnowledgeBases(list, sort);
        return page(list, page, pageSize);
    }

    public Map<String, Object> knowledgeStats() {
        long userId = requireUser();
        List<AiKnowledgeBase> active = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<AiKnowledgeBase>()
                        .eq(AiKnowledgeBase::getTenantId, tenantId())
                        .eq(AiKnowledgeBase::getDeleted, 0)).stream()
                .filter(item -> canReadKnowledgeBase(item, userId))
                .toList();
        return Map.of(
                "knowledgeBaseCount", active.size(),
                "documentCount", active.stream().mapToLong(item -> nvl(item.getDocumentCount())).sum(),
                "vectorCount", active.stream().mapToLong(item -> nvl(item.getVectorCount())).sum(),
                "storageUsedBytes", active.stream().mapToLong(item -> nvl(item.getStorageBytes())).sum(),
                "storageLimitBytes", 53687091200L
        );
    }

    @Transactional
    public Map<String, Object> createKnowledgeBase(Map<String, Object> body) {
        long userId = requireUser();
        AiKnowledgeBase item = new AiKnowledgeBase();
        item.setId("kb_" + shortId());
        item.setTenantId(tenantId());
        item.setOwnerId(userId);
        item.setName(str(body.get("name"), "Untitled Knowledge Base"));
        item.setDescription(str(body.get("description"), ""));
        item.setCategory(str(body.get("category"), "General"));
        item.setType(str(body.get("type"), str(body.get("category"), "General")));
        item.setVisibility(str(body.get("visibility"), "private"));
        item.setDocumentCount(0L);
        item.setVectorCount(0L);
        item.setStorageBytes(0L);
        stampNew(item);
        knowledgeBaseMapper.insert(item);
        return kbView(item, userId, Set.of());
    }

    @Transactional
    public Map<String, Object> updateKnowledgeBase(String knowledgeBaseId, Map<String, Object> body) {
        long userId = requireUser();
        AiKnowledgeBase item = requireKnowledgeBase(knowledgeBaseId);
        requireOwner(item.getOwnerId(), userId);
        if (body.containsKey("name")) item.setName(str(body.get("name"), item.getName()));
        if (body.containsKey("description")) item.setDescription(str(body.get("description"), ""));
        if (body.containsKey("category")) item.setCategory(str(body.get("category"), item.getCategory()));
        if (body.containsKey("type")) item.setType(str(body.get("type"), item.getType()));
        if (body.containsKey("visibility")) item.setVisibility(str(body.get("visibility"), item.getVisibility()));
        item.setUpdatedAt(now());
        knowledgeBaseMapper.updateById(item);
        return kbView(item, userId, favoriteKbIds(userId));
    }

    @Transactional
    public Map<String, Object> deleteKnowledgeBase(String knowledgeBaseId) {
        long userId = requireUser();
        AiKnowledgeBase item = requireKnowledgeBase(knowledgeBaseId);
        requireOwner(item.getOwnerId(), userId);
        item.setDeleted(1);
        item.setUpdatedAt(now());
        knowledgeBaseMapper.updateById(item);
        return Map.of("id", knowledgeBaseId, "deleted", true);
    }

    @Transactional
    public Map<String, Object> favoriteKnowledgeBase(String knowledgeBaseId, boolean favorite) {
        long userId = requireUser();
        requireReadableKnowledgeBase(knowledgeBaseId, userId);
        boolean exists = kbFavoriteMapper.selectCount(new LambdaQueryWrapper<AiKbFavorite>()
                .eq(AiKbFavorite::getTenantId, tenantId())
                .eq(AiKbFavorite::getUserId, userId)
                .eq(AiKbFavorite::getKnowledgeBaseId, knowledgeBaseId)) > 0;
        if (favorite && !exists) {
            AiKbFavorite row = new AiKbFavorite();
            row.setTenantId(tenantId());
            row.setUserId(userId);
            row.setKnowledgeBaseId(knowledgeBaseId);
            kbFavoriteMapper.insert(row);
        } else if (!favorite && exists) {
            kbFavoriteMapper.deleteFlag(tenantId(), userId, knowledgeBaseId);
        }
        return Map.of("id", knowledgeBaseId, "isFavorite", favorite);
    }

    public Map<String, Object> shareKnowledgeBase(String knowledgeBaseId, Map<String, Object> body) {
        long userId = requireUser();
        AiKnowledgeBase kb = requireKnowledgeBase(knowledgeBaseId);
        requireOwner(kb.getOwnerId(), userId);
        return Map.of(
                "shareId", "share_" + shortId(),
                "knowledgeBaseId", knowledgeBaseId,
                "targetUserIds", list(body.get("targetUserIds")),
                "permission", str(body.get("permission"), "read"),
                "url", "/ai/knowledge-bases/" + knowledgeBaseId,
                "expiresAt", OffsetDateTime.now().plusDays(7).toString()
        );
    }

    @Transactional
    public Map<String, Object> uploadDocuments(String knowledgeBaseId, MultipartFile[] files,
                                               String tags, String parseMode) {
        long userId = requireUser();
        AiKnowledgeBase kb = requireKnowledgeBase(knowledgeBaseId);
        requireOwner(kb.getOwnerId(), userId);
        List<String> documentIds = new ArrayList<>();
        String taskId = "task_" + shortId();
        AiIngestTask task = newTask(taskId, knowledgeBaseId, userId);
        taskMapper.insert(task);

        int uploaded = 0;
        int failed = 0;
        long storageBytes = 0;
        long chunkCount = 0;
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                try {
                    AiKnowledgeDocument doc = ingestOne(kb, file, tags, parseMode, userId);
                    documentIds.add(doc.getId());
                    uploaded++;
                    storageBytes += nvl(doc.getStorageBytes());
                    chunkCount += nvl(doc.getChunkCount());
                } catch (Exception e) {
                    failed++;
                    log.warn("AI knowledge document ingest failed: {}", e.getMessage());
                }
            }
        }
        kb.setDocumentCount(nvl(kb.getDocumentCount()) + uploaded);
        kb.setVectorCount(nvl(kb.getVectorCount()) + chunkCount);
        kb.setStorageBytes(nvl(kb.getStorageBytes()) + storageBytes);
        kb.setUpdatedAt(now());
        knowledgeBaseMapper.updateById(kb);

        task.setUploaded(uploaded);
        task.setFailed(failed);
        task.setDocumentIds(json(documentIds));
        task.setProgress(100);
        task.setStatus(failed > 0 && uploaded == 0 ? "failed" : "ready");
        task.setMessage(uploaded + " 个文档已解析并写入检索索引");
        task.setErrorMessage(failed > 0 ? failed + " 个文档导入失败" : null);
        task.setUpdatedAt(now());
        taskMapper.updateById(task);
        return taskView(task);
    }

    public List<Map<String, Object>> listDocuments(String knowledgeBaseId) {
        requireReadableKnowledgeBase(knowledgeBaseId, requireUser());
        return documentMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getTenantId, tenantId())
                        .eq(AiKnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(AiKnowledgeDocument::getDeleted, 0)
                        .orderByDesc(AiKnowledgeDocument::getCreatedAt)).stream()
                .map(this::docView)
                .toList();
    }

    @Transactional
    public Map<String, Object> deleteDocument(String knowledgeBaseId, String documentId) {
        long userId = requireUser();
        AiKnowledgeBase kb = requireKnowledgeBase(knowledgeBaseId);
        requireOwner(kb.getOwnerId(), userId);
        AiKnowledgeDocument doc = requireDocument(knowledgeBaseId, documentId);
        doc.setDeleted(1);
        doc.setUpdatedAt(now());
        documentMapper.updateById(doc);
        chunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getTenantId, tenantId())
                .eq(AiKnowledgeChunk::getDocumentId, documentId));
        searchClient.deleteChunks(documentId);
        if (!blank(doc.getStorageKey())) {
            storageService.delete(doc.getStorageKey());
        }
        kb.setDocumentCount(Math.max(0L, nvl(kb.getDocumentCount()) - 1));
        kb.setVectorCount(Math.max(0L, nvl(kb.getVectorCount()) - nvl(doc.getChunkCount())));
        kb.setStorageBytes(Math.max(0L, nvl(kb.getStorageBytes()) - nvl(doc.getStorageBytes())));
        kb.setUpdatedAt(now());
        knowledgeBaseMapper.updateById(kb);
        return Map.of("id", documentId, "deleted", true);
    }

    public Map<String, Object> ingestTask(String taskId) {
        long userId = requireUser();
        AiIngestTask task = taskMapper.selectById(taskId);
        if (task == null || nvl(task.getDeleted()) == 1 || !Objects.equals(task.getTenantId(), tenantId()) || !Objects.equals(task.getOwnerId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return taskView(task);
    }

    @Transactional
    public Map<String, Object> createQaPair(String knowledgeBaseId, Map<String, Object> body) {
        long userId = requireUser();
        AiKnowledgeBase kb = requireKnowledgeBase(knowledgeBaseId);
        requireOwner(kb.getOwnerId(), userId);
        AiKbQaPair pair = new AiKbQaPair();
        pair.setId("qa_" + shortId());
        pair.setTenantId(tenantId());
        pair.setKnowledgeBaseId(knowledgeBaseId);
        pair.setOwnerId(userId);
        pair.setQuestion(str(body.get("question"), ""));
        pair.setAnswer(str(body.get("answer"), ""));
        pair.setTags(json(list(body.get("tags"))));
        stampNew(pair);
        qaPairMapper.insert(pair);
        return Map.of("id", pair.getId(), "question", pair.getQuestion(), "answer", pair.getAnswer(), "tags", jsonList(pair.getTags()));
    }

    public Map<String, Object> knowledgeUsage(String knowledgeBaseId) {
        long userId = requireUser();
        AiKnowledgeBase kb = requireReadableKnowledgeBase(knowledgeBaseId, userId);
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("knowledgeBaseId", knowledgeBaseId);
        usage.put("documentCount", nvl(kb.getDocumentCount()));
        usage.put("vectorCount", nvl(kb.getVectorCount()));
        usage.put("storageBytes", nvl(kb.getStorageBytes()));
        usage.put("lastIndexedAt", documentMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getTenantId, tenantId())
                        .eq(AiKnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(AiKnowledgeDocument::getDeleted, 0)
                        .orderByDesc(AiKnowledgeDocument::getIndexedAt)
                        .last("LIMIT 1")).stream().findFirst().map(AiKnowledgeDocument::getIndexedAt).orElse(null));
        return usage;
    }

    @Transactional
    public Map<String, Object> createConversation(Map<String, Object> body) {
        long userId = requireUser();
        String agentId = str(body.get("agentId"), "");
        if (!blank(agentId)) requireReadableAgent(agentId, userId);
        List<String> pluginIds = requireReadablePlugins(list(body.get("pluginIds")), userId);
        List<String> kbIds = requireReadableKnowledgeBases(list(body.get("knowledgeBaseIds")), userId);
        AiConversation item = new AiConversation();
        item.setId("chat_" + shortId());
        item.setTenantId(tenantId());
        item.setOwnerId(userId);
        item.setTitle(str(body.get("title"), "新的对话"));
        item.setAgentId(agentId);
        item.setPluginIds(json(pluginIds));
        item.setKnowledgeBaseIds(json(kbIds));
        item.setModel(str(body.get("model"), ""));
        stampNew(item);
        conversationMapper.insert(item);
        return conversationView(item);
    }

    public Map<String, Object> listConversations(int page, int pageSize) {
        long userId = requireUser();
        List<Map<String, Object>> list = conversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getTenantId, tenantId())
                        .eq(AiConversation::getOwnerId, userId)
                        .eq(AiConversation::getDeleted, 0)
                        .orderByDesc(AiConversation::getUpdatedAt)).stream()
                .map(this::conversationView)
                .collect(Collectors.toCollection(ArrayList::new));
        return page(list, page, pageSize);
    }

    public List<Map<String, Object>> conversationMessages(String conversationId) {
        long userId = requireUser();
        requireConversationOwner(conversationId, userId);
        return messageMapper.selectList(new LambdaQueryWrapper<AiConversationMessage>()
                        .eq(AiConversationMessage::getTenantId, tenantId())
                        .eq(AiConversationMessage::getConversationId, conversationId)
                        .eq(AiConversationMessage::getDeleted, 0)
                        .orderByAsc(AiConversationMessage::getCreatedAt)).stream()
                .map(this::messageView)
                .toList();
    }

    @Transactional
    public Map<String, Object> sendMessage(String conversationId, Map<String, Object> body) {
        long userId = requireUser();
        AiConversation conversation = requireConversationOwner(conversationId, userId);
        String content = str(body.get("content"), "");
        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "消息内容不能为空");
        }
        String model = str(body.get("model"), str(conversation.getModel(), ""));
        List<String> kbIds = jsonList(conversation.getKnowledgeBaseIds());
        List<String> pluginIds = jsonList(conversation.getPluginIds());

        AiConversationMessage userMessage = newMessage(conversation, "user", content, model, pluginIds, kbIds, null);
        messageMapper.insert(userMessage);

        List<AiKnowledgeSearchClient.Citation> citations = searchClient.search(content, tenantId(), kbIds, ragProperties.getTopK());
        String context = buildConversationContext(conversation.getAgentId(), pluginIds, kbIds, citations);
        String reply = aiService.chat(List.of(new AiService.ChatMessage("user", content)), context, model);
        AiConversationMessage assistant = newMessage(conversation, "assistant", reply, model, pluginIds, kbIds, json(citations));
        messageMapper.insert(assistant);

        conversation.setTitle(titleFrom(content, conversation.getTitle()));
        conversation.setUpdatedAt(now());
        conversationMapper.updateById(conversation);
        return Map.of(
                "userMessage", messageView(userMessage),
                "assistantMessage", messageView(assistant),
                "citations", citations.stream().map(this::citationView).toList()
        );
    }

    @Transactional
    public Map<String, Object> feedback(String messageId, Map<String, Object> body) {
        long userId = requireUser();
        AiConversationMessage msg = messageMapper.selectById(messageId);
        if (msg == null || nvl(msg.getDeleted()) == 1 || !Objects.equals(msg.getTenantId(), tenantId()) || !Objects.equals(msg.getOwnerId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        msg.setFeedbackHelpful(Boolean.TRUE.equals(body.get("helpful")) ? 1 : 0);
        msg.setFeedbackComment(str(body.get("comment"), ""));
        msg.setUpdatedAt(now());
        messageMapper.updateById(msg);
        return Map.of("id", messageId, "feedbackSaved", true);
    }

    private AiKnowledgeDocument ingestOne(AiKnowledgeBase kb, MultipartFile file, String tags, String parseMode, long userId) throws Exception {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "文件名不能为空");
        }
        String ext = extension(originalName);
        if (!allowedExtensions.contains(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "不支持的文件类型：" + ext);
        }
        mimeTypeValidator.validate(file, ext);
        String storageKey;
        try (InputStream in = file.getInputStream()) {
            storageKey = storageService.upload(in, originalName, file.getContentType(), file.getSize());
        }
        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setId("doc_" + shortId());
        doc.setTenantId(tenantId());
        doc.setKnowledgeBaseId(kb.getId());
        doc.setOwnerId(userId);
        doc.setFileName(originalName);
        doc.setFileSize(file.getSize());
        doc.setFileType(ext);
        doc.setStorageKey(storageKey);
        doc.setTags(json(splitTags(tags)));
        doc.setParseMode(blank(parseMode) ? "auto" : parseMode);
        doc.setStatus("processing");
        doc.setChunkCount(0L);
        doc.setStorageBytes(file.getSize());
        stampNew(doc);
        documentMapper.insert(doc);

        try (InputStream parseIn = file.getInputStream()) {
            String text = truncate(extractText(parseIn), ragProperties.getMaxDocumentChars());
            if (text.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "文档未解析出有效文本");
            }
            List<String> chunks = chunk(text, ragProperties.getChunkSize(), ragProperties.getChunkOverlap());
            List<AiKnowledgeChunk> rows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk();
                chunk.setId("chunk_" + shortId());
                chunk.setTenantId(tenantId());
                chunk.setKnowledgeBaseId(kb.getId());
                chunk.setDocumentId(doc.getId());
                chunk.setOwnerId(userId);
                chunk.setChunkIndex(i);
                chunk.setTitle(originalName);
                chunk.setContent(chunks.get(i));
                chunk.setContentHash(sha256(chunks.get(i)));
                chunk.setStatus("ready");
                stampNew(chunk);
                chunkMapper.insert(chunk);
                rows.add(chunk);
            }
            searchClient.indexChunks(rows, originalName, splitTags(tags), kb.getVisibility());
            doc.setStatus("ready");
            doc.setChunkCount((long) rows.size());
            doc.setIndexedAt(now());
            doc.setUpdatedAt(now());
            documentMapper.updateById(doc);
            return doc;
        } catch (Exception e) {
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            doc.setUpdatedAt(now());
            documentMapper.updateById(doc);
            throw e;
        }
    }

    private String extractText(InputStream inputStream) throws Exception {
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());
        return handler.toString().replaceAll("\\s+", " ").strip();
    }

    private List<String> chunk(String text, int chunkSize, int overlap) {
        int safeSize = Math.max(200, chunkSize);
        int safeOverlap = Math.min(Math.max(0, overlap), safeSize / 2);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + safeSize);
            chunks.add(text.substring(start, end).strip());
            if (end >= text.length()) break;
            start = Math.max(0, end - safeOverlap);
        }
        return chunks.stream().filter(item -> !item.isBlank()).toList();
    }

    private AiConversationMessage newMessage(AiConversation conversation, String role, String content, String model,
                                             List<String> pluginIds, List<String> kbIds, String citations) {
        AiConversationMessage msg = new AiConversationMessage();
        msg.setId("msg_" + shortId());
        msg.setTenantId(tenantId());
        msg.setConversationId(conversation.getId());
        msg.setOwnerId(conversation.getOwnerId());
        msg.setRole(role);
        msg.setContent(content);
        msg.setModel(model);
        msg.setAgentId(str(conversation.getAgentId(), ""));
        msg.setPluginIds(json(pluginIds));
        msg.setKnowledgeBaseIds(json(kbIds));
        msg.setCitations(citations);
        stampNew(msg);
        return msg;
    }

    private String buildConversationContext(String agentId, List<String> pluginIds, List<String> kbIds,
                                            List<AiKnowledgeSearchClient.Citation> citations) {
        StringBuilder context = new StringBuilder();
        context.append("你叫小青，是青云阁网站的知识库检索增强助手。回答必须优先依据【知识库检索资料】；如果资料不足，请明确说明无法从现有资料确认，不要编造。\n\n");
        if (!blank(agentId)) {
            AiAgent agent = agentMapper.selectById(agentId);
            if (agent != null && !blank(agent.getPrompt())) {
                context.append("【智能体提示词】\n").append(agent.getPrompt()).append("\n\n");
            }
        }
        if (!pluginIds.isEmpty()) {
            context.append("【可用插件】\n").append(String.join(", ", pluginIds)).append("\n\n");
        }
        if (!kbIds.isEmpty()) {
            context.append("【已选择知识库】\n").append(String.join(", ", kbIds)).append("\n\n");
        }
        context.append("【知识库检索资料】\n");
        if (citations == null || citations.isEmpty()) {
            context.append("未召回到相关资料。\n");
        } else {
            for (int i = 0; i < citations.size(); i++) {
                AiKnowledgeSearchClient.Citation citation = citations.get(i);
                context.append("[").append(i + 1).append("] ")
                        .append(citation.getTitle()).append("\n")
                        .append(citation.getSnippet()).append("\n")
                        .append("chunkId: ").append(citation.getChunkId()).append("\n\n");
            }
        }
        return truncate(context.toString(), ragProperties.getMaxContextChars());
    }

    private Map<String, Object> agentView(AiAgent item, long userId, Set<String> favorites) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("name", item.getName());
        view.put("description", item.getDescription());
        view.put("category", item.getCategory());
        view.put("model", item.getModel());
        view.put("prompt", item.getPrompt());
        view.put("abilities", jsonList(item.getAbilities()));
        view.put("knowledgeBaseIds", readableKnowledgeBases(jsonList(item.getKnowledgeBaseIds()), userId));
        view.put("pluginIds", readablePlugins(jsonList(item.getPluginIds()), userId));
        view.put("tags", jsonList(item.getTags()));
        view.put("avatar", item.getAvatar());
        view.put("color", item.getColor());
        view.put("userCount", nvl(item.getUserCount()));
        view.put("rating", item.getRating());
        view.put("createdAt", item.getCreatedAt());
        view.put("updatedAt", item.getUpdatedAt());
        view.put("isMine", isOwner(item.getOwnerId(), userId));
        view.put("isFavorite", favorites.contains(item.getId()));
        return view;
    }

    private Map<String, Object> pluginView(AiPlugin item, Set<String> installs) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("name", item.getName());
        view.put("description", item.getDescription());
        view.put("category", item.getCategory());
        view.put("icon", item.getIcon());
        view.put("color", item.getColor());
        view.put("usageCount", nvl(item.getUsageCount()));
        view.put("installCount", nvl(item.getInstallCount()));
        view.put("rating", item.getRating());
        view.put("isOfficial", nvl(item.getIsOfficial()) == 1);
        view.put("isFeatured", nvl(item.getIsFeatured()) == 1);
        view.put("permissions", jsonList(item.getPermissions()));
        view.put("inputSchema", jsonMap(item.getInputSchema()));
        view.put("outputSchema", jsonMap(item.getOutputSchema()));
        view.put("endpoint", item.getEndpoint());
        view.put("reviewStatus", item.getReviewStatus());
        view.put("createdAt", item.getCreatedAt());
        view.put("isInstalled", installs.contains(item.getId()));
        return view;
    }

    private Map<String, Object> kbView(AiKnowledgeBase item, long userId, Set<String> favorites) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("name", item.getName());
        view.put("description", item.getDescription());
        view.put("category", item.getCategory());
        view.put("type", item.getType());
        view.put("visibility", item.getVisibility());
        view.put("documentCount", nvl(item.getDocumentCount()));
        view.put("vectorCount", nvl(item.getVectorCount()));
        view.put("storageBytes", nvl(item.getStorageBytes()));
        view.put("owner", isOwner(item.getOwnerId(), userId) ? "Mine" : "Shared");
        view.put("createdAt", item.getCreatedAt());
        view.put("updatedAt", item.getUpdatedAt());
        view.put("isMine", isOwner(item.getOwnerId(), userId));
        view.put("isFavorite", favorites.contains(item.getId()));
        return view;
    }

    private Map<String, Object> docView(AiKnowledgeDocument item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("fileName", item.getFileName());
        view.put("fileSize", nvl(item.getFileSize()));
        view.put("fileType", item.getFileType());
        view.put("tags", jsonList(item.getTags()));
        view.put("parseMode", item.getParseMode());
        view.put("status", item.getStatus());
        view.put("chunkCount", nvl(item.getChunkCount()));
        view.put("storageBytes", nvl(item.getStorageBytes()));
        view.put("indexedAt", item.getIndexedAt());
        view.put("errorMessage", item.getErrorMessage());
        view.put("createdAt", item.getCreatedAt());
        return view;
    }

    private Map<String, Object> taskView(AiIngestTask task) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("taskId", task.getId());
        view.put("knowledgeBaseId", task.getKnowledgeBaseId());
        view.put("status", task.getStatus());
        view.put("progress", nvl(task.getProgress()));
        view.put("uploaded", nvl(task.getUploaded()));
        view.put("failed", nvl(task.getFailed()));
        view.put("documentIds", jsonList(task.getDocumentIds()));
        view.put("message", task.getMessage());
        view.put("errorMessage", task.getErrorMessage());
        view.put("updatedAt", task.getUpdatedAt());
        return view;
    }

    private Map<String, Object> conversationView(AiConversation item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("title", item.getTitle());
        view.put("agentId", item.getAgentId());
        view.put("pluginIds", jsonList(item.getPluginIds()));
        view.put("knowledgeBaseIds", jsonList(item.getKnowledgeBaseIds()));
        view.put("model", item.getModel());
        view.put("createdAt", item.getCreatedAt());
        view.put("updatedAt", item.getUpdatedAt());
        view.put("messageCount", messageMapper.selectCount(new LambdaQueryWrapper<AiConversationMessage>()
                .eq(AiConversationMessage::getTenantId, tenantId())
                .eq(AiConversationMessage::getConversationId, item.getId())
                .eq(AiConversationMessage::getDeleted, 0)));
        return view;
    }

    private Map<String, Object> messageView(AiConversationMessage item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("role", item.getRole());
        view.put("content", item.getContent());
        view.put("model", item.getModel());
        view.put("agentId", item.getAgentId());
        view.put("pluginIds", jsonList(item.getPluginIds()));
        view.put("knowledgeBaseIds", jsonList(item.getKnowledgeBaseIds()));
        view.put("citations", jsonListOfMaps(item.getCitations()));
        view.put("createdAt", item.getCreatedAt());
        return view;
    }

    private Map<String, Object> citationView(AiKnowledgeSearchClient.Citation citation) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("knowledgeBaseId", citation.getKnowledgeBaseId());
        view.put("documentId", citation.getDocumentId());
        view.put("chunkId", citation.getChunkId());
        view.put("title", citation.getTitle());
        view.put("snippet", citation.getSnippet());
        view.put("score", citation.getScore());
        return view;
    }

    private AiAgent requireReadableAgent(String agentId, long userId) {
        AiAgent item = agentMapper.selectById(agentId);
        if (item == null || nvl(item.getDeleted()) == 1 || !Objects.equals(item.getTenantId(), tenantId()) || !canReadAgent(item, userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private AiPlugin requireReadablePlugin(String pluginId, long userId) {
        AiPlugin item = pluginMapper.selectById(pluginId);
        if (item == null || nvl(item.getDeleted()) == 1 || !Objects.equals(item.getTenantId(), tenantId()) || !canReadPlugin(item, userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private AiKnowledgeBase requireKnowledgeBase(String knowledgeBaseId) {
        AiKnowledgeBase item = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (item == null || nvl(item.getDeleted()) == 1 || !Objects.equals(item.getTenantId(), tenantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private AiKnowledgeBase requireReadableKnowledgeBase(String knowledgeBaseId, long userId) {
        AiKnowledgeBase item = requireKnowledgeBase(knowledgeBaseId);
        if (!canReadKnowledgeBase(item, userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private AiKnowledgeDocument requireDocument(String knowledgeBaseId, String documentId) {
        AiKnowledgeDocument item = documentMapper.selectById(documentId);
        if (item == null || nvl(item.getDeleted()) == 1 || !Objects.equals(item.getTenantId(), tenantId()) || !Objects.equals(item.getKnowledgeBaseId(), knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private AiConversation requireConversationOwner(String conversationId, long userId) {
        AiConversation item = conversationMapper.selectById(conversationId);
        if (item == null || nvl(item.getDeleted()) == 1 || !Objects.equals(item.getTenantId(), tenantId()) || !Objects.equals(item.getOwnerId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return item;
    }

    private List<String> requireReadableKnowledgeBases(List<String> ids, long userId) {
        ids.forEach(id -> requireReadableKnowledgeBase(id, userId));
        return ids;
    }

    private List<String> requireReadablePlugins(List<String> ids, long userId) {
        ids.forEach(id -> requireReadablePlugin(id, userId));
        return ids;
    }

    private List<String> readableKnowledgeBases(List<String> ids, long userId) {
        return ids.stream().filter(id -> {
            try {
                requireReadableKnowledgeBase(id, userId);
                return true;
            } catch (BusinessException e) {
                return false;
            }
        }).toList();
    }

    private List<String> readablePlugins(List<String> ids, long userId) {
        return ids.stream().filter(id -> {
            try {
                requireReadablePlugin(id, userId);
                return true;
            } catch (BusinessException e) {
                return false;
            }
        }).toList();
    }

    private boolean canReadAgent(AiAgent item, long userId) {
        return isOwner(item.getOwnerId(), userId) || nvl(item.getOwnerId()) == 0L;
    }

    private boolean canReadPlugin(AiPlugin item, long userId) {
        return isOwner(item.getOwnerId(), userId)
                || nvl(item.getOwnerId()) == 0L
                || "approved".equalsIgnoreCase(str(item.getReviewStatus(), ""));
    }

    private boolean canReadKnowledgeBase(AiKnowledgeBase item, long userId) {
        return isOwner(item.getOwnerId(), userId)
                || "shared".equalsIgnoreCase(str(item.getVisibility(), ""))
                || "public".equalsIgnoreCase(str(item.getVisibility(), ""));
    }

    private boolean isOwner(Long ownerId, long userId) {
        return userId > 0 && Objects.equals(ownerId, userId);
    }

    private void requireOwner(Long ownerId, long userId) {
        if (!isOwner(ownerId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private long requireUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return StpUtil.getLoginIdAsLong();
    }

    private long userIdOrGuest() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 0L;
    }

    private Long tenantId() {
        return TenantContext.getTenantId() == null ? 1L : TenantContext.getTenantId();
    }

    private Set<String> favoriteAgentIds(long userId) {
        if (userId <= 0) return Set.of();
        return agentFavoriteMapper.selectList(new LambdaQueryWrapper<AiAgentFavorite>()
                        .eq(AiAgentFavorite::getTenantId, tenantId())
                        .eq(AiAgentFavorite::getUserId, userId)).stream()
                .map(AiAgentFavorite::getAgentId)
                .collect(Collectors.toSet());
    }

    private Set<String> installedPluginIds(long userId) {
        if (userId <= 0) return Set.of();
        return pluginInstallMapper.selectList(new LambdaQueryWrapper<AiPluginInstall>()
                        .eq(AiPluginInstall::getTenantId, tenantId())
                        .eq(AiPluginInstall::getUserId, userId)).stream()
                .map(AiPluginInstall::getPluginId)
                .collect(Collectors.toSet());
    }

    private Set<String> favoriteKbIds(long userId) {
        if (userId <= 0) return Set.of();
        return kbFavoriteMapper.selectList(new LambdaQueryWrapper<AiKbFavorite>()
                        .eq(AiKbFavorite::getTenantId, tenantId())
                        .eq(AiKbFavorite::getUserId, userId)).stream()
                .map(AiKbFavorite::getKnowledgeBaseId)
                .collect(Collectors.toSet());
    }

    private boolean pluginTab(AiPlugin item, String tab) {
        if ("official".equals(tab)) return nvl(item.getIsOfficial()) == 1;
        if ("featured".equals(tab)) return nvl(item.getIsFeatured()) == 1;
        return true;
    }

    private boolean kbTab(AiKnowledgeBase item, String tab, long userId, Set<String> favorites) {
        if ("mine".equals(tab)) return isOwner(item.getOwnerId(), userId);
        if ("shared".equals(tab)) return "shared".equals(item.getVisibility()) || "public".equals(item.getVisibility());
        if ("favorite".equals(tab)) return favorites.contains(item.getId());
        return true;
    }

    private void sortAgents(List<Map<String, Object>> list, String sort) {
        if ("latest".equals(sort)) list.sort(Comparator.comparing(item -> str(item.get("createdAt"), ""), Comparator.reverseOrder()));
        else if ("popular".equals(sort)) list.sort(Comparator.comparingLong(item -> -number(item.get("userCount"))));
        else list.sort(Comparator.comparingDouble(item -> -dbl(item.get("rating"))));
    }

    private void sortPlugins(List<Map<String, Object>> list, String sort) {
        if ("rating".equals(sort)) list.sort(Comparator.comparingDouble(item -> -dbl(item.get("rating"))));
        else if ("usage".equals(sort)) list.sort(Comparator.comparingLong(item -> -number(item.get("usageCount"))));
        else list.sort(Comparator.comparingLong(item -> -number(item.get("installCount"))));
    }

    private void sortKnowledgeBases(List<Map<String, Object>> list, String sort) {
        if ("docs".equals(sort)) list.sort(Comparator.comparingLong(item -> -number(item.get("documentCount"))));
        else if ("vectors".equals(sort)) list.sort(Comparator.comparingLong(item -> -number(item.get("vectorCount"))));
        else list.sort(Comparator.comparing(item -> str(item.get("updatedAt"), ""), Comparator.reverseOrder()));
    }

    private Map<String, Object> page(List<Map<String, Object>> list, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize <= 0 ? 10 : pageSize));
        int from = Math.min(list.size(), (safePage - 1) * safeSize);
        int to = Math.min(list.size(), from + safeSize);
        return Map.of("items", list.subList(from, to), "total", list.size());
    }

    private boolean matches(String keyword, String... values) {
        if (blank(keyword)) return true;
        String q = keyword.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private Map<String, Object> pluginOutput(String pluginId, Map<String, Object> input) {
        if ("plugin_weather".equals(pluginId)) {
            return Map.of("city", str(input.get("city"), "Unknown"), "summary", "Weather lookup completed");
        }
        if ("plugin_translate".equals(pluginId)) {
            return Map.of("text", str(input.get("text"), ""), "result", "Translation queued");
        }
        return Map.of("result", "Plugin invoked", "input", input);
    }

    private void seedAgents() {
        if (agentMapper.selectById("agent_study") != null) return;
        insertAgent("agent_study", "学习规划助手", "拆解学习目标，规划复习节奏", "Study", List.of("规划", "复习"), "#18c7a7");
        insertAgent("agent_writer", "写作润色助手", "帮助梳理表达和修改文稿", "Writing", List.of("写作", "润色"), "#7c3aed");
    }

    private void insertAgent(String id, String name, String description, String category, List<String> tags, String color) {
        AiAgent item = new AiAgent();
        item.setId(id);
        item.setTenantId(1L);
        item.setOwnerId(0L);
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setModel("deepseek-v4-flash");
        item.setPrompt(description);
        item.setAbilities(json(tags));
        item.setKnowledgeBaseIds(json(List.of()));
        item.setPluginIds(json(List.of()));
        item.setTags(json(tags));
        item.setAvatar("");
        item.setColor(color);
        item.setUserCount(0L);
        item.setRating(BigDecimal.valueOf(4.8));
        stampNew(item);
        agentMapper.insert(item);
    }

    private void seedPlugins() {
        if (pluginMapper.selectById("plugin_weather") != null) return;
        insertPlugin("plugin_weather", "天气查询", "查询城市天气摘要", "Tool", "#38bdf8", true, true, List.of("network"));
        insertPlugin("plugin_translate", "翻译助手", "翻译短文本", "Language", "#f59e0b", true, false, List.of("text"));
    }

    private void insertPlugin(String id, String name, String description, String category, String color,
                              boolean official, boolean featured, List<String> permissions) {
        AiPlugin item = new AiPlugin();
        item.setId(id);
        item.setTenantId(1L);
        item.setOwnerId(0L);
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setIcon("");
        item.setColor(color);
        item.setUsageCount(0L);
        item.setInstallCount(0L);
        item.setRating(BigDecimal.valueOf(4.7));
        item.setIsOfficial(official ? 1 : 0);
        item.setIsFeatured(featured ? 1 : 0);
        item.setPermissions(json(permissions));
        item.setInputSchema(json(Map.of()));
        item.setOutputSchema(json(Map.of()));
        item.setEndpoint("");
        item.setReviewStatus("approved");
        stampNew(item);
        pluginMapper.insert(item);
    }

    private void seedKnowledgeBases() {
        if (knowledgeBaseMapper.selectById("kb_001") != null) return;
        insertKnowledgeBase("kb_001", "公共学习资料", "全站共享的学习资料索引", "Study", "General", "shared", 0L);
        insertKnowledgeBase("kb_002", "我的私人知识库", "示例私人知识库", "Private", "General", "private", 1L);
    }

    private void insertKnowledgeBase(String id, String name, String description, String category, String type, String visibility, long ownerId) {
        AiKnowledgeBase item = new AiKnowledgeBase();
        item.setId(id);
        item.setTenantId(1L);
        item.setOwnerId(ownerId);
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setType(type);
        item.setVisibility(visibility);
        item.setDocumentCount(0L);
        item.setVectorCount(0L);
        item.setStorageBytes(0L);
        stampNew(item);
        knowledgeBaseMapper.insert(item);
    }

    private AiIngestTask newTask(String taskId, String knowledgeBaseId, long userId) {
        AiIngestTask task = new AiIngestTask();
        task.setId(taskId);
        task.setTenantId(tenantId());
        task.setKnowledgeBaseId(knowledgeBaseId);
        task.setOwnerId(userId);
        task.setStatus("processing");
        task.setProgress(0);
        task.setUploaded(0);
        task.setFailed(0);
        task.setDocumentIds(json(List.of()));
        task.setMessage("导入中");
        stampNew(task);
        return task;
    }

    private void stampNew(Object item) {
        LocalDateTime now = now();
        if (item instanceof AiAgent e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiPlugin e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiKnowledgeBase e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiKnowledgeDocument e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiKnowledgeChunk e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiIngestTask e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiConversation e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiConversationMessage e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
        else if (item instanceof AiKbQaPair e) { e.setCreatedAt(now); e.setUpdatedAt(now); e.setDeleted(0); }
    }

    private List<String> splitTags(String tags) {
        if (blank(tags)) return List.of();
        return Arrays.stream(tags.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        return value instanceof Map<?, ?> map ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
    }

    private List<String> list(Object value) {
        if (value instanceof List<?> raw) return raw.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        if (value instanceof String s && !s.isBlank()) return List.of(s);
        return List.of();
    }

    private List<String> jsonList(String json) {
        if (blank(json)) return List.of();
        try { return objectMapper.readValue(json, STRING_LIST); } catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> jsonListOfMaps(String json) {
        if (blank(json)) return List.of();
        try { return objectMapper.readValue(json, List.class); } catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonMap(String json) {
        if (blank(json)) return Map.of();
        try { return objectMapper.readValue(json, Map.class); } catch (Exception e) { return Map.of(); }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value == null ? List.of() : value); } catch (Exception e) { return "[]"; }
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    private String titleFrom(String content, String fallback) {
        if (blank(content)) return fallback;
        String value = content.strip();
        return value.length() > 24 ? value.substring(0, 24) : value;
    }

    private String truncate(String value, int limit) {
        if (value == null) return "";
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String str(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private double dbl(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }
}
