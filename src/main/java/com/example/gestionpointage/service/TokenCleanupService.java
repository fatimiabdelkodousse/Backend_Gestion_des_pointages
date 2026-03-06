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

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanTokens() {

        LocalDateTime now = LocalDateTime.now();

        accountTokenRepository.deleteAllExpired(now);
        accountTokenRepository.deleteAllUsed();
        refreshTokenRepository.deleteAllExpired(now);
    }
}