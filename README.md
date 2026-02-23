# JWT-Redis Auth Server

- Spring Boot + JWT + Redis 기반의 토큰 인증 및 세션 통제 서버
- Access Token + Refresh Token 구조
- Redis 기반 토큰 관리 (회전 및 블랙리스트) 구현
- Stateless 구조를 유지하면서 로그아웃 즉시 무효화 지원

---

### 🎯 Overview

- JWT 기반 Access / Refresh Token 발급
- Redis를 이용한 Refresh Token 저장 (TTL 적용)
- Access Token 블랙리스트 관리
- Refresh Token Rotation 지원
- 로그아웃 즉시 토큰 무효화
- Stateless 인증 구조 유지

---

### ⚙️ Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Security
- JWT (jjwt 0.12.x)
- Redis 7+
- Gradle

---

### 🚀 Getting Started

#### 1️⃣ Requirements

- Java 17+
- Redis 7+
- Gradle

---

#### 2️⃣ Redis 실행 (Docker)

```bash
docker run -d -p 6379:6379 redis:7-alpine
```

---

#### 3️⃣ application.yml 설정

```yaml
spring:
  application:
    name: jwt
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: change-this-secret-key-change-this-secret-key
  access-minutes: 30
  refresh-days: 7
```

---

#### 4️⃣ 서버 실행

```bash
./gradlew bootRun
```

---

### 🔐 API Usage

#### 1️⃣ 로그인

```http
POST /auth/login
```

```json
{
  "username": "daeho",
  "password": "1234"
}
```

응답:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "uuid-refresh-token"
}
```

---

#### 2️⃣ 보호된 API 호출

```http
GET /me
```

Header:

```
Authorization: Bearer {accessToken}
```

---

#### 3️⃣ 토큰 재발급

```http
POST /auth/refresh
```

```json
{
  "refreshToken": "uuid-refresh-token"
}
```

응답:

```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token"
}
```

---

#### 4️⃣ 로그아웃

```http
POST /auth/logout
```

Header:

```
Authorization: Bearer {accessToken}
```

Body:

```json
{
  "refreshToken": "uuid-refresh-token"
}
```

동작:

- Refresh Token Redis 삭제
- Access Token Redis 블랙리스트 등록
- 즉시 무효화

---

### 🔄 Token Flow

```
Client
   ↓
POST /auth/login
   ↓
Access + Refresh 발급
   ↓
Refresh → Redis 저장 (TTL)
   ↓
Authorization: Bearer AccessToken
   ↓
JwtAuthFilter 인증
   ↓
Protected API 접근
   ↓
Logout → Redis 블랙리스트 등록
   ↓
즉시 무효화
```

---

### 💡 Core Implementation

#### 1️⃣ Refresh Token Redis 저장

```java
redis.opsForValue().set(
    "rt:" + refreshToken,
    username,
    Duration.ofDays(7)
);
```

- Opaque Token(UUID) 사용
- TTL 기반 자동 만료

---

#### 2️⃣ Access Token 블랙리스트 처리

```java
blacklistService.blacklist(
    accessToken,
    Duration.ofMillis(remainingMillis)
);
```

- 남은 만료 시간만큼 TTL 설정
- 로그아웃 즉시 무효화

---

#### 3️⃣ JWT 필터 인증 처리

```java
if (blacklistService.isBlacklisted(token)) {
    response.setStatus(401);
    return;
}
```

- 요청마다 블랙리스트 확인
- Stateless 인증 유지

---
