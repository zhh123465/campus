package com.campusforum.points.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_logs")
public class PointsLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long amount;
    private String type;
    private String reference;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
