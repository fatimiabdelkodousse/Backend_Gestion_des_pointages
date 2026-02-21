package com.example.gestionpointage.dto;

import com.example.gestionpointage.model.Role;

public class UtilisateurBadgeDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private String badgeUid;
    private Boolean active;
    private Long siteId;       // ← جديد

    // ✅ Constructor المستخدم من JPQL
    public UtilisateurBadgeDTO(
            Long id,
            String nom,
            String prenom,
            String email,
            Role role,
            String badgeUid,
            Boolean active,
            Long siteId            // ← جديد
    ) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.badgeUid = badgeUid;
        this.active = active;
        this.siteId = siteId;     // ← جديد
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getBadgeUid() { return badgeUid; }
    public Boolean getActive() { return active; }
    public Long getSiteId() { return siteId; }   // ← جديد
}