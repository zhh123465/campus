package com.campusforum.space.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("space_members")
public class SpaceMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long spaceId;
    private Long userId;
    private String role;
    private Integer status;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
}
