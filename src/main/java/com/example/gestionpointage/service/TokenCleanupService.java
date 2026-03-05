package com.example.gestionpointage.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.example.gestionpointage.repository.AccountTokenRepository;

@Service
public class TokenCleanupService {

    private final AccountTokenRepository repository;

    public TokenCleanupService(AccountTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void cleanTokens() {

        int expiredCount = repository.deleteAllExpired(LocalDateTime.now());
        int usedCount    = repository.deleteAllUsed();

        if (expiredCount > 0 || usedCount > 0) {
            System.out.println("🧹 Token cleanup: "
                    + expiredCount + " expired, "
                    + usedCount + " used deleted");
        }
    }
}