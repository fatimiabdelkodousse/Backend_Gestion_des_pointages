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

    public RefreshTokenResponseDTO rotate(
            String rawToken,
            String ip,
            String userAgent
    ) {
        // 1️⃣ Hash incoming token
        String hash = TokenHashUtil.hash(rawToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                );

        // 2️⃣ Expired
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 3️⃣ Reuse attack detection
        if (storedToken.isRevoked()) {
            revokeAllUserTokens(storedToken.getUser());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 4️⃣ Revoke old token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        Utilisateur user = storedToken.getUser();

        // 5️⃣ New access token (JWT)
        String newAccessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole().name()
        );

        // 6️⃣ New refresh token (opaque)
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

    private void revokeAllUserTokens(Utilisateur user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
    
    public void logout(String rawRefreshToken) {

        String hash = TokenHashUtil.hash(rawRefreshToken);

        RefreshToken token = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED)
                );

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
    
    public void logoutAll(Utilisateur user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    public void logoutAllEmployees(List<Long> userIds) {
        refreshTokenRepository.revokeAllByUserIdsAndRole(userIds, Role.EMPLOYE);
    }

    public void logoutAllEmployees() {
        refreshTokenRepository.revokeAllByRole(Role.EMPLOYE);
    }
    
    public String createRefreshToken(Utilisateur user, String ip, String userAgent) {

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

}
