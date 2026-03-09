package com.example.gestionpointage.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class WeeklyReportDTO {

    private Long userId;
    private String nom;
    private String prenom;

    private long joursPresence;
    private long joursAbsence;
    private long totalMinutes;
    private String totalHeuresTravail;
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
        this.totalHeuresTravail = formatMinutes(totalMinutes);
        this.retards = retards;
    }

    private String formatMinutes(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "h " + m + "min";
    }

    public Long getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public long getJoursPresence() { return joursPresence; }
    public long getJoursAbsence() { return joursAbsence; }

    @JsonIgnore
    public long getTotalMinutes() { return totalMinutes; }

    public String getTotalHeuresTravail() { return totalHeuresTravail; }
    public long getRetards() { return retards; }
}