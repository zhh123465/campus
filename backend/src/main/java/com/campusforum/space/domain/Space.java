package com.campusforum.space.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campusforum.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spaces")
public class Space extends BaseEntity {

    private Long ownerId;
    private String name;
    private String description;
    private String category;
    private String visibility;
    private String coverUrl;
    private Integer memberCount;
    private Integer postCount;
    private Integer status;
    private String sensitiveWords;
    private String postNotice;
}
