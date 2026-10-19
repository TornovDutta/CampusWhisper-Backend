package com.campus.whisper.controllers;

import com.campus.whisper.models.Post;
import com.campus.whisper.models.Vote;
import com.campus.whisper.models.VoteType;
import com.campus.whisper.payload.request.VoteRequest;
import com.campus.whisper.payload.response.MessageResponse;
import com.campus.whisper.repository.PostRepository;
import com.campus.whisper.repository.VoteRepository;
import com.campus.whisper.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/votes")
public class VoteController {

    @Autowired
    VoteRepository voteRepository;

    @Autowired
    PostRepository postRepository;

    @PostMapping
    @CacheEvict(value = {"posts", "post"}, allEntries = true)
    public ResponseEntity<?> voteOnPost(@Valid @RequestBody VoteRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        VoteType voteType;
        try {
            voteType = VoteType.valueOf(request.getType().toLowerCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid vote type. Must be 'upvote' or 'downvote'"));
        }

        Post post = postRepository.findById(request.getPostId()).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Post not found"));
        }

        Optional<Vote> existingVote = voteRepository.findByPostIdAndUserId(request.getPostId(), userId);
        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            // Undo previous vote effect
            if (vote.getType() == VoteType.upvote) {
                post.setUpvotes(post.getUpvotes() - 1);
            } else if (vote.getType() == VoteType.downvote) {
                post.setDownvotes(post.getDownvotes() - 1);
            }

            // If same type, it's a toggle off (destroy the vote)
            if (vote.getType() == voteType) {
                voteRepository.delete(vote);
            } else {
                // Change vote type
                vote.setType(voteType);
                if (voteType == VoteType.upvote) post.setUpvotes(post.getUpvotes() + 1);
                if (voteType == VoteType.downvote) post.setDownvotes(post.getDownvotes() + 1);
                voteRepository.save(vote);
            }
        } else {
            // New vote
            Vote vote = new Vote();
            vote.setPostId(request.getPostId());
            vote.setUserId(userId);
            vote.setType(voteType);
            voteRepository.save(vote);

            if (voteType == VoteType.upvote) post.setUpvotes(post.getUpvotes() + 1);
            if (voteType == VoteType.downvote) post.setDownvotes(post.getDownvotes() + 1);
        }

        postRepository.save(post);
        return ResponseEntity.ok(new MessageResponse("Vote processed successfully!"));
    }
}
