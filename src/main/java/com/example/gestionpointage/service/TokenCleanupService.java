package com.example.gestionpointage.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

import com.example.gestionpointage.repository.AccountTokenRepository;

@Service
public class TokenCleanupService {

    private final AccountTokenRepository repository;

    public TokenCleanupService(AccountTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 * * * *") // كل ساعة
    public void cleanExpiredTokens() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}

