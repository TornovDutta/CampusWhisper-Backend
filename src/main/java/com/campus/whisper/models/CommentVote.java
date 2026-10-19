package com.campus.whisper.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "comment_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"commentId", "userId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long commentId;

    @Column(nullable = false)
    private Long userId;
}
