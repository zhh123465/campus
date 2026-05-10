package com.campusforum.space.dto;

import com.campusforum.user.dto.UserVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SpaceVO {

    private Long id;
    private Long ownerId;
    private UserVO owner;
    private String name;
    private String description;
    private String category;
    private String visibility;
    private Integer memberCount;
    private Integer postCount;
    private Integer status;
    private Boolean isMember;
    private String memberRole;
    private LocalDateTime createdAt;
}
