package com.aidetector.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TextDetectionRequest {

    @NotBlank(message = "Text content cannot be empty")
    @Size(
        min = 50,
        max = 50000,
        message = "Text must be between 50 and 50,000 characters"
    )
    private String text;

    private String language = "en";
}