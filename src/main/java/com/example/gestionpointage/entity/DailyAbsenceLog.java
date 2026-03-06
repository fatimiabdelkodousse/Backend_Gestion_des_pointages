package com.example.gestionpointage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_absence_log")
public class DailyAbsenceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private LocalDate date;

    public DailyAbsenceLog() {}

    public DailyAbsenceLog(LocalDate date) {
        this.date = date;
    }

    public Long getId()        { return id; }
    public LocalDate getDate() { return date; }

    public void setId(Long id)         { this.id = id; }
    public void setDate(LocalDate date){ this.date = date; }
}