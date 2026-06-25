package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_chunks")
public class AiKnowledgeChunk {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private String knowledgeBaseId;
    private String documentId;
    private Long ownerId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private String contentHash;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
