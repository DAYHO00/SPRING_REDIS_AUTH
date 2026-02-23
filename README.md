# JWT-Redis Auth Server

Spring Boot + JWT + Redis 기반의  
Access/Refresh Token 구조를 적용한 실무형 인증 서버

Redis를 활용한 토큰 회전(Rotation) 및 블랙리스트 관리로  
Stateless 구조를 유지하면서도 즉시 로그아웃을 지원합니다.

---

## 📌 목차

- [🎯 주요 기능](#-주요-기능)
- [⚙️ 기술 스택](#-기술-스택)
- [🚀 시작하기](#-시작하기)
- [🔐 API 사용 예시](#-api-사용-예시)
- [💡 핵심 구현 내용](#-핵심-구현-내용)
- [🔄 데이터 흐름](#-데이터-흐름)

---

## 🎯 주요 기능

### 🔐 JWT 기반 인증
- Access Token 발급 (기본 30분)
- Refresh Token 발급 (기본 7일)
- Authorization Header 기반 인증 처리

### 🧰 Redis 기반 토큰 관리
- Refresh Token Redis 저장 (TTL 적용)
- Access Token 블랙리스트 관리
- 로그아웃 즉시 토큰 무효화

### 🛡 보안 강화 설계
- Refresh Token Rotation 지원
- Access Token 만료 시간 기반 블랙리스트 TTL 설정
- Stateless 인증 구조 유지

---

## ⚙️ 기술 스택

- **Language**: Java 17  
- **Framework**: Spring Boot 3.x  
- **Security**: Spring Security  
- **Authentication**: JWT (jjwt 0.12.x)  
- **Cache / Session Store**: Redis 7+  
- **Build Tool**: Gradle  

---

## 🚀 시작하기

### 1️⃣ 사전 요구사항

- Java 17+
- Redis 7+
- Gradle

---

### 2️⃣ Redis 실행 (Docker)

```bash
docker run -d -p 6379:6379 redis:7-alpine
```

---

### 3️⃣ application.yml 설정

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

### 4️⃣ 서버 실행

```bash
./gradlew bootRun
```

---

## 🔐 API 사용 예시

### 1️⃣ 로그인

```
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

### 2️⃣ 보호된 API 호출

```
GET /me
```

Header:

```
Authorization: Bearer {accessToken}
```

---

### 3️⃣ 토큰 재발급

```
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

### 4️⃣ 로그아웃

```
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

## 💡 핵심 구현 내용

### 1️⃣ Refresh Token Redis 저장

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

### 2️⃣ Access Token 블랙리스트 처리

```java
blacklistService.blacklist(
    accessToken,
    Duration.ofMillis(remainingMillis)
);
```

- 남은 만료 시간만큼 TTL 설정
- 로그아웃 즉시 무효화

---

### 3️⃣ JWT 필터 인증 처리

```java
if (blacklistService.isBlacklisted(token)) {
    response.setStatus(401);
    return;
}
```

- 요청마다 블랙리스트 확인
- Stateless 인증 유지

---

## 🔄 데이터 흐름

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
