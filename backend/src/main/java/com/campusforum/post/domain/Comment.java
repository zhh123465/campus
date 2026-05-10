package com.campusforum.post.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campusforum.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comments")
public class Comment extends BaseEntity {

    private Long postId;
    private Long parentId;
    private Long replyToId;
    private Long authorId;
    private String content;
    private Integer likeCount;
    private Integer status;
}
