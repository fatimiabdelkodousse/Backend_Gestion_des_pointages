package com.example.gestionpointage.event;

public record AccountActivationEvent(
        String email,
        String prenom,
        String nom,
        String activationLink
) {}
