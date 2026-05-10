package com.campusforum.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotNull
    private Long postId;

    private Long parentId;
    private Long replyToId;

    @NotBlank
    @Size(min = 1, max = 5000)
    private String content;
}
