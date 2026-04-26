# 🚀 URL Shortener Service — Spring Boot

A **production-grade URL shortener** built with modern backend technologies, designed for **performance, scalability, and reliability**.

---

## ✨ Features

* 🔗 Shorten long URLs instantly
* ⚡ High-performance redirects with Redis caching
* 📊 Built-in analytics (click tracking, geo insights)
* 🔒 Secure (Spring Security, JWT-ready)
* 🚦 Rate limiting per IP (Bucket4j)
* ⏳ Expiring links (TTL support)
* ♻️ Soft delete (data safety)
* 📈 Observability (Micrometer + Prometheus)
* 🐳 Fully Dockerized setup

---

## 🏗️ Architecture

```
Client
  │
  ▼
┌──────────────────────────────────────────┐
│        Spring Boot Application           │
│                                          │
│  ┌─────────────┐   ┌──────────────────┐  │
│  │UrlController│──▶│  UrlServiceImpl  │  │
│  └─────────────┘   └────────┬─────────┘  │
│                             │            │
│                    ┌────────┴────────┐   │
│                    │                 │   │
│              ┌─────▼──────┐  ┌──────▼─┐ │
│              │ PostgreSQL │  │ Redis  │ │
│              │ (Database) │  │ Cache  │ │
│              └────────────┘  └────────┘ │
└──────────────────────────────────────────┘
```

---

## 🧰 Tech Stack

| Layer         | Technology                          |
| ------------- | ----------------------------------- |
| Framework     | Spring Boot 3.2, Java 21            |
| Database      | PostgreSQL 16 + Spring Data JPA     |
| Cache         | Redis 7                             |
| Security      | Spring Security (Basic / JWT-ready) |
| Rate Limiting | Bucket4j                            |
| Observability | Micrometer + Prometheus             |
| API Docs      | Swagger (Springdoc OpenAPI)         |
| Build         | Maven, Docker, Docker Compose       |
| Testing       | JUnit 5, Mockito, MockMvc           |

---

## ⚡ Quick Start

### 🔧 Prerequisites

* Java 21+
* Docker & Docker Compose

---

### 🐳 Run with Docker

```bash
git clone <repo-url>
cd url-shortener
docker-compose up -d
```

👉 App: [http://localhost:8080](http://localhost:8080)
👉 Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### 💻 Run Locally

```bash
# Start dependencies
docker-compose up -d postgres redis

# Run application
./mvnw spring-boot:run
```

---

## 📡 API Reference

### 🔗 Create Short URL

**POST** `/api/v1/urls`

```json
{
  "originalUrl": "https://example.com/very/long/path",
  "customAlias": "my-link",
  "ttlDays": 30
}
```

✅ Response:

```json
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
```

---

### 🔁 Redirect

**GET** `/{shortCode}`
➡️ Returns **HTTP 301 Redirect**

---

### 📄 Get URL Info

**GET** `/api/v1/urls/{shortCode}`

---

### ✏️ Update URL

**PATCH** `/api/v1/urls/{id}`

```json
{
  "originalUrl": "https://new-url.com",
  "active": true
}
```

---

### 🗑️ Delete URL (Soft Delete)

**DELETE** `/api/v1/urls/{id}`

---

### 📋 List URLs

**GET** `/api/v1/urls?page=0&size=20`

---

### 📊 Analytics

**GET** `/api/v1/urls/{shortCode}/analytics`

```json
{
  "urlId": 1,
  "shortCode": "my-link",
  "totalClicks": 1234,
  "clicksByCountry": {
    "US": 800,
    "GB": 200,
    "DE": 100
  },
  "dailyClicks": [
    { "date": "2025-04-25", "clicks": 42 }
  ]
}
```

---

## 📂 Project Structure

```
src/
├── main/java/com/urlshortener/
│   ├── controller/        # REST endpoints
│   ├── service/           # Business logic
│   ├── repository/        # Data access layer
│   ├── model/             # Entities
│   ├── dto/               # Request/Response DTOs
│   ├── exception/         # Global error handling
│   ├── config/            # Security, Cache, RateLimit
│   └── util/              # Utilities (Base62 generator)
│
└── test/
    ├── service/           # Unit tests
    └── controller/        # API tests
```

---

## 🧠 Key Design Decisions

### 🔑 Short Code Generation

* Base62 encoding
* SecureRandom-based
* Collision-safe with retry logic

### ⚡ Caching Strategy

* Redis for hot redirects
* TTL: 30 minutes
* Auto-eviction on update/delete

### 🚦 Rate Limiting

* Token Bucket algorithm
* 60 requests/min per IP

### 🗃️ Data Management

* Soft delete instead of hard delete
* Expired URLs automatically deactivated

### 📊 Analytics

* Click tracking with anonymized IP
* Country-based aggregation
* Daily statistics

---

## 🔒 Security

* Stateless architecture
* HTTP Basic (for demo)
* JWT-ready for production
* Public redirect endpoint

---

## 🧪 Testing

* ✅ Unit Tests (Mockito)
* ✅ Controller Tests (MockMvc)
* ✅ Clean separation of concerns

---

## 🐳 Docker

* Multi-stage build
* Lightweight image
* Non-root user for security
* Full environment via Docker Compose

---

## 🔐 Demo Credentials

| User  | Password |
| ----- | -------- |
| user  | password |
| admin | admin    |

> ⚠️ Replace with **JWT + database-backed users** in production

---

## 📈 Future Improvements

* JWT authentication
* Custom domain support
* QR code generation
* Advanced analytics dashboard
* Distributed rate limiting (Redis-based)

---

## 👩‍💻 Author

Built with ❤️ using Spring Boot
