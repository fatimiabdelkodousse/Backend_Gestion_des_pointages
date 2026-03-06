package com.example.gestionpointage.service;

import com.example.gestionpointage.entity.AccountToken;
import com.example.gestionpointage.model.TokenType;
import com.example.gestionpointage.repository.AccountTokenRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;
import com.example.gestionpointage.security.LoginProtectionService;
import com.example.gestionpointage.security.SecureTokenGenerator;
import com.example.gestionpointage.security.TokenHashUtil;
import com.example.gestionpointage.model.Utilisateur;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ForgotPasswordService {

    private final UtilisateurRepository utilisateurRepository;
    private final AccountTokenRepository tokenRepository;
    private final LoginProtectionService protectionService;
    private final EmailService emailService;

    public ForgotPasswordService(
            UtilisateurRepository utilisateurRepository,
            AccountTokenRepository tokenRepository,
            LoginProtectionService protectionService,
            EmailService emailService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.tokenRepository = tokenRepository;
        this.protectionService = protectionService;
        this.emailService = emailService;
    }

    @Transactional
    public void process(
            String email,
            String nom,
            String prenom,
            String badgeUid,
            String ip
    ) {

        String key = "FORGOT:" + ip + ":" + email;
        protectionService.check(key);

        String normalizedBadgeUid = (badgeUid == null || badgeUid.isBlank())
                ? null
                : badgeUid.trim();

        Utilisateur user = utilisateurRepository
                .findActiveUserForPasswordReset(
                        email.trim(),
                        nom.trim(),
                        prenom.trim(),
                        normalizedBadgeUid
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Informations incorrectes"
                        )
                );

        protectionService.success(key);

        tokenRepository.deleteByUtilisateurAndTypeAndUsedFalse(
                user,
                TokenType.RESET
        );

        String token = SecureTokenGenerator.generate();
        String hash  = TokenHashUtil.hash(token);

        AccountToken t = new AccountToken();
        t.setUtilisateur(user);
        t.setTokenHash(hash);
        t.setType(TokenType.RESET);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        t.setUsed(false);

        tokenRepository.save(t);

        String link = "https://gestion-pointages.up.railway.app/reset-password?token="
                + token;

        emailService.sendResetLinkEmail(
                user.getEmail(),
                user.getPrenom(),
                user.getNom(),
                link
        );
    }
}