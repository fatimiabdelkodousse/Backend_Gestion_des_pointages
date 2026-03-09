package com.example.gestionpointage.dto;

public class MonthlyReportDTO {

    private Long userId;
    private String nom;
    private String prenom;

    private long totalJoursTravail;
    private long totalMinutes;
    private String totalHeuresTravail;   
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
        this.totalHeuresTravail = formatMinutes(totalMinutes);  
        this.tauxPresence = tauxPresence;
        this.absences = absences;
    }

    private String formatMinutes(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "h " + m + "min";
    }

    public Long getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public long getTotalJoursTravail() { return totalJoursTravail; }
    public long getTotalMinutes() { return totalMinutes; }
    public String getTotalHeuresTravail() { return totalHeuresTravail; }  
    public double getTauxPresence() { return tauxPresence; }
    public long getAbsences() { return absences; }
}