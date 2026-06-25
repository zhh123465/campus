package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_ingest_tasks")
public class AiIngestTask {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private String knowledgeBaseId;
    private Long ownerId;
    private String status;
    private Integer progress;
    private Integer uploaded;
    private Integer failed;
    private String documentIds;
    private String message;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
