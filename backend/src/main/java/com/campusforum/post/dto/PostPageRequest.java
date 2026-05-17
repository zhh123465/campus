package com.campusforum.post.dto;

import lombok.Data;

@Data
public class PostPageRequest {

    private String scope = "SQUARE";
    private Long authorId;
    private String sort = "latest";
    private Long cursor;
    private Long cursorId;
    private int limit = 20;
}
