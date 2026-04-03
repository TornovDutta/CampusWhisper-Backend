package com.campus.whisper.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {
    @NotNull
    private Long postId;

    @NotBlank
    private String type; // "upvote" or "downvote"
}
