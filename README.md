# 🛡️ Insurance Policy Management System

A **production-quality enterprise Insurance Platform** built with Java 21, Spring Boot 3.x, and PostgreSQL. Demonstrates real-world backend architecture including JWT authentication, role-based access control, automated risk assessment, and a scheduled policy expiry engine.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT (React/Postman)                │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP/HTTPS
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  SPRING SECURITY LAYER                   │
│  JwtAuthenticationFilter → SecurityContextHolder        │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│              CONTROLLER LAYER (REST API)                 │
│  AuthController │ CustomerController │ PolicyController  │
│  ClaimController │ RiskController │ DashboardController  │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                   SERVICE LAYER                          │
│  AuthService │ CustomerService │ PolicyService           │
│  ClaimService │ RiskAssessmentService │ DashboardService │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│         REPOSITORY LAYER (Spring Data JPA)              │
│  UserRepo │ CustomerRepo │ PolicyRepo │ ClaimRepo        │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                  POSTGRESQL DATABASE                     │
│   Managed by Flyway migrations (V1–V6)                  │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (JJWT) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Mapping | MapStruct |
| Build | Maven |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Testing | JUnit 5 + Mockito |
| Containerization | Docker + Docker Compose |
| Utilities | Lombok, BCrypt |

---

## 📁 Project Structure

```
Insurance_Policy_Management/
├── backend/                    ← Spring Boot application
│   ├── src/main/java/com/insurance/
│   │   ├── config/             ← ApplicationConfig, SecurityConfig, SwaggerConfig
│   │   ├── controller/         ← REST controllers
│   │   ├── dto/
│   │   │   ├── request/        ← Incoming request DTOs
│   │   │   └── response/       ← Outgoing response DTOs
│   │   ├── entity/             ← JPA entities
│   │   ├── enums/              ← Role, PolicyStatus, ClaimStatus, RiskLevel, PolicyType
│   │   ├── exception/          ← Custom exceptions + GlobalExceptionHandler
│   │   ├── mapper/             ← MapStruct mappers
│   │   ├── repository/         ← Spring Data JPA repositories
│   │   ├── security/           ← JWT service, filter, UserDetailsService
│   │   └── service/            ← Business logic services
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-docker.yml
│   │   └── db/migration/       ← Flyway SQL migrations V1–V6
│   ├── src/test/               ← JUnit 5 + Mockito test suite
│   ├── Dockerfile              ← Multi-stage Docker build
│   ├── docker-compose.yml      ← PostgreSQL + App services
│   └── pom.xml
├── frontend/                   ← React/Vite (coming soon)
└── docs/                       ← Module documentation
    ├── module-01-project-setup.md
    ├── module-02-auth-security.md
    └── module-03-to-07-core-domain.md
```

---

## 🗄️ Database Schema

```
users ─── customers ─── policies ─── claims
              │
              └─── risk_assessments
```

| Table | Description |
|-------|-------------|
| `users` | Authentication — credentials, role, enabled flag |
| `customers` | Insurance profiles — demographics, contact, KYC |
| `policies` | Insurance contracts — type, coverage, premium, status |
| `claims` | Claim submissions — amount, status, review workflow |
| `risk_assessments` | Risk profiles — age/coverage/history scores, risk level |

---

## 🔐 Authentication

JWT-based stateless authentication with role hierarchy:

| Role | Access |
|------|--------|
| `ROLE_ADMIN` | Full system access including dashboard and user management |
| `ROLE_AGENT` | Customer, policy, and claim management |
| `ROLE_CUSTOMER` | Self-service: own profile, policies, and claims |

### Seeded Demo Users (from Flyway V6)

| Email | Password | Role |
|-------|----------|------|
| `admin@insurance.com` | `Password123!` | ADMIN |
| `agent.sarah@insurance.com` | `Password123!` | AGENT |
| `agent.mike@insurance.com` | `Password123!` | AGENT |
| `john.doe@email.com` | `Password123!` | CUSTOMER |
| `jane.smith@email.com` | `Password123!` | CUSTOMER |

---

## 📊 Premium Calculation

```
Annual Premium = Coverage Amount × Base Rate × Risk Multiplier

Base Rates:
  LIFE:     0.5%   HEALTH: 1.0%
  VEHICLE:  2.0%   PROPERTY: 0.8%
  TRAVEL:   3.0%

Risk Multipliers:
  LOW: 1.0x  |  MEDIUM: 1.5x  |  HIGH: 2.0x

Example: MEDIUM risk customer, HEALTH policy, 500,000 coverage, MONTHLY
  Monthly Premium = 500,000 × 0.010 × 1.5 / 12 = ₹625/month
```

---

## ⚠️ Business Rules

### Policies
- Status machine: `PENDING → ACTIVE → EXPIRED/CANCELLED`
- Auto-expiry at midnight via `@Scheduled` cron job
- Only `PENDING` or `CANCELLED` policies can be deleted

### Claims
1. Policy must be **ACTIVE** and in-force
2. Claim amount **≤ coverage amount**
3. Customer can only claim **own policies**
4. Incident date **within policy period**
5. `APPROVED`, `REJECTED`, `WITHDRAWN` are **terminal states** (immutable)

### Risk Scoring
- Recalculated on-demand via `/risk/assess/{customerId}`
- Drives premium multipliers in real time

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
cd backend
docker-compose up --build
```

App: http://localhost:8080/api/v1  
Swagger: http://localhost:8080/api/v1/swagger-ui.html

### Option 2: Local Development

**Prerequisites:** Java 21, Maven, PostgreSQL 16 running on port 5432

1. Create the database:
```sql
CREATE DATABASE insurance_db;
```

2. Configure `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/insurance_db
    username: postgres
    password: your-password
```

3. Run:
```bash
cd backend
mvn spring-boot:run
```

---

## 📖 API Documentation

Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

### Quick Reference

```http
# Authentication
POST /api/v1/auth/register
POST /api/v1/auth/login

# Customers
GET    /api/v1/customers
GET    /api/v1/customers/me
GET    /api/v1/customers/{id}
POST   /api/v1/customers/{userId}/profile
PUT    /api/v1/customers/{id}
DELETE /api/v1/customers/{id}

# Policies
GET    /api/v1/policies
POST   /api/v1/policies
GET    /api/v1/policies/{id}
PUT    /api/v1/policies/{id}
PATCH  /api/v1/policies/{id}/status
GET    /api/v1/policies/customer/{customerId}

# Claims
GET    /api/v1/claims
POST   /api/v1/claims
POST   /api/v1/claims/{id}/review
POST   /api/v1/claims/{id}/approve
POST   /api/v1/claims/{id}/reject
POST   /api/v1/claims/{id}/withdraw

# Risk
POST   /api/v1/risk/assess/{customerId}
GET    /api/v1/risk/{customerId}

# Dashboard
GET    /api/v1/dashboard
```

---

## 🧪 Tests

```bash
cd backend
mvn test
```

| Test Class | Coverage |
|-----------|---------|
| `AuthServiceTest` | Registration, login, BCrypt, role assignment |
| `ClaimServiceTest` | All 7 business rules + terminal states |
| `PolicyServiceTest` | Premium calculation for all types + @Scheduled |
| `RiskAssessmentServiceTest` | Scoring algorithm + LOW/HIGH risk scenarios |

---

## ⚙️ Environment Variables (Production)

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL |
| `POSTGRES_USER` | Database username |
| `POSTGRES_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded HMAC-SHA256 key (min 256 bits) |
| `JWT_EXPIRATION` | Token validity in ms (default: 86400000 = 24h) |

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.
