package com.example.gestionpointage.dto;

import java.time.LocalTime;

public class DailyReportRowDTO {

    private Long userId;
    private String nom;
    private String prenom;

    private LocalTime heureEntree;
    private LocalTime heureSortie;

    private long totalMinutes;
    private String statut;

    public DailyReportRowDTO(
            Long userId,
            String nom,
            String prenom,
            LocalTime heureEntree,
            LocalTime heureSortie,
            long totalMinutes,
            String statut
    ) {
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.heureEntree = heureEntree;
        this.heureSortie = heureSortie;
        this.totalMinutes = totalMinutes;
        this.statut = statut;
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public LocalTime getHeureEntree() { return heureEntree; }
    public LocalTime getHeureSortie() { return heureSortie; }
    public long getTotalMinutes() { return totalMinutes; }
    public String getStatut() { return statut; }
}
