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
        Map<String, String> envMap = new HashMap<>();
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

        if (envFile != null) {
            try {
                List<String> lines = Files.readAllLines(envFile.toPath());
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
                    if (!val.isEmpty() && System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, val);
                    }
                }
            } catch (IOException e) {
                System.err.println("Notice: Could not read .env file: " + e.getMessage());
            }
        }

        // =========================================================================
        // Database Configuration (runs on both local and Render production)
        // =========================================================================
        String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (dbUrl == null || dbUrl.trim().isEmpty()) dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.trim().isEmpty()) dbUrl = envMap.get("SPRING_DATASOURCE_URL");
        if (dbUrl == null || dbUrl.trim().isEmpty()) dbUrl = envMap.get("DATABASE_URL");

        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            dbUrl = dbUrl.trim();
            // Standardize postgres:// or postgresql:// into jdbc:postgresql://
            if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://") || (dbUrl.startsWith("jdbc:postgresql://") && dbUrl.contains("@"))) {
                try {
                    String clean = dbUrl.replace("jdbc:", "");
                    java.net.URI uri = java.net.URI.create(clean);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String userInfo = uri.getUserInfo();

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                        jdbcUrl += "?" + uri.getQuery();
                    }
                    if (host != null && host.contains("supabase.co") && !jdbcUrl.contains("sslmode")) {
                        jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
                    }

                    System.setProperty("spring.datasource.url", jdbcUrl);
                    System.setProperty("SPRING_DATASOURCE_URL", jdbcUrl);

                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        System.setProperty("spring.datasource.username", parts[0]);
                        System.setProperty("spring.datasource.password", parts[1]);
                    }
                } catch (Exception ex) {
                    if (!dbUrl.startsWith("jdbc:")) {
                        dbUrl = "jdbc:" + dbUrl;
                    }
                    System.setProperty("spring.datasource.url", dbUrl);
                    System.setProperty("SPRING_DATASOURCE_URL", dbUrl);
                }
            } else {
                if (!dbUrl.startsWith("jdbc:")) {
                    dbUrl = "jdbc:" + dbUrl;
                }
                if (dbUrl.contains("supabase.co") && !dbUrl.contains("sslmode")) {
                    dbUrl += (dbUrl.contains("?") ? "&" : "?") + "sslmode=require";
                }
                System.setProperty("spring.datasource.url", dbUrl);
                System.setProperty("SPRING_DATASOURCE_URL", dbUrl);
            }
        }

        // =========================================================================
        // Redis Configuration (runs on both local and Render production)
        // =========================================================================
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null || redisHost.trim().isEmpty()) redisHost = envMap.get("REDIS_HOST");
        if (redisHost != null && !redisHost.trim().isEmpty()) {
            System.setProperty("spring.data.redis.host", redisHost.trim());
        }

        String redisPort = System.getenv("REDIS_PORT");
        if (redisPort == null || redisPort.trim().isEmpty()) redisPort = envMap.get("REDIS_PORT");
        if (redisPort != null && !redisPort.trim().isEmpty()) {
            System.setProperty("spring.data.redis.port", redisPort.trim());
        }

        String redisUsername = System.getenv("REDIS_USERNAME");
        if (redisUsername == null || redisUsername.trim().isEmpty()) redisUsername = envMap.get("REDIS_USERNAME");
        if (redisUsername != null && !redisUsername.trim().isEmpty()) {
            System.setProperty("spring.data.redis.username", redisUsername.trim());
        }

        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword == null || redisPassword.trim().isEmpty()) redisPassword = envMap.get("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            System.setProperty("spring.data.redis.password", redisPassword.trim());
        }

        String redisSsl = System.getenv("REDIS_SSL");
        if (redisSsl == null || redisSsl.trim().isEmpty()) redisSsl = envMap.get("REDIS_SSL");
        if (redisSsl != null && !redisSsl.trim().isEmpty()) {
            System.setProperty("spring.data.redis.ssl.enabled", redisSsl.trim());
        }

        String redisUrl = System.getenv("REDIS_URL");
        if (redisUrl == null || redisUrl.trim().isEmpty()) redisUrl = envMap.get("REDIS_URL");
        if (redisUrl != null && !redisUrl.trim().isEmpty()) {
            System.setProperty("spring.data.redis.url", redisUrl.trim());
        }
    }
}

