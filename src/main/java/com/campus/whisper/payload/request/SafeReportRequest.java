package com.campus.whisper.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SafeReportRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;
}
