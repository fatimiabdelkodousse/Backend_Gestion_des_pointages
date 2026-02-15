package com.example.gestionpointage.entity;

import com.example.gestionpointage.model.PointageType;
import com.example.gestionpointage.model.Utilisateur;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pointage")
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private Utilisateur user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointageType type;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Getters & Setters

    public Long getId() { return id; }

    public Utilisateur getUser() { return user; }
    public void setUser(Utilisateur user) { this.user = user; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public PointageType getType() { return type; }
    public void setType(PointageType type) { this.type = type; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
