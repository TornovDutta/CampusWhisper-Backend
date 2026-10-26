package com.campus.whisper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthSecurityTest {

    static {
        WhisperApplication.loadDotEnv();
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheckApiHealth_IsPubliclyAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("CampusWhisper Backend"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void healthCheckHealth_IsPubliclyAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("CampusWhisper Backend"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void protectedEndpoints_RemainUnauthorizedWithoutAuth() throws Exception {
        // Verify GET /api/posts is protected
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());

        // Verify POST /api/posts is protected
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Test whisper\",\"category\":\"General\"}"))
                .andExpect(status().isUnauthorized());

        // Verify GET /api/notifications is protected
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
