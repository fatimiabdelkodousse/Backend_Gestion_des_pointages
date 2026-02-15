package com.example.gestionpointage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
    name = "daily_absence_log",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"date", "site_id"})
    }
)
public class DailyAbsenceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    public DailyAbsenceLog() {}

    public DailyAbsenceLog(LocalDate date, Site site) {
        this.date = date;
        this.site = site;
    }

    // =====================
    // GETTERS
    // =====================

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Site getSite() {
        return site;
    }

    // =====================
    // SETTERS
    // =====================

    public void setId(Long id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setSite(Site site) {
        this.site = site;
    }
}
