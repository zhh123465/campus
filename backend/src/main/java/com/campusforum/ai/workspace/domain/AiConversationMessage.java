package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversation_messages")
public class AiConversationMessage {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private String conversationId;
    private Long ownerId;
    private String role;
    private String content;
    private String model;
    private String agentId;
    private String pluginIds;
    private String knowledgeBaseIds;
    private String citations;
    private Integer feedbackHelpful;
    private String feedbackComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
