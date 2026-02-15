package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.CreateUserWithBadgeDTO;

import org.springframework.http.ResponseEntity;

import com.example.gestionpointage.dto.UtilisateurBadgeDTO;
import com.example.gestionpointage.model.Badge;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.BadgeRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;
import com.example.gestionpointage.service.UtilisateurService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/users")
@CrossOrigin
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final BadgeRepository badgeRepository;
    private final UtilisateurService utilisateurService;
    
    public UtilisateurController(
            UtilisateurRepository utilisateurRepository,
            BadgeRepository badgeRepository,
            UtilisateurService utilisateurService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.badgeRepository = badgeRepository;
        this.utilisateurService = utilisateurService;
    }


    // =========================
    // 👤 + 🪪 ADD USER WITH BADGE
    // =========================
    @PostMapping("/with-badge")
    public Utilisateur addUserWithBadge(
            @RequestBody CreateUserWithBadgeDTO dto
    ) {
        return utilisateurService.createUserWithBadge(dto);
    }

    // =========================
    // ❌ DELETE USER
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {

        Utilisateur u = utilisateurRepository.findById(id)
            .orElseThrow();

        u.setDeleted(true);
        utilisateurRepository.save(u);

        return ResponseEntity.ok().build();
    }
    // =========================
    // ✏️ UPDATE USER + BADGE
    // =========================
    @PutMapping("/{id}/with-badge")
    public Utilisateur updateUserWithBadge(
            @PathVariable Long id,
            @RequestBody CreateUserWithBadgeDTO dto
    ) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 🔹 تحديث المستخدم
        u.setNom(dto.nom);
        u.setPrenom(dto.prenom);
        u.setEmail(dto.email);
        u.setRole(dto.role);

        // 🔹 معالجة البطاقة
        if (dto.badgeUid == null || dto.badgeUid.isBlank()) {
            if (u.getBadge() != null) {
                badgeRepository.delete(u.getBadge());
                u.setBadge(null);
            }
        } else {
            Badge b = u.getBadge();
            if (b == null) {
                b = new Badge();
                b.setUtilisateur(u);
            }
            b.setBadgeUid(dto.badgeUid);
            b.setActive(dto.active != null ? dto.active : true);
            badgeRepository.save(b);
            u.setBadge(b);
        }

        return utilisateurRepository.save(u);
    }
    
    @PostMapping("/{id}/upload-image")
    public String uploadProfileImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        Utilisateur user = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        String uploadDir = "uploads/";
        Files.createDirectories(Paths.get(uploadDir));

        // 🧹 حذف الصورة القديمة إن وجدت
        if (user.getImagePath() != null) {
            Path oldPath = Paths.get("." + user.getImagePath());
            Files.deleteIfExists(oldPath);
        }

        // 🆕 حفظ الصورة الجديدة
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path newPath = Paths.get(uploadDir + filename);
        Files.write(newPath, file.getBytes());

        user.setImagePath("/uploads/" + filename);
        utilisateurRepository.save(user);

        return user.getImagePath();
    }
    
    @GetMapping("/with-badges")
    public List<UtilisateurBadgeDTO> getEmployeesWithBadges() {
        return utilisateurRepository.findEmployeesWithBadges();
    }
}
