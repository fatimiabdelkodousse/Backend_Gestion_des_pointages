package com.example.gestionpointage.service;

import com.example.gestionpointage.model.AuthCredentials;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.AuthCredentialsRepository;
import com.example.gestionpointage.security.PasswordHashUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthCredentialsService {

    private final AuthCredentialsRepository authRepo;

    public AuthCredentialsService(AuthCredentialsRepository authRepo) {
        this.authRepo = authRepo;
    }

    /**
     * Create or update password for user
     */
    public void setPassword(Utilisateur user, String rawPassword) {

        AuthCredentials credentials = authRepo
                .findByUtilisateur(user)
                .orElse(new AuthCredentials());

        credentials.setUtilisateur(user);
        credentials.setPasswordHash(
                PasswordHashUtil.hash(rawPassword)
        );

        authRepo.save(credentials);
    }

    /**
     * Verify login password
     */
    public boolean verifyPassword(Utilisateur user, String rawPassword) {
        return authRepo.findByUtilisateur(user)
                .map(c -> PasswordHashUtil.verify(rawPassword, c.getPasswordHash()))
                .orElse(false);
    }
}
