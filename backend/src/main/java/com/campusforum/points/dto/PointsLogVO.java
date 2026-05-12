package com.campusforum.points.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PointsLogVO {
    private Long id;
    private Long userId;
    private Long amount;
    private String type;
    private String reference;
    private Long balance;
    private LocalDateTime createdAt;
}
