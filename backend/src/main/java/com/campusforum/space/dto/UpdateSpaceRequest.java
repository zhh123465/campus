package com.campusforum.space.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSpaceRequest {

    @Size(max = 64)
    private String name;

    @Size(max = 255)
    private String description;

    private String visibility;
}
