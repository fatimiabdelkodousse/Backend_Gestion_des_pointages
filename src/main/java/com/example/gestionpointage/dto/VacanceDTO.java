package com.example.gestionpointage.dto;

import java.time.LocalDate;

public class VacanceDTO {

    private Long id;
    private LocalDate date;
    private String nom;

    public VacanceDTO() {}

    public VacanceDTO(Long id, LocalDate date, String nom) {
        this.id = id;
        this.date = date;
        this.nom = nom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}