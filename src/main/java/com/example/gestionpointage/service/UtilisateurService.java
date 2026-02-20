package com.example.gestionpointage.service;

import com.example.gestionpointage.dto.CreateUserWithBadgeDTO;
import com.example.gestionpointage.model.Badge;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestionpointage.repository.SiteRepository;
import com.example.gestionpointage.entity.Site;

@Service
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final SiteRepository siteRepository;
    
    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            SiteRepository siteRepository
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.siteRepository = siteRepository;
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
            user.setActive(true);
            user.setNom(dto.nom);
            user.setPrenom(dto.prenom);
            user.setRole(dto.role);

        } else {

            user = new Utilisateur();
            user.setNom(dto.nom);
            user.setPrenom(dto.prenom);
            user.setEmail(dto.email);
            user.setRole(dto.role);
            user.setActive(true);
            user.setDeleted(false);
        }
        
        Site site = siteRepository.findById(dto.siteId)
                .orElseThrow(() -> new RuntimeException("Site introuvable"));

        user.setSite(site);

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

        return savedUser;
    }
}
