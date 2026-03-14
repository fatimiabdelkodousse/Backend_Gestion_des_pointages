package com.example.gestionpointage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "vacance")
public class Vacance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false)
    private String nom;

    public Vacance() {}

    public Vacance(LocalDate date, String nom) {
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