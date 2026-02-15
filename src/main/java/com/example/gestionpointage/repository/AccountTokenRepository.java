package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.AccountToken;

import com.example.gestionpointage.model.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gestionpointage.model.Utilisateur;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findByTokenHashAndUsedFalseAndType(
            String tokenHash,
            TokenType type
    );
    
    Optional<AccountToken> findByTokenHashAndUsedFalse(
            String tokenHash
    );
    
    void deleteByUtilisateurAndTypeAndUsedFalse(
            Utilisateur utilisateur,
            TokenType type
    );
    
    Optional<AccountToken> 
    findTopByUtilisateurAndTypeAndUsedFalseOrderByCreatedAtDesc(
            Utilisateur utilisateur,
            TokenType type
    );
    
    void deleteByExpiresAtBefore(LocalDateTime time);
}
