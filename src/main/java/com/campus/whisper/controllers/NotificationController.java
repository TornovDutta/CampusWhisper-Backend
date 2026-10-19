package com.campus.whisper.controllers;

import com.campus.whisper.models.Notification;
import com.campus.whisper.payload.response.MessageResponse;
import com.campus.whisper.repository.NotificationRepository;
import com.campus.whisper.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userDetails.getId());
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userDetails.getId());
        for (Notification notif : notifications) {
            notif.setRead(true);
        }
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok(new MessageResponse("All notifications marked as read"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Notification notif = notificationRepository.findById(id).orElse(null);
        if (notif == null || !notif.getUserId().equals(userDetails.getId())) {
            return ResponseEntity.notFound().build();
        }
        notif.setRead(true);
        notificationRepository.save(notif);
        return ResponseEntity.ok(notif);
    }
}
