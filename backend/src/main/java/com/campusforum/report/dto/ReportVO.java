package com.campusforum.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportVO {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String reason;
    private String description;
    private Integer status;
    private Long handlerId;
    private String handlerName;
    private String handleNote;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
