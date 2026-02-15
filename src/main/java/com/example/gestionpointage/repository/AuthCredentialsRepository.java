package com.example.gestionpointage.repository;

import com.example.gestionpointage.model.AuthCredentials;

import com.example.gestionpointage.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthCredentialsRepository
        extends JpaRepository<AuthCredentials, Long> {

    Optional<AuthCredentials> findByUtilisateur(Utilisateur utilisateur);
}
