package com.example.gestionpointage.dto;

import com.example.gestionpointage.model.Role;

public class CreateUserWithBadgeDTO {

    public String nom;
    public String prenom;
    public String email;
    public Role role;
    public String badgeUid;
    public Boolean active;
    public Long siteId;
}
