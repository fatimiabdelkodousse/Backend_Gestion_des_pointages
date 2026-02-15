package com.example.gestionpointage.dto;

import com.example.gestionpointage.model.AttendanceStatus;

public class DailyAttendanceDTO {

    private Long userId;
    private String nom;
    private String prenom;
    private AttendanceStatus status;
    private long lateMinutes; // فقط إذا كان متأخر

    public DailyAttendanceDTO(
            Long userId,
            String nom,
            String prenom,
            AttendanceStatus status,
            long lateMinutes
    ) {
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.status = status;
        this.lateMinutes = lateMinutes;
    }

    public Long getUserId() { return userId; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public AttendanceStatus getStatus() { return status; }
    public long getLateMinutes() { return lateMinutes; }
}
