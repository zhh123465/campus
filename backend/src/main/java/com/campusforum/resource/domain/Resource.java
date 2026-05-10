package com.campusforum.resource.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campusforum.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resources")
public class Resource extends BaseEntity {
    private Long uploaderId;
    private Long spaceId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileMd5;
    private String storageKey;
    private String visibility;
    private String college;
    private String major;
    private String course;
    private String semester;
    private String tags;
    private Integer downloadCount;
    private Integer collectCount;
    private String version;
    private String description;
    private Integer status;
}
