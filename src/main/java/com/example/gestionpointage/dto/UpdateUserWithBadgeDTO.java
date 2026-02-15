package com.example.gestionpointage.dto;

import com.example.gestionpointage.model.Role;

public class UpdateUserWithBadgeDTO {

    public String nom;
    public String prenom;
    public String email;
    public Role role;

    // badge (اختياري)
    public String badgeUid;
    public Boolean active;
}
