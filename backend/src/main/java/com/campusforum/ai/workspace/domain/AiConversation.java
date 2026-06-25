package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversations")
public class AiConversation {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private Long ownerId;
    private String title;
    private String agentId;
    private String pluginIds;
    private String knowledgeBaseIds;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
