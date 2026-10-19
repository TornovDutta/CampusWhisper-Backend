package com.campus.whisper.repository;

import com.campus.whisper.models.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
    Optional<CommentVote> findByCommentIdAndUserId(Long commentId, Long userId);
    List<CommentVote> findByUserId(Long userId);
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
}
