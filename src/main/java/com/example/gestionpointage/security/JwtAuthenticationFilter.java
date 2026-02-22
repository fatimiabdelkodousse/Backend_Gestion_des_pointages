package com.example.gestionpointage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("🔍 JWT FILTER: " + method + " " + path);

        // ═══ تجاوز المسارات العامة تماماً ═══
        if (isPublicPath(path)) {
            System.out.println("⏭️ SKIPPING (public): " + path);
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("⚠️ NO TOKEN for: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String type = jwtService.extractType(token);

            if (!"ACCESS".equals(type)) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("✅ AUTH SET: " + role);

        } catch (Exception e) {
            System.out.println("❌ JWT ERROR: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/reset-password")
                || path.startsWith("/auth/")
                || path.startsWith("/pointages")
                || path.startsWith("/error")
                || path.startsWith("/ws");
    }

    // ═══ حذف shouldNotFilter ═══
}