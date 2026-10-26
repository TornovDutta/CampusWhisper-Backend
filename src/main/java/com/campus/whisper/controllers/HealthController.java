package com.campus.whisper.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class HealthController {

    @GetMapping({"/api/health", "/health"})
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "CampusWhisper Backend");
        response.put("message", "Service is healthy and running");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
