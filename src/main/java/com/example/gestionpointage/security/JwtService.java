package com.example.gestionpointage.security;

import io.jsonwebtoken.Claims;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final Key key;
    private final long expirationMs;
    private static final long ACCESS_EXPIRATION  = 15 * 60 * 1000;   // 15 minutes
    private static final long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 days

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMinutes * 60 * 1000;
    }

    // 🔐 Generate JWT
    public String generateToken(String userId, String email, String role) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
        	    .setClaims(claims)      // ✅ أولاً
        	    .setSubject(userId)     // ✅ ثم الـ subject
        	    .setIssuedAt(new Date())
        	    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
        	    .signWith(key, SignatureAlgorithm.HS256)
        	    .compact();
    }
    
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(
                    new Date(System.currentTimeMillis() + REFRESH_EXPIRATION)
                )
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .claim("type", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(
                    new Date(System.currentTimeMillis() + ACCESS_EXPIRATION)
                )
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    // ✅ Validate & parse
    private Claims parse(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    public String extractType(String token) {
        return parse(token).get("type", String.class);
    }
    
    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}