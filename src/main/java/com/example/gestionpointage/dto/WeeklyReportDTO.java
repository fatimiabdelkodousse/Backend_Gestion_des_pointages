package com.example.gestionpointage.dto;

public class WeeklyReportDTO {

    private Long userId;
    private String nom;
    private String prenom;

    private long joursPresence;
    private long joursAbsence;
    private long totalMinutes;
    private long retards;

    public WeeklyReportDTO(
            Long userId,
            String nom,
            String prenom,
            long joursPresence,
            long joursAbsence,
            long totalMinutes,
            long retards
    ) {
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.joursPresence = joursPresence;
        this.joursAbsence = joursAbsence;
        this.totalMinutes = totalMinutes;
        this.retards = retards;
    }

    public Long getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public long getJoursPresence() { return joursPresence; }
    public long getJoursAbsence() { return joursAbsence; }
    public long getTotalMinutes() { return totalMinutes; }
    public long getRetards() { return retards; }
}
