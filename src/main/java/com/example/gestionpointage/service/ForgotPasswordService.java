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
import java.util.List;
import java.util.Optional;

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

        // ✅ Log لتسهيل التشخيص
        System.out.println("🔍 Forgot password request:");
        System.out.println("   email: " + email);
        System.out.println("   nom: " + nom);
        System.out.println("   prenom: " + prenom);
        System.out.println("   badgeUid: " + normalizedBadgeUid);

        Utilisateur user = utilisateurRepository
                .findActiveUserForPasswordReset(
                        email.trim(),
                        nom.trim(),
                        prenom.trim(),
                        normalizedBadgeUid
                )
                .orElseThrow(() -> {
                    System.out.println("❌ User not found for password reset");
                    return new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Informations incorrectes"
                    );
                });

        System.out.println("✅ User found: " + user.getId()
                + " - " + user.getNom() + " " + user.getPrenom());

        // ==============================
        // 🔐 RATE LIMIT (5 min)
        // ==============================

        Optional<AccountToken> existingToken =
                tokenRepository.findTopByUtilisateurAndTypeAndUsedFalseOrderByCreatedAtDesc(
                        user,
                        TokenType.RESET
                );

        if (existingToken.isPresent()) {
            AccountToken lastToken = existingToken.get();
            if (lastToken.getCreatedAt()
                    .isAfter(LocalDateTime.now().minusMinutes(5))) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait before requesting another reset link."
                );
            }
        }

        // ==============================
        // 🧹 Invalider les anciens tokens
        // ==============================

        List<AccountToken> oldTokens =
                tokenRepository.findByUtilisateurAndTypeAndUsedFalse(
                        user,
                        TokenType.RESET
                );

        for (AccountToken old : oldTokens) {
            old.setUsed(true);
            tokenRepository.save(old);
        }

        // ==============================
        // 🔑 Nouveau token
        // ==============================

        String token = SecureTokenGenerator.generate();
        String hash = TokenHashUtil.hash(token);

        AccountToken t = new AccountToken();
        t.setUtilisateur(user);
        t.setTokenHash(hash);
        t.setType(TokenType.RESET);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        t.setUsed(false);

        tokenRepository.save(t);

        String link = "https://gestion-pointage.up.railway.app/reset-password?token=" + token;

        System.out.println("🔗 Reset link generated for: " + user.getEmail());

        // ✅ Email avec try-catch pour ne pas bloquer la réponse
        try {
            emailService.sendResetLinkEmail(
                    user.getEmail(),
                    user.getPrenom(),
                    user.getNom(),
                    link
            );
            System.out.println("✅ Email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.out.println("❌ Email sending failed: " + e.getMessage());
            e.printStackTrace();
            // Ne pas faire échouer la requête si l'email échoue
        }
    }
}