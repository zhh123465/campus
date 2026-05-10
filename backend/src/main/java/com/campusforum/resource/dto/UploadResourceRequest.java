package com.campusforum.resource.dto;

import lombok.Data;

import java.util.List;

@Data
public class UploadResourceRequest {
    private Long spaceId;
    private String visibility;
    private String college;
    private String major;
    private String course;
    private String semester;
    private List<String> tags;
    private String description;
}
