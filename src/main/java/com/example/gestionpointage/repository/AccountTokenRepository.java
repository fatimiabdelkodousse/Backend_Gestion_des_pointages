package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.AccountToken;
import com.example.gestionpointage.model.TokenType;
import com.example.gestionpointage.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountTokenRepository
        extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findByTokenHashAndUsedFalseAndType(
            String tokenHash,
            TokenType type
    );

    Optional<AccountToken> findByTokenHashAndUsedFalse(
            String tokenHash
    );

    Optional<AccountToken>
    findTopByUtilisateurAndTypeAndUsedFalseOrderByCreatedAtDesc(
            Utilisateur utilisateur,
            TokenType type
    );

    List<AccountToken> findByUtilisateurAndTypeAndUsedFalse(
            Utilisateur utilisateur,
            TokenType type
    );

    // ══════════════════════════════════════════
    // ✅ حذف توكنات مستخدم معيّن
    // ══════════════════════════════════════════

    @Modifying
    @Transactional
    void deleteByUtilisateurAndTypeAndUsedFalse(
            Utilisateur utilisateur,
            TokenType type
    );

    // ══════════════════════════════════════════
    // ✅ حذف التوكنات المنتهية
    // ══════════════════════════════════════════

    @Modifying
    @Transactional
    @Query("DELETE FROM AccountToken t WHERE t.expiresAt < :now")
    int deleteAllExpired(LocalDateTime now);

    // ══════════════════════════════════════════
    // ✅ حذف التوكنات المستعملة
    // ══════════════════════════════════════════

    @Modifying
    @Transactional
    @Query("DELETE FROM AccountToken t WHERE t.used = true")
    int deleteAllUsed();

    // ══════════════════════════════════════════
    // القديم (يمكن حذفه)
    // ══════════════════════════════════════════

    void deleteByExpiresAtBefore(LocalDateTime time);
}