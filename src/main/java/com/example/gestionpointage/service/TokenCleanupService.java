package com.example.gestionpointage.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.example.gestionpointage.repository.AccountTokenRepository;
import com.example.gestionpointage.repository.RefreshTokenRepository;

@Service
public class TokenCleanupService {

    private final AccountTokenRepository accountTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenCleanupService(
            AccountTokenRepository accountTokenRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.accountTokenRepository = accountTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedRate = 1800000) // 30 min
    @Transactional
    public void cleanTokens() {

        LocalDateTime now = LocalDateTime.now();

        // ── Account tokens ──
        int expiredAccount = accountTokenRepository.deleteAllExpired(now);
        int usedAccount    = accountTokenRepository.deleteAllUsed();

        // ── Refresh tokens ──
        int revokedRefresh = refreshTokenRepository.deleteAllRevoked();
        int expiredRefresh = refreshTokenRepository.deleteAllExpired(now);

        int total = expiredAccount + usedAccount + revokedRefresh + expiredRefresh;

        if (total > 0) {
            System.out.println("🧹 Token cleanup: "
                    + expiredAccount + " expired account, "
                    + usedAccount    + " used account, "
                    + revokedRefresh + " revoked refresh, "
                    + expiredRefresh + " expired refresh — deleted");
        }
    }
}