package com.campusforum.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reports")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String description;
    private Integer status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
