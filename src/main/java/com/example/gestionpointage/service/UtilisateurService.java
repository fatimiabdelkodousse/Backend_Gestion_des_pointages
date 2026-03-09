package com.example.gestionpointage.service;

import com.example.gestionpointage.dto.CreateUserWithBadgeDTO;
import com.example.gestionpointage.model.Badge;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestionpointage.repository.SiteRepository;
import com.example.gestionpointage.entity.Site;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@Transactional
public class UtilisateurService {

    private static final LocalTime ELIGIBILITY_CUTOFF = LocalTime.of(9, 0);

    private final UtilisateurRepository utilisateurRepository;
    private final SiteRepository siteRepository;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            SiteRepository siteRepository
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.siteRepository = siteRepository;
    }

    private LocalDate computeEligibleFrom() {
        return LocalTime.now().isBefore(ELIGIBILITY_CUTOFF)
                ? LocalDate.now()
                : LocalDate.now().plusDays(1);
    }

    public Utilisateur createUserWithBadge(CreateUserWithBadgeDTO dto) {

        Utilisateur existingUser =
                utilisateurRepository.findByEmail(dto.email).orElse(null);

        Utilisateur user;

        if (existingUser != null) {

            if (!existingUser.isDeleted()) {
                throw new RuntimeException("Email already exists");
            }

            // ═══ استعادة موظف محذوف ═══
            user = existingUser;
            user.setDeleted(false);
            user.setActive(true);
            user.setNom(dto.nom);
            user.setPrenom(dto.prenom);
            user.setRole(dto.role);

        } else {

            // ═══ إنشاء موظف جديد ═══
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

            if (badge.isActive()) {
                user.setEligibleFrom(computeEligibleFrom());
            }
        }

        Utilisateur savedUser = utilisateurRepository.save(user);

        return savedUser;
    }
}