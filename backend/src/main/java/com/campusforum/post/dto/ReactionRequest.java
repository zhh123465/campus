package com.campusforum.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReactionRequest {

    @NotBlank
    private String targetType;

    @NotNull
    private Long targetId;

    @NotBlank
    private String type;
}
