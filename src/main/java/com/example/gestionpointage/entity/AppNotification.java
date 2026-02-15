package com.example.gestionpointage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String message;

    @Column(length = 2000)
    private String details;   // 👈 جديد

    private boolean readStatus = false;

    private LocalDateTime createdAt;

    @ManyToOne
    private Site site;

    public AppNotification() {}

    public AppNotification(
            String title,
            String message,
            String details,
            Site site
    ) {
        this.title = title;
        this.message = message;
        this.details = details;
        this.site = site;
        this.createdAt = LocalDateTime.now();
        this.readStatus = false;
    }

    // getters & setters

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public boolean isReadStatus() { return readStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Site getSite() { return site; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setDetails(String details) { this.details = details; }
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setSite(Site site) { this.site = site; }
}


