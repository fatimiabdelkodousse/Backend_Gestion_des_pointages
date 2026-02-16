package com.example.gestionpointage.service;

import com.example.gestionpointage.dto.CreateUserWithBadgeDTO;

import com.example.gestionpointage.entity.AccountToken;
import com.example.gestionpointage.event.AccountActivationEvent;
import com.example.gestionpointage.model.Badge;
import com.example.gestionpointage.model.TokenType;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.AccountTokenRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;
import com.example.gestionpointage.security.SecureTokenGenerator;
import com.example.gestionpointage.security.TokenHashUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            AccountTokenRepository accountTokenRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    public Utilisateur createUserWithBadge(CreateUserWithBadgeDTO dto) {

        // 🔎 البحث عن مستخدم بنفس الإيميل
        Utilisateur existingUser =
                utilisateurRepository.findByEmail(dto.email).orElse(null);

        Utilisateur user;

        if (existingUser != null) {

            // ❌ إذا موجود وغير محذوف → نرفض
            if (!existingUser.isDeleted()) {
                throw new RuntimeException("Email already exists");
            }

            // ♻️ إذا محذوف → نعيد تفعيله
            user = existingUser;
            user.setDeleted(false);
            user.setActive(false); // يحتاج تفعيل من جديد
            user.setNom(dto.nom);
            user.setPrenom(dto.prenom);
            user.setRole(dto.role);

        } else {

            // 🆕 مستخدم جديد
            user = new Utilisateur();
            user.setNom(dto.nom);
            user.setPrenom(dto.prenom);
            user.setEmail(dto.email);
            user.setRole(dto.role);
            user.setActive(false);
            user.setDeleted(false);
        }

        // 🪪 إدارة البطاقة
        if (dto.badgeUid != null && !dto.badgeUid.isBlank()) {

            Badge badge = user.getBadge();

            if (badge == null) {
                badge = new Badge();
                badge.setUtilisateur(user);
            }

            badge.setBadgeUid(dto.badgeUid);
            badge.setActive(dto.active != null ? dto.active : false);

            user.setBadge(badge);
        }

        Utilisateur savedUser = utilisateurRepository.save(user);

        // 🎟️ إنشاء Token جديد دائماً
        String token = SecureTokenGenerator.generate();

        AccountToken accountToken = new AccountToken();
        accountToken.setUtilisateur(savedUser);
        accountToken.setTokenHash(TokenHashUtil.hash(token));
        accountToken.setType(TokenType.ACTIVATION);
        accountToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        accountToken.setUsed(false);

        accountTokenRepository.save(accountToken);

        String link = "https://gestion-pointage.up.railway.app/activate-account?token=" + token;

        eventPublisher.publishEvent(
                new AccountActivationEvent(
                        savedUser.getEmail(),
                        savedUser.getPrenom(),
                        savedUser.getNom(),
                        link
                )
        );

        return savedUser;
    }
}
