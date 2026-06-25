package com.campusforum.ai.workspace;

import com.campusforum.ai.workspace.domain.AiKnowledgeChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiKnowledgeSearchClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String host;
    private final String apiKey;
    private final boolean active;
    private final AiWorkspaceRagProperties properties;
    private volatile boolean settingsConfigured;

    public AiKnowledgeSearchClient(@Value("${search.type:mysql}") String searchType,
                                   @Value("${search.meilisearch.host:http://localhost:7700}") String host,
                                   @Value("${search.meilisearch.api-key:}") String apiKey,
                                   AiWorkspaceRagProperties properties) {
        this.host = host;
        this.apiKey = apiKey;
        this.properties = properties;
        this.active = "meilisearch".equalsIgnoreCase(searchType);
    }

    public void indexChunks(List<AiKnowledgeChunk> chunks, String fileName, List<String> tags, String visibility) {
        if (!active || chunks == null || chunks.isEmpty()) {
            return;
        }
        try {
            ensureSettings();
            List<Map<String, Object>> docs = chunks.stream()
                    .map(chunk -> {
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id", chunk.getId());
                        doc.put("tenantId", chunk.getTenantId());
                        doc.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
                        doc.put("documentId", chunk.getDocumentId());
                        doc.put("chunkId", chunk.getId());
                        doc.put("chunkIndex", chunk.getChunkIndex());
                        doc.put("ownerId", chunk.getOwnerId());
                        doc.put("title", fileName);
                        doc.put("content", chunk.getContent());
                        doc.put("tags", tags == null ? List.of() : tags);
                        doc.put("visibility", visibility);
                        return doc;
                    })
                    .collect(Collectors.toList());
            post("/indexes/" + properties.getIndexName() + "/documents", docs);
        } catch (Exception e) {
            log.warn("AI knowledge chunks failed to index in MeiliSearch: {}", e.getMessage());
        }
    }

    public void deleteChunks(String documentId) {
        if (!active || documentId == null || documentId.isBlank()) {
            return;
        }
        try {
            post("/indexes/" + properties.getIndexName() + "/documents/delete", Map.of(
                    "filter", "documentId = \"" + escapeFilter(documentId) + "\""
            ));
        } catch (Exception e) {
            log.debug("AI knowledge chunk delete failed documentId={}: {}", documentId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Citation> search(String query, Long tenantId, List<String> knowledgeBaseIds, int limit) {
        if (!active || tenantId == null || query == null || query.isBlank() || knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return List.of();
        }
        try {
            ensureSettings();
            Map<String, Object> body = new HashMap<>();
            body.put("q", query);
            body.put("limit", Math.max(1, limit));
            body.put("attributesToRetrieve", List.of("knowledgeBaseId", "documentId", "chunkId", "title", "content"));
            body.put("filter", buildFilter(tenantId, knowledgeBaseIds));
            if (properties.isSemanticEnabled() && hasText(properties.getEmbedderApiKey())) {
                body.put("hybrid", Map.of(
                        "embedder", properties.getEmbedderName(),
                        "semanticRatio", 0.5
                ));
            }
            Map<String, Object> resp = post("/indexes/" + properties.getIndexName() + "/search", body);
            Object hitsRaw = resp.get("hits");
            if (!(hitsRaw instanceof List<?> hits)) {
                return List.of();
            }
            List<Citation> citations = new ArrayList<>();
            for (Object item : hits) {
                if (item instanceof Map<?, ?> hit) {
                    String content = str(hit.get("content"));
                    citations.add(Citation.builder()
                            .knowledgeBaseId(str(hit.get("knowledgeBaseId")))
                            .documentId(str(hit.get("documentId")))
                            .chunkId(str(hit.get("chunkId")))
                            .title(str(hit.get("title")))
                            .snippet(content.length() > 260 ? content.substring(0, 260) + "..." : content)
                            .score(score(hit))
                            .build());
                }
            }
            return citations;
        } catch (Exception e) {
            log.warn("AI knowledge search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private void ensureSettings() {
        if (!active || settingsConfigured) {
            return;
        }
        try {
            post("/indexes", Map.of("uid", properties.getIndexName(), "primaryKey", "id"));
        } catch (Exception ignored) {
        }
        try {
            exchange("/indexes/" + properties.getIndexName() + "/settings/filterable-attributes",
                    HttpMethod.PUT,
                    List.of("tenantId", "knowledgeBaseId", "documentId", "ownerId", "visibility"));
            exchange("/indexes/" + properties.getIndexName() + "/settings/searchable-attributes",
                    HttpMethod.PUT,
                    List.of("title", "content", "tags"));
            if (properties.isSemanticEnabled() && hasText(properties.getEmbedderApiKey())) {
                Map<String, Object> embedder = new HashMap<>();
                embedder.put("source", properties.getEmbedderSource());
                embedder.put("apiKey", properties.getEmbedderApiKey());
                embedder.put("model", properties.getEmbedderModel());
                embedder.put("documentTemplate", properties.getDocumentTemplate());
                exchange("/indexes/" + properties.getIndexName() + "/settings/embedders",
                        HttpMethod.PATCH,
                        Map.of(properties.getEmbedderName(), embedder));
            }
            settingsConfigured = true;
        } catch (Exception e) {
            log.warn("AI knowledge MeiliSearch settings failed: {}", e.getMessage());
        }
    }

    private String buildFilter(Long tenantId, List<String> knowledgeBaseIds) {
        String kbFilter = knowledgeBaseIds.stream()
                .filter(Objects::nonNull)
                .map(id -> "knowledgeBaseId = \"" + escapeFilter(id) + "\"")
                .collect(Collectors.joining(" OR "));
        return "tenantId = " + tenantId + " AND (" + kbFilter + ")";
    }

    private Map<String, Object> post(String path, Object body) {
        return exchange(path, HttpMethod.POST, body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(String path, HttpMethod method, Object body) {
        ResponseEntity<Map> resp = restTemplate.exchange(host + path, method,
                new HttpEntity<>(body, jsonHeaders()), Map.class);
        return resp.getBody() == null ? Map.of() : resp.getBody();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasText(apiKey)) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    private String escapeFilter(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double score(Map<?, ?> hit) {
        Object rankingScore = hit.get("_rankingScore");
        return rankingScore instanceof Number n ? n.doubleValue() : 0.0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    @Builder
    public static class Citation {
        private String knowledgeBaseId;
        private String documentId;
        private String chunkId;
        private String title;
        private String snippet;
        private double score;
    }
}
