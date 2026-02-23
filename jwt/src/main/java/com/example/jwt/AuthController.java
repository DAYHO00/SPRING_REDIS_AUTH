package com.example.jwt;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final AccessTokenBlacklistService blacklistService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authManager,
                          JwtTokenProvider tokenProvider,
                          AccessTokenBlacklistService blacklistService,
                          RefreshTokenService refreshTokenService) {
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.blacklistService=blacklistService;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        var token = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        authManager.authenticate(token);

        String accessToken = tokenProvider.createAccessToken(req.username());
        String refreshToken = refreshTokenService.issue(req.username());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestBody RefreshRequest req) {
        String username = refreshTokenService.getUsernameIfValid(req.refreshToken());
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String newAccessToken = tokenProvider.createAccessToken(username);

        // 회전(선택이지만 실무 느낌 좋아서 기본 ON)
        String newRefreshToken = refreshTokenService.rotate(req.refreshToken(), username);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

   @PostMapping("/logout")
    public Map<String, String> logout(
            @RequestBody RefreshRequest req,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        // 1) refresh 폐기
        refreshTokenService.revoke(req.refreshToken());

        // 2) access도 있으면 블랙리스트 처리
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String accessToken = auth.substring(7);
            long remaining = tokenProvider.getRemainingMillis(accessToken);
            blacklistService.blacklist(accessToken, java.time.Duration.ofMillis(remaining));
        }

        return Map.of("result", "OK");
    }
    public record LoginRequest(String username, String password) {}
    public record RefreshRequest(String refreshToken) {}
}