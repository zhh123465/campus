package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_plugins")
public class AiPlugin {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long tenantId;
    private Long ownerId;
    private String name;
    private String description;
    private String category;
    private String icon;
    private String color;
    private Long usageCount;
    private Long installCount;
    private BigDecimal rating;
    private Integer isOfficial;
    private Integer isFeatured;
    private String permissions;
    private String inputSchema;
    private String outputSchema;
    private String endpoint;
    private String reviewStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
