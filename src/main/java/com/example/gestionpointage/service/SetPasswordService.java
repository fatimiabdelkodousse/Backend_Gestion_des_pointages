package com.example.gestionpointage.service;

import com.example.gestionpointage.entity.AccountToken;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.AccountTokenRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;
import com.example.gestionpointage.security.TokenHashUtil;
import com.example.gestionpointage.security.PasswordPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class SetPasswordService {

    private final AccountTokenRepository accountTokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthCredentialsService authCredentialsService;

    public SetPasswordService(
            AccountTokenRepository accountTokenRepository,
            UtilisateurRepository utilisateurRepository,
            AuthCredentialsService authCredentialsService
    ) {
        this.accountTokenRepository = accountTokenRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authCredentialsService = authCredentialsService;
    }

    public void validateToken(String token) {

        AccountToken accountToken = getValidToken(token);

        if (accountToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token expired"
            );
        }
    }

    public void activateAccount(String token, String password) {

        PasswordPolicy.validate(password);

        AccountToken accountToken = getValidToken(token);

        if (accountToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token expired"
            );
        }

        Utilisateur user = accountToken.getUtilisateur();

        authCredentialsService.setPassword(user, password);

        accountTokenRepository.delete(accountToken);
    }

    private AccountToken getValidToken(String token) {

        String tokenHash = TokenHashUtil.hash(token);

        return accountTokenRepository
                .findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid token"
                        )
                );
    }
}