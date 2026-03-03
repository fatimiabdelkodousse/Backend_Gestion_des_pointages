package com.example.gestionpointage.dto;

public class SiteDTO {

    private Long id;
    private String name;
    private String address;
    private boolean active;

    public SiteDTO() {}

    public SiteDTO(Long id, String name, String address, boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}