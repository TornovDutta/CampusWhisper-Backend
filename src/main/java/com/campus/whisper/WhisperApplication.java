package com.campus.whisper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class WhisperApplication {

    static {
        loadDotEnv();
    }

    public static void main(String[] args) {
        SpringApplication.run(WhisperApplication.class, args);
    }

    private static void loadDotEnv() {
        File[] candidateFiles = new File[] {
            new File(".env"),
            new File("CampusWhisper-Backend/.env"),
            new File("../CampusWhisper-Backend/.env")
        };

        File envFile = null;
        for (File candidate : candidateFiles) {
            if (candidate.exists() && candidate.isFile()) {
                envFile = candidate;
                break;
            }
        }

        if (envFile == null) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            Map<String, String> envMap = new HashMap<>();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    envMap.put(key, val);
                }
            }

            // Resolve references like ${DB_HOST}
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                for (Map.Entry<String, String> ref : envMap.entrySet()) {
                    val = val.replace("${" + ref.getKey() + "}", ref.getValue());
                }
                envMap.put(key, val);
            }

            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (val.isEmpty()) {
                    continue; // Skip empty values to avoid setting blank URL or password
                }
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, val);
                }
            }

            // Dedicated Redis URL handler: only configure if explicitly provided
            String redisUrl = envMap.get("REDIS_URL");
            if (redisUrl == null) redisUrl = System.getenv("REDIS_URL");
            if (redisUrl != null && !redisUrl.trim().isEmpty()) {
                System.setProperty("spring.data.redis.url", redisUrl.trim());
            }

            String redisPassword = envMap.get("REDIS_PASSWORD");
            if (redisPassword == null) redisPassword = System.getenv("REDIS_PASSWORD");
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                System.setProperty("spring.data.redis.password", redisPassword.trim());
            }

            // If connecting to Supabase pooler, ensure sslmode=require is present
            String dsUrl = System.getProperty("SPRING_DATASOURCE_URL");
            if (dsUrl != null && dsUrl.contains("supabase.co") && !dsUrl.contains("sslmode")) {
                dsUrl += (dsUrl.contains("?") ? "&" : "?") + "sslmode=require";
                System.setProperty("SPRING_DATASOURCE_URL", dsUrl);
            }
        } catch (IOException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
    }
}

