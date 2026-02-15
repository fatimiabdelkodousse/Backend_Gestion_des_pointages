package com.example.gestionpointage.dto;

public record MeResponseDTO(
    Long id,
    String email,
    String role,
    String prenom,
    String nom
) {}
