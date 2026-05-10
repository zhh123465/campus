package com.campusforum.post.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campusforum.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("posts")
public class Post extends BaseEntity {

    private Long authorId;
    private String scope;
    private Long spaceId;
    private String type;
    private String title;
    private String content;
    private String attachments;
    private String topics;
    private String tags;
    private String aiSummary;
    private Integer aiRiskLevel;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isPinned;
    private Integer isEssence;
    private Integer status;
}
