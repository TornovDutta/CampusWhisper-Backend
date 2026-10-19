package com.campus.whisper.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    private Long userId;

    @Column(nullable = false)
    private String anonymousUsername;

    @Column(nullable = false, length = 500)
    private String content;

    private int upvotes = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
