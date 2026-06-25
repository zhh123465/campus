package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_agents")
public class AiAgent {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private Long ownerId;
    private String name;
    private String description;
    private String category;
    private String model;
    private String prompt;
    private String abilities;
    private String knowledgeBaseIds;
    private String pluginIds;
    private String tags;
    private String avatar;
    private String color;
    private Long userCount;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
