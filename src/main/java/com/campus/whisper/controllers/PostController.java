package com.campus.whisper.controllers;

import com.campus.whisper.models.Post;
import com.campus.whisper.payload.request.PostRequest;
import com.campus.whisper.payload.response.MessageResponse;
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
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    PostRepository postRepository;

    @GetMapping
    @Cacheable(value = "posts", key = "#category != null ? #category : 'all'")
    public ResponseEntity<?> getAllPosts(@RequestParam(required = false) String category) {
        List<Post> posts;
        if (category != null && !category.trim().isEmpty()) {
            posts = postRepository.findByCategoryOrderByCreatedAtDesc(category);
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    @Cacheable(value = "post", key = "#id")
    public ResponseEntity<?> getSinglePost(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @CacheEvict(value = "posts", allEntries = true)
    public ResponseEntity<?> createPost(@Valid @RequestBody PostRequest request) {
        Post post = new Post();
        post.setAnonymousUsername("Anonymous"); // Real app might generate random adjectives
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setCreatedAt(LocalDateTime.now());
        post.setUpvotes(0);
        post.setDownvotes(0);
        post.setCommentCount(0);
        
        postRepository.save(post);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = {"posts", "post"}, allEntries = true)
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        if (!postRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        postRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Post deleted successfully"));
    }
}
