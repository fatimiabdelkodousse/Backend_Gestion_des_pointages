package com.example.gestionpointage.dto;

import com.example.gestionpointage.model.Role;

public class LoginResponseDTO {

    private Long id;
    private Role role;
    private String prenom;
    private String nom;
    private String email;
    private String badgeUid;
    private Boolean badgeActive;
    private String imagePath;

    // 🔐 JWT
    private String accessToken;
    private String refreshToken;

    public LoginResponseDTO(
            Long id,
            Role role,
            String prenom,
            String nom,
            String email,
            String badgeUid,
            Boolean badgeActive,
            String imagePath,
            String accessToken,
            String refreshToken
    ) {
        this.id = id;
        this.role = role;
        this.prenom = prenom;
        this.nom = nom;
        this.email = email;
        this.badgeUid = badgeUid;
        this.badgeActive = badgeActive;
        this.imagePath = imagePath;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public String getPrenom() { return prenom; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getBadgeUid() { return badgeUid; }
    public Boolean getBadgeActive() { return badgeActive; }
    public String getImagePath() { return imagePath; }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}
