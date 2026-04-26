URL Shortener Service — Spring Boot
A production-grade URL shortener built with Spring Boot 3, PostgreSQL, and Redis.

Architecture
Client
│
▼
┌──────────────────────────────────────────┐
│           Spring Boot Application         │
│                                          │
│  ┌─────────────┐   ┌──────────────────┐  │
│  │UrlController│──▶│  UrlServiceImpl  │  │
│  └─────────────┘   └────────┬─────────┘  │
│                             │            │
│                    ┌────────┴────────┐   │
│                    │                 │   │
│              ┌─────▼──────┐  ┌──────▼─┐ │
│              │UrlRepository│  │ Redis  │ │
│              │(PostgreSQL) │  │ Cache  │ │
│              └────────────┘  └────────┘ │
└──────────────────────────────────────────┘
Tech Stack
Layer	Technology
Framework	Spring Boot 3.2, Java 21
Database	PostgreSQL 16 + Spring Data JPA
Cache	Redis 7 (30-min TTL on redirects)
Security	Spring Security (HTTP Basic / JWT ready)
Rate Limiting	Bucket4j (60 req/min per IP)
Observability	Micrometer + Prometheus
API Docs	Springdoc OpenAPI / Swagger UI
Build	Maven, Docker, Docker Compose
Testing	JUnit 5, Mockito, MockMvc
Quick Start
Prerequisites
Java 21+
Docker & Docker Compose
Run with Docker Compose
git clone <repo-url>
cd url-shortener
docker-compose up -d
Service available at: http://localhost:8080 Swagger UI: http://localhost:8080/swagger-ui.html

Run locally (without Docker)
# Start dependencies
docker-compose up -d postgres redis

# Run the app
./mvnw spring-boot:run
API Reference
Create Short URL
POST /api/v1/urls
Authorization: Basic dXNlcjpwYXNzd29yZA==
Content-Type: application/json

{
"originalUrl": "https://example.com/very/long/path",
"customAlias": "my-link",   // optional
"ttlDays": 30               // optional, null = no expiry
}
Response 201:

{
"id": 1,
"shortCode": "my-link",
"shortUrl": "http://localhost:8080/my-link",
"originalUrl": "https://example.com/very/long/path",
"expiresAt": "2025-05-26T12:00:00",
"clickCount": 0,
"active": true,
"createdAt": "2025-04-26T12:00:00"
}
Redirect
GET /{shortCode}
→ HTTP 301 Location: https://example.com/...
Get URL Info
GET /api/v1/urls/{shortCode}
Update URL
PATCH /api/v1/urls/{id}
Authorization: Basic ...

{ "originalUrl": "https://new-url.com", "active": true }
Delete URL (soft delete)
DELETE /api/v1/urls/{id}
Authorization: Basic ...
List My URLs
GET /api/v1/urls?page=0&size=20
Authorization: Basic ...
Analytics
GET /api/v1/urls/{shortCode}/analytics
Authorization: Basic ...
Response:

{
"urlId": 1,
"shortCode": "my-link",
"totalClicks": 1234,
"clicksByCountry": { "US": 800, "GB": 200, "DE": 100 },
"dailyClicks": [
{ "date": "2025-04-25", "clicks": 42 }
]
}
Project Structure
src/
├── main/
│   ├── java/com/urlshortener/
│   │   ├── UrlShortenerApplication.java
│   │   ├── controller/
│   │   │   └── UrlController.java          # REST endpoints + redirect
│   │   ├── service/
│   │   │   ├── UrlService.java             # Interface
│   │   │   ├── UrlServiceImpl.java         # Business logic
│   │   │   └── UrlMapper.java              # MapStruct entity↔DTO
│   │   ├── repository/
│   │   │   ├── UrlRepository.java          # JPA queries
│   │   │   └── ClickEventRepository.java   # Analytics queries
│   │   ├── model/
│   │   │   ├── Url.java                    # URL entity
│   │   │   └── ClickEvent.java             # Analytics entity
│   │   ├── dto/
│   │   │   └── UrlDtos.java                # All request/response DTOs
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── UrlNotFoundException.java
│   │   │   ├── UrlExpiredException.java
│   │   │   └── AliasAlreadyExistsException.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java         # Spring Security
│   │   │   ├── CacheConfig.java            # Redis cache manager
│   │   │   ├── RateLimitFilter.java        # Bucket4j rate limiter
│   │   │   └── OpenApiConfig.java          # Swagger config
│   │   └── util/
│   │       └── Base62CodeGenerator.java    # Secure random code generator
│   └── resources/
│       ├── application.yml
│       └── application-test.yml
└── test/
└── java/com/urlshortener/
├── service/UrlServiceImplTest.java
└── controller/UrlControllerTest.java
Best Practices Applied
Layered Architecture — Controller → Service (interface) → Repository
Short Code Generation — Base62, SecureRandom, collision retry
Caching — Redis caches hot redirect lookups (30-min TTL); evicted on update/delete
Rate Limiting — Bucket4j token bucket, 60 req/min per IP
Soft Delete — URLs are deactivated, not physically removed
Analytics — Click events stored with anonymized IP (GDPR)
Scheduled Cleanup — Daily job deactivates expired URLs
Validation — Bean Validation on all DTOs, custom regex patterns
Global Error Handling — Consistent JSON error envelope
Observability — Micrometer counters, Prometheus endpoint, Actuator
Security — Stateless, HTTP Basic (JWT-ready), public redirect endpoint
Testing — Unit tests (Mockito) + controller slice tests (MockMvc)
Docker — Multi-stage Dockerfile, non-root user, Docker Compose
Default Credentials (demo)
User	Password
user	password
admin	admin
Replace with JWT + database-backed users for production.