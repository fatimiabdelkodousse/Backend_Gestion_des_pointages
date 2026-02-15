package com.example.gestionpointage.dto;

public class ForgotPasswordRequestDTO {

    private String nom;
    private String prenom;
    private String email;
    private String badgeUid;

    public ForgotPasswordRequestDTO() {}

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

    public String getBadgeUid() {
        return badgeUid;
    }

    public void setBadgeUid(String badgeUid) {
        this.badgeUid = badgeUid;
    }
}
