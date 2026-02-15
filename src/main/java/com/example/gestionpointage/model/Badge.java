package com.example.gestionpointage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idBadge;

    @Column(unique = true, nullable = false)
    private String badgeUid;

    private boolean active = true;

    // 🔗 Relation avec Utilisateur (propriétaire de la relation)
    @OneToOne
    @JoinColumn(name = "id_user", nullable = false)
    @JsonIgnore
    private Utilisateur utilisateur;

    public Badge() {
    }

    // ===== Getters & Setters =====

    public Long getIdBadge() {
        return idBadge;
    }

    public void setIdBadge(Long idBadge) {
        this.idBadge = idBadge;
    }

    public String getBadgeUid() {
        return badgeUid;
    }

    public void setBadgeUid(String badgeUid) {
        this.badgeUid = badgeUid;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}
