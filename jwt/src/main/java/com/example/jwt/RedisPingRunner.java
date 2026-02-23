package com.example.jwt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPingRunner implements CommandLineRunner {
    
    private final StringRedisTemplate redis;

    public RedisPingRunner(StringRedisTemplate redis){
        this.redis=redis;
    }

    @Override
    public void run(String... args){
        redis.opsForValue().set("ping","pong");
        System.out.println("[REDIS]="+redis.opsForValue().get("ping"));
    }
}
