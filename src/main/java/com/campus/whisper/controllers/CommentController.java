package com.campus.whisper.controllers;

import com.campus.whisper.models.Comment;
import com.campus.whisper.models.Post;
import com.campus.whisper.payload.request.CommentRequest;
import com.campus.whisper.payload.response.MessageResponse;
import com.campus.whisper.repository.CommentRepository;
import com.campus.whisper.repository.PostRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    PostRepository postRepository;

    @GetMapping("/post/{postId}")
    @Cacheable(value = "comments", key = "#postId")
    public ResponseEntity<?> getCommentsForPost(@PathVariable Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    @CacheEvict(value = {"comments", "posts", "post"}, allEntries = true)
    public ResponseEntity<?> addComment(@Valid @RequestBody CommentRequest request) {
        Post post = postRepository.findById(request.getPostId()).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Post not found"));
        }

        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setAnonymousUsername("Anonymous");
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        // Update post comment count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return ResponseEntity.ok(comment);
    }
}
