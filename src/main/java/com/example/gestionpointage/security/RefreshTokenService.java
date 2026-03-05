package com.example.gestionpointage.security;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.gestionpointage.dto.RefreshTokenResponseDTO;
import com.example.gestionpointage.entity.RefreshToken;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.RefreshTokenRepository;
import com.example.gestionpointage.model.Role;

import java.util.List;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    // ══════════════════════════════════════════════
    // 🔄 ROTATE
    // ══════════════════════════════════════════════

    public RefreshTokenResponseDTO rotate(
            String rawToken,
            String ip,
            String userAgent
    ) {
        String hash = TokenHashUtil.hash(rawToken);

        // 1️⃣ البحث عن التوكن
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                );

        // 2️⃣ التحقق من الصلاحية
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Utilisateur user = storedToken.getUser();

        // 3️⃣ حذف التوكن القديم (بدل revoke)
        refreshTokenRepository.delete(storedToken);

        // 4️⃣ Access token جديد
        String newAccessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole().name()
        );

        // 5️⃣ Refresh token جديد
        String newRawRefreshToken = SecureTokenGenerator.generate();
        String newHash = TokenHashUtil.hash(newRawRefreshToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setTokenHash(newHash);
        newToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        newToken.setIpAddress(ip);
        newToken.setUserAgent(userAgent);
        newToken.setRevoked(false);

        refreshTokenRepository.save(newToken);

        return new RefreshTokenResponseDTO(
                newAccessToken,
                newRawRefreshToken
        );
    }

    // ══════════════════════════════════════════════
    // 🔐 CREATE (on login — one per device)
    // ══════════════════════════════════════════════

    public String createRefreshToken(
            Utilisateur user,
            String ip,
            String userAgent
    ) {
        // حذف كل التوكنز السابقة لنفس الجهاز
        refreshTokenRepository.deleteAllByUserAndUserAgent(user, userAgent);

        String rawToken = SecureTokenGenerator.generate();
        String hash = TokenHashUtil.hash(rawToken);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setIpAddress(ip);
        token.setUserAgent(userAgent);
        token.setRevoked(false);

        refreshTokenRepository.save(token);

        return rawToken;
    }

    // ══════════════════════════════════════════════
    // 🚪 LOGOUT
    // ══════════════════════════════════════════════

    public void logout(String rawRefreshToken) {
        String hash = TokenHashUtil.hash(rawRefreshToken);

        // حذف التوكن — إذا غير موجود، لا مشكلة
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(refreshTokenRepository::delete);
    }

    public void logoutAll(Utilisateur user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    public void logoutAllEmployees(List<Long> userIds) {
        refreshTokenRepository.deleteAllByUserIdsAndRole(userIds, Role.EMPLOYE);
    }

    public void logoutAllEmployees() {
        refreshTokenRepository.deleteAllByRole(Role.EMPLOYE);
    }
}