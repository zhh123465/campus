package com.campusforum.ai.workspace;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai-workspace.rag")
public class AiWorkspaceRagProperties {
    private String indexName = "ai_kb_chunks";
    private boolean semanticEnabled = false;
    private String embedderName = "default";
    private String embedderSource = "openAi";
    private String embedderApiKey = "";
    private String embedderModel = "text-embedding-3-small";
    private String documentTemplate = "{{doc.title}}\n{{doc.content}}";
    private int chunkSize = 900;
    private int chunkOverlap = 150;
    private int topK = 6;
    private int maxContextChars = 3200;
    private int maxDocumentChars = 200000;
}
