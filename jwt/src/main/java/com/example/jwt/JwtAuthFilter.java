package com.example.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final AccessTokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtTokenProvider tokenProvider,
                         AccessTokenBlacklistService blacklistService) {
        this.tokenProvider = tokenProvider;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            // ✅ 블랙리스트면 즉시 차단
            if (blacklistService.isBlacklisted(token)) {
                response.setStatus(401);
                return;
            }

            try {
                String username = tokenProvider.getUsername(token);

                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}