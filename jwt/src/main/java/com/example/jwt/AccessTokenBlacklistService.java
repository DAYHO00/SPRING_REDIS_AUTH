package com.example.jwt;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenBlacklistService {

    private final StringRedisTemplate redis;

    public AccessTokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String accessToken) {
        return "bl:at:" + accessToken;
    }

    public void blacklist(String accessToken, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) return;
        redis.opsForValue().set(key(accessToken), "1", ttl);
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redis.hasKey(key(accessToken)));
    }
}