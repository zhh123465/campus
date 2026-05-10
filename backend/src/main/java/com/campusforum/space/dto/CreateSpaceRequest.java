package com.campusforum.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSpaceRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank
    private String category; // MAJOR/CLASS/CLUB/INTEREST

    private String visibility = "PUBLIC"; // PUBLIC/REVIEW/INVITE
}
