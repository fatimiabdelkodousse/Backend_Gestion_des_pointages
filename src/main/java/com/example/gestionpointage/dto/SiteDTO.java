package com.example.gestionpointage.dto;

public class SiteDTO {

    private Long id;
    private String name;

    public SiteDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
