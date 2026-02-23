package com.example.jwt;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authManager, JwtTokenProvider tokenProvider) {
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        var token = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        authManager.authenticate(token); // 여기서 실패하면 401(예외)

        String accessToken = tokenProvider.createAccessToken(req.username());
        return Map.of("accessToken", accessToken);
    }

    public record LoginRequest(String username, String password) {}
}