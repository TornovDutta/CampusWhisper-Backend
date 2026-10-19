package com.campus.whisper.controllers;

import com.campus.whisper.models.Comment;
import com.campus.whisper.models.CommentVote;
import com.campus.whisper.models.Notification;
import com.campus.whisper.models.Post;
import com.campus.whisper.payload.request.CommentRequest;
import com.campus.whisper.payload.response.MessageResponse;
import com.campus.whisper.repository.CommentRepository;
import com.campus.whisper.repository.CommentVoteRepository;
import com.campus.whisper.repository.NotificationRepository;
import com.campus.whisper.repository.PostRepository;
import com.campus.whisper.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    CommentVoteRepository commentVoteRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @GetMapping("/post/{postId}")
    @Cacheable(value = "comments", key = "#postId")
    public ResponseEntity<?> getCommentsForPost(@PathVariable Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/liked-ids")
    public ResponseEntity<?> getLikedCommentIds(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<CommentVote> votes = commentVoteRepository.findByUserId(userDetails.getId());
        List<Long> likedIds = votes.stream().map(CommentVote::getCommentId).collect(Collectors.toList());
        return ResponseEntity.ok(likedIds);
    }

    @PostMapping
    @CacheEvict(value = {"comments", "posts", "post"}, allEntries = true)
    public ResponseEntity<?> addComment(@Valid @RequestBody CommentRequest request, Authentication authentication) {
        Post post = postRepository.findById(request.getPostId()).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Post not found"));
        }

        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        
        Long authorId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            authorId = userDetails.getId();
            comment.setUserId(authorId);
        }

        comment.setAnonymousUsername("Anonymous");
        comment.setContent(request.getContent());
        comment.setUpvotes(0);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        // Update post comment count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // Notify post owner of the new reply
        if (authorId != null && post.getUserId() != null && !post.getUserId().equals(authorId)) {
            Notification notif = new Notification();
            notif.setUserId(post.getUserId());
            notif.setType("reply");
            notif.setTitle("New Reply");
            String snippet = comment.getContent();
            if (snippet.length() > 60) {
                snippet = snippet.substring(0, 57) + "...";
            }
            notif.setMessage("Someone replied to your whisper: \"" + snippet + "\"");
            notif.setPostId(post.getId());
            notif.setRead(false);
            notif.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notif);
        }

        return ResponseEntity.ok(comment);
    }

    @PostMapping("/{commentId}/like")
    @CacheEvict(value = {"comments", "post", "posts"}, allEntries = true)
    public ResponseEntity<?> likeComment(@PathVariable Long commentId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Comment not found"));
        }

        Post post = postRepository.findById(comment.getPostId()).orElse(null);

        Optional<CommentVote> existingVote = commentVoteRepository.findByCommentIdAndUserId(commentId, userId);
        boolean hasUpvoted;
        if (existingVote.isPresent()) {
            commentVoteRepository.delete(existingVote.get());
            comment.setUpvotes(Math.max(0, comment.getUpvotes() - 1));
            hasUpvoted = false;
        } else {
            CommentVote cv = new CommentVote();
            cv.setCommentId(commentId);
            cv.setUserId(userId);
            commentVoteRepository.save(cv);
            comment.setUpvotes(comment.getUpvotes() + 1);
            hasUpvoted = true;

            String commentSnippet = comment.getContent();
            if (commentSnippet.length() > 60) {
                commentSnippet = commentSnippet.substring(0, 57) + "...";
            }

            // 1. Notify the owner of the post
            if (post != null && post.getUserId() != null && !post.getUserId().equals(userId)) {
                Notification notif = new Notification();
                notif.setUserId(post.getUserId());
                notif.setType("upvote");
                notif.setTitle("Comment Liked on Your Post");
                notif.setMessage("Someone liked a comment on your whisper: \"" + commentSnippet + "\"");
                notif.setPostId(post.getId());
                notif.setRead(false);
                notif.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notif);
            }

            // 2. Also notify the author of the comment (if different user and not already notified as post owner)
            if (comment.getUserId() != null && !comment.getUserId().equals(userId)
                    && (post == null || !comment.getUserId().equals(post.getUserId()))) {
                Notification notif = new Notification();
                notif.setUserId(comment.getUserId());
                notif.setType("upvote");
                notif.setTitle("Comment Liked");
                notif.setMessage("Someone liked your comment: \"" + commentSnippet + "\"");
                if (post != null) {
                    notif.setPostId(post.getId());
                }
                notif.setRead(false);
                notif.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notif);
            }
        }

        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("commentId", comment.getId());
        result.put("upvotes", comment.getUpvotes());
        result.put("hasUpvoted", hasUpvoted);
        return ResponseEntity.ok(result);
    }
}

