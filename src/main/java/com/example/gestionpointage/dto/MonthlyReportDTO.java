package com.example.gestionpointage.dto;

public class MonthlyReportDTO {

    private Long userId;
    private String nom;
    private String prenom;

    private long totalJoursTravail;
    private long totalMinutes;
    private double tauxPresence;
    private long absences;

    public MonthlyReportDTO(
            Long userId,
            String nom,
            String prenom,
            long totalJoursTravail,
            long totalMinutes,
            double tauxPresence,
            long absences
    ) {
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.totalJoursTravail = totalJoursTravail;
        this.totalMinutes = totalMinutes;
        this.tauxPresence = tauxPresence;
        this.absences = absences;
    }

    public Long getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public long getTotalJoursTravail() { return totalJoursTravail; }
    public long getTotalMinutes() { return totalMinutes; }
    public double getTauxPresence() { return tauxPresence; }
    public long getAbsences() { return absences; }
}
