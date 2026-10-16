# CampusWhisper Backend

A secure, scalable RESTful API service powering the CampusWhisper anonymous student social network. Engineered with Spring Boot 3, Java 17, Spring Security with stateless JWT, Spring Data JPA with PostgreSQL, and Redis caching, CampusWhisper Backend enforces strict data decoupling to ensure authentic, constructive, and cryptographically confidential campus discourse.

---

## Table of Contents

- [Overview](#overview)
- [Architecture & Design Decisions](#architecture--design-decisions)
- [Tech Stack](#tech-stack)
- [Project Directory Structure](#project-directory-structure)
- [Environment Configuration](#environment-configuration)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Option A: Running with Docker Compose (Recommended)](#option-a-running-with-docker-compose-recommended)
  - [Option B: Running Locally with Maven](#option-b-running-locally-with-maven)
- [REST API Reference](#rest-api-reference)
  - [Authentication Endpoints](#authentication-endpoints)
  - [Whisper Posts Endpoints](#whisper-posts-endpoints)
  - [Comment Endpoints](#comment-endpoints)
  - [Voting Endpoints](#voting-endpoints)
  - [Notification Endpoints](#notification-endpoints)
  - [Safe Report Endpoints](#safe-report-endpoints)
- [Database Schema & Data Model](#database-schema--data-model)
- [Caching Strategy](#caching-strategy)
- [Security & Cryptography](#security--cryptography)
- [Testing](#testing)
- [License](#license)

---

## Overview

CampusWhisper Backend provides a hardened server architecture designed for student anonymity:
- **Identity Decoupling**: Student accounts authenticate via institutional email credentials, but published whispers and comments are detached from relational user foreign keys using pseudonymous handles.
- **Sub-Second Performance**: Critical feed endpoints and vote tallies leverage Redis cache layers with automated invalidation.
- **Institutional Compliance**: An encrypted Safe Report processing pipeline routes serious misconduct or safety alerts directly to oversight teams with unique anonymous ticket references.

---

## Architecture & Design Decisions

- **Stateless Authentication**: Eliminates server-side session bloat via cryptographically signed JWT tokens (HMAC-SHA256).
- **Relational Consistency with PostgreSQL**: Guarantees ACID transactions for whisper creation, vote aggregation, and safe reports.
- **Distributed Cache Layer**: Employs Redis for high-frequency feed queries and vote deduplication, reducing database connection load.
- **Flexible Environment Profiles**: Uses standard environment variable interpolation (`application.yml`) to support local development, containerized staging, and cloud production environments without code modification.

---

## Tech Stack

| Layer | Component | Version | Description |
| :--- | :--- | :--- | :--- |
| **Runtime** | Java OpenJDK | 17 | Modern LTS Java runtime environment |
| **Framework** | Spring Boot | 3.4.4 | Microservice framework with autoconfiguration |
| **Security** | Spring Security & JJWT | 6.x / 0.11.5 | Stateless JWT authentication, BCrypt hashing, and CORS |
| **Persistence** | Spring Data JPA / Hibernate | 6.x | Object-relational mapping with PostgreSQL dialect |
| **Database** | PostgreSQL | 15-alpine | Relational persistence for accounts, posts, and reports |
| **Cache** | Redis & Spring Data Redis | 7-alpine | In-memory key-value cache with 10-minute TTL |
| **Containerization**| Docker & Docker Compose | 3.8 | Multi-service local orchestration |
| **Build Tool** | Apache Maven | Wrapper 3.9+ | Build and dependency management |

---

## Project Directory Structure

```
CampusWhisper-Backend/
├── src/
│   ├── main/
│   │   ├── java/com/campus/whisper/
│   │   │   ├── config/
│   │   │   │   └── RedisConfig.java          # Redis cache TTL and JSON serialization
│   │   │   ├── controllers/
│   │   │   │   ├── AuthController.java       # User registration, login, and profile
│   │   │   │   ├── CommentController.java    # Threaded whisper replies
│   │   │   │   ├── NotificationController.java # User activity notifications
│   │   │   │   ├── PostController.java       # Whisper CRUD and channel filtering
│   │   │   │   ├── ReportController.java     # Whisper content moderation flags
│   │   │   │   ├── SafeReportController.java # Institutional confidential whistleblower
│   │   │   │   └── VoteController.java       # Upvote and downvote tracking
│   │   │   ├── models/
│   │   │   │   ├── Comment.java              # Comment entity definition
│   │   │   │   ├── Notification.java         # Activity alert entity
│   │   │   │   ├── Post.java                 # Whisper post entity
│   │   │   │   ├── Report.java               # Flagged post entity
│   │   │   │   ├── SafeReport.java           # Confidential incident report entity
│   │   │   │   ├── User.java                 # Student account entity
│   │   │   │   ├── Vote.java                 # Post vote audit record
│   │   │   │   └── VoteType.java             # Enum: UPVOTE, DOWNVOTE
│   │   │   ├── payload/
│   │   │   │   ├── request/                  # DTO request models
│   │   │   │   └── response/                 # DTO response models (JwtResponse, etc.)
│   │   │   ├── repository/                   # Spring Data JPA repositories
│   │   │   ├── security/
│   │   │   │   ├── jwt/
│   │   │   │   │   ├── AuthEntryPointJwt.java # Unauthorized access handler
│   │   │   │   │   ├── AuthTokenFilter.java   # Per-request JWT Bearer token validator
│   │   │   │   │   └── JwtUtils.java          # JWT token creation and verification
│   │   │   │   ├── services/
│   │   │   │   │   ├── UserDetailsImpl.java   # Spring Security UserPrincipal
│   │   │   │   │   └── UserDetailsServiceImpl.java # Database user resolution
│   │   │   │   └── WebSecurityConfig.java     # Security filter chain and CORS policy
│   │   │   └── WhisperApplication.java       # Spring Boot main class
│   │   └── resources/
│   │       ├── application.yml               # Centralized configuration template
│   │       └── keystore.p12                  # Optional SSL certificate bundle
├── .env                                      # Local environment configuration
├── .env.example                              # Environment configuration template
├── docker-compose.yml                        # Docker services (PostgreSQL, Redis, App)
├── Dockerfile                                # Multi-stage Docker container build
├── mvnw & mvnw.cmd                           # Cross-platform Maven wrapper scripts
└── pom.xml                                   # Maven dependencies and build plugins
```

---

## Environment Configuration

The backend utilizes `application.yml` with Spring Boot property placeholder expansion (`${VARIABLE_NAME:defaultValue}`) to ingest configuration values directly from environment variables.

### 1. Template File: `.env.example`
The repository includes a ready-to-use template file `.env.example`:

```bash
# Server Configuration
SERVER_PORT=8080
SSL_ENABLED=false
SSL_KEY_STORE=classpath:keystore.p12
SSL_KEY_STORE_PASSWORD=whisper
SSL_KEY_STORE_TYPE=PKCS12
SSL_KEY_ALIAS=whisper

# PostgreSQL Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=whisper
DB_USER=whisper_user
DB_PASSWORD=whisper_password
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/whisper

# Redis Cache Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Authentication (HS256 Base64 Secret & Expiry in Milliseconds)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION_MS=86400000

# JPA & Hibernate Settings
HIBERNATE_DDL_AUTO=update
SHOW_SQL=true

# Allowed CORS Origins (comma-separated)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

### 2. Creating Local Configuration
Create your active `.env` file from the template:

```bash
cp .env.example .env
```

---

## Getting Started

### Prerequisites
- Java Development Kit (JDK): Version 17 or higher
- Docker & Docker Compose (if running containerized)
- PostgreSQL 15+ and Redis 7+ (if running locally without Docker)

---

### Option A: Running with Docker Compose (Recommended)

Docker Compose automatically spins up the PostgreSQL database, Redis cache, and the Spring Boot backend container.

1. Navigate to the backend directory:
   ```bash
   cd CampusWhisper-Backend
   ```

2. Verify or update `.env`:
   ```bash
   cp .env.example .env
   ```

3. Build and launch all services:
   ```bash
   docker-compose up --build -d
   ```

4. Verify running containers:
   ```bash
   docker-compose ps
   ```

5. View application logs:
   ```bash
   docker-compose logs -f app
   ```

To stop all services:
```bash
docker-compose down
```

---

### Option B: Running Locally with Maven

If you have PostgreSQL and Redis running locally:

1. Ensure PostgreSQL is active on port `5432` with database `whisper` created:
   ```sql
   CREATE DATABASE whisper;
   CREATE USER whisper_user WITH ENCRYPTED PASSWORD 'whisper_password';
   GRANT ALL PRIVILEGES ON DATABASE whisper TO whisper_user;
   ```

2. Ensure Redis is active on port `6379`.

3. Start the application using Maven:
   - On Linux/macOS:
     ```bash
     ./mvnw spring-boot:run
     ```
   - On Windows (PowerShell):
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```

4. The API will be accessible at:
   ```
   http://localhost:8080/api
   ```

---

## REST API Reference

All protected endpoints require the following HTTP header:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
```

### Authentication Endpoints

#### 1. Register Student Account
* **Endpoint:** `POST /api/auth/register`
* **Access:** Public
* **Request Body:**
  ```json
  {
    "username": "StudentFox",
    "email": "student@university.edu",
    "password": "SecurePassword123"
  }
  ```
* **Response:** `200 OK`
  ```json
  {
    "message": "User registered successfully!"
  }
  ```

#### 2. Authenticate & Obtain Token
* **Endpoint:** `POST /api/auth/login`
* **Access:** Public
* **Request Body:**
  ```json
  {
    "email": "student@university.edu",
    "password": "SecurePassword123"
  }
  ```
* **Response:** `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "StudentFox",
    "email": "student@university.edu"
  }
  ```

#### 3. Fetch Current Student Profile
* **Endpoint:** `GET /api/auth/me`
* **Access:** Authenticated

---

### Whisper Posts Endpoints

#### 1. Retrieve Whispers Feed
* **Endpoint:** `GET /api/posts`
* **Access:** Public / Authenticated
* **Query Parameters:**
  - `category` *(optional)*: Filter by channel (`placement`, `academics`, `events`, `confessions`, `general`)
* **Response:** `200 OK` (Array of Post objects)

#### 2. Create Anonymous Whisper
* **Endpoint:** `POST /api/posts`
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "content": "Microsoft campus drive shortlists just dropped for 2026. Prepare graph DP questions.",
    "category": "placement"
  }
  ```
* **Response:** `201 Created`

#### 3. Get Whisper Details
* **Endpoint:** `GET /api/posts/{id}`
* **Access:** Public / Authenticated

---

### Comment Endpoints

#### 1. Get Comments for a Post
* **Endpoint:** `GET /api/comments/post/{postId}`
* **Access:** Public / Authenticated

#### 2. Post Anonymous Comment
* **Endpoint:** `POST /api/comments`
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "postId": "post-101",
    "content": "Can confirm topological sort was tested extensively in round 1."
  }
  ```

---

### Voting Endpoints

#### 1. Vote on Whisper
* **Endpoint:** `POST /api/votes`
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "postId": "post-101",
    "type": "upvote"
  }
  ```
* Note: `type` accepts `"upvote"` or `"downvote"`. Repeating the same vote action toggles and cancels the vote.

---

### Notification Endpoints

#### 1. Fetch User Notifications
* **Endpoint:** `GET /api/notifications`
* **Access:** Authenticated
* **Response:** `200 OK` (Array of decoupled notifications)

---

### Safe Report Endpoints

#### 1. Submit Confidential Incident Report
* **Endpoint:** `POST /api/safe-reports`
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "title": "Severe safety hazard in chemical laboratory",
    "description": "Fume hoods in Lab 304 are malfunctioning and venting hazardous solvent fumes.",
    "category": "safety",
    "severity": "urgent",
    "location": "Science Building Room 304",
    "referenceTicket": "CW-SAFE-8942-X7"
  }
  ```
* **Response:** `200 OK`

---

## Database Schema & Data Model

The data layer uses Hibernate with PostgreSQL:

```
+------------------+         +------------------+         +------------------+
|      users       |         |      posts       |         |     comments     |
+------------------+         +------------------+         +------------------+
| id (PK)          |         | id (PK)          |         | id (PK)          |
| username         |         | anonymous_alias  |         | post_id (FK)     |
| email (Unique)   |         | content          |         | anonymous_alias  |
| password (Hash)  |         | category         |         | content          |
| role             |         | upvotes          |         | created_at       |
| created_at       |         | downvotes        |         +------------------+
+------------------+         | comment_count    |
                             | created_at       |
                             +------------------+
                                       |
                                       v
                             +------------------+
                             |      votes       |
                             +------------------+
                             | id (PK)          |
                             | user_id (FK)     |
                             | post_id (FK)     |
                             | vote_type        |
                             +------------------+
```

*Crucial Design Consideration*: `posts` and `comments` do not maintain hard foreign keys to `users` in client responses; all publicly visible author identities use generated anonymous pseudonyms.

---

## Caching Strategy

The Redis configuration is defined in [`RedisConfig.java`](file:///E:/project/resume/Campus/Whisper/CampusWhisper-Backend/src/main/java/com/campus/whisper/config/RedisConfig.java):
- **Cache Eviction**: Automatic TTL expiration set to 10 minutes.
- **Serialization**: `GenericJackson2JsonRedisSerializer` enables human-readable JSON inspection in Redis CLI tools.
- **Key Namespacing**: Prefixes prevent collision across post queries, comment streams, and vote aggregations.

---

## Security & Cryptography

1. **Password Protection**: BCrypt algorithm with salted rounds configured in `WebSecurityConfig.java`.
2. **JWT Signing**: JSON Web Tokens are signed using HMAC-SHA256 with a 256-bit Base64-encoded key (`jwt.secret`).
3. **CORS Policy**: Restricts cross-origin resource sharing to trusted frontend origins (`http://localhost:5173`, `http://localhost:3000`).
4. **Decoupled Architecture**: Personal account records are segregated from published whisper records.

---

## Testing

Execute the test suite using Maven:

```bash
./mvnw test
```

---

## License

This project is licensed under the MIT License.
