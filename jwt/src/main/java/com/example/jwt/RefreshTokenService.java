package com.example.jwt;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redis;
    private final long refreshDays;

    public RefreshTokenService(StringRedisTemplate redis,
                               @Value("${jwt.refresh-days}") long refreshDays) {
        this.redis = redis;
        this.refreshDays = refreshDays;
    }

    private String key(String refreshToken) {
        return "rt:" + refreshToken;
    }

    // 로그인 시 발급 + Redis 저장
    public String issue(String username) {
        String refreshToken = UUID.randomUUID().toString(); // opaque token
        redis.opsForValue().set(key(refreshToken), username, Duration.ofDays(refreshDays));
        return refreshToken;
    }

    // refresh 시 검증(존재하면 username 반환, 없으면 null)
    public String getUsernameIfValid(String refreshToken) {
        return redis.opsForValue().get(key(refreshToken));
    }

    // 로그아웃(폐기)
    public void revoke(String refreshToken) {
        redis.delete(key(refreshToken));
    }

    // (선택) refresh 할 때 토큰 회전: 기존 토큰 삭제 + 새 토큰 발급
    public String rotate(String oldRefreshToken, String username) {
        revoke(oldRefreshToken);
        return issue(username);
    }
}