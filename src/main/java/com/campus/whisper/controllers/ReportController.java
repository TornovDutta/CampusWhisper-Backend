package com.campus.whisper.controllers;

import com.campus.whisper.models.Report;
import com.campus.whisper.payload.request.ReportRequest;
import com.campus.whisper.payload.response.MessageResponse;
import com.campus.whisper.repository.PostRepository;
import com.campus.whisper.repository.ReportRepository;
import com.campus.whisper.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    PostRepository postRepository;

    @PostMapping
    public ResponseEntity<?> reportPost(@Valid @RequestBody ReportRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!postRepository.existsById(request.getPostId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Post not found"));
        }

        Report report = new Report();
        report.setPostId(request.getPostId());
        report.setUserId(userDetails.getId());
        report.setReason(request.getReason());
        
        reportRepository.save(report);

        return ResponseEntity.ok(new MessageResponse("Report submitted successfully"));
    }
}
