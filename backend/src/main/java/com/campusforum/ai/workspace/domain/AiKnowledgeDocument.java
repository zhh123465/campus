package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_documents")
public class AiKnowledgeDocument {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private String knowledgeBaseId;
    private Long ownerId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String storageKey;
    private String tags;
    private String parseMode;
    private String status;
    private Long chunkCount;
    private Long storageBytes;
    private LocalDateTime indexedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
