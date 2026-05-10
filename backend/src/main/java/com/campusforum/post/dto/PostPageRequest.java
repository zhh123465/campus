package com.campusforum.post.dto;

import lombok.Data;

@Data
public class PostPageRequest {

    private String scope = "SQUARE";
    private String sort = "latest";
    private Long cursor;
    private int limit = 20;
}
