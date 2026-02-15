package com.example.gestionpointage.model;

import com.example.gestionpointage.entity.Site;

import jakarta.persistence.*;

@Entity
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String email;
    
    @Column(name = "image_path")
    private String imagePath;
    
    @Enumerated(EnumType.STRING)
    private Role role;

    // 🔗 Relation inverse avec Badge (CHOIX 2)
    @OneToOne(
        mappedBy = "utilisateur",
        cascade = CascadeType.ALL,
        orphanRemoval = true   // ✅ هذا هو السطر الناقص
    )
    private Badge badge;
    
    @Column(nullable = false)
    private boolean active = false;
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Utilisateur() {
    }

    // ===== Getters & Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }
    
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
