package com.campus.whisper.controllers;

import com.campus.whisper.models.SafeReport;
import com.campus.whisper.payload.request.SafeReportRequest;
import com.campus.whisper.payload.response.MessageResponse;
import com.campus.whisper.repository.SafeReportRepository;
import com.campus.whisper.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/safe-reports")
public class SafeReportController {

    @Autowired
    SafeReportRepository safeReportRepository;

    @PostMapping
    public ResponseEntity<?> createSafeReport(@Valid @RequestBody SafeReportRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        SafeReport safeReport = new SafeReport();
        safeReport.setUserId(userDetails.getId());
        safeReport.setTitle(request.getTitle());
        safeReport.setDescription(request.getDescription());
        safeReport.setCreatedAt(LocalDateTime.now());

        safeReportRepository.save(safeReport);

        return ResponseEntity.ok(new MessageResponse("Safe report submitted successfully"));
    }
}
