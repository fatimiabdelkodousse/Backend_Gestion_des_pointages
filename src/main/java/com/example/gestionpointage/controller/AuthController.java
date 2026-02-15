package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.LoginRequestDTO;

import com.example.gestionpointage.dto.LoginResponseDTO;
import com.example.gestionpointage.dto.ForgotPasswordRequestDTO;
import com.example.gestionpointage.dto.MeResponseDTO;
import com.example.gestionpointage.dto.RefreshTokenRequestDTO;
import com.example.gestionpointage.dto.RefreshTokenResponseDTO;
import com.example.gestionpointage.model.AuthCredentials;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.model.Badge;
import com.example.gestionpointage.repository.AuthCredentialsRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;
import com.example.gestionpointage.security.PasswordHashUtil;
import com.example.gestionpointage.security.LoginProtectionService;
import com.example.gestionpointage.service.ForgotPasswordService;
import com.example.gestionpointage.security.JwtService;
import com.example.gestionpointage.security.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final AuthCredentialsRepository authRepo;
    private final LoginProtectionService loginProtectionService;
    private final ForgotPasswordService forgotPasswordService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UtilisateurRepository utilisateurRepository,
            AuthCredentialsRepository authRepo,
            LoginProtectionService loginProtectionService,
            ForgotPasswordService forgotPasswordService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.authRepo = authRepo;
        this.loginProtectionService = loginProtectionService;
        this.forgotPasswordService = forgotPasswordService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(
            @RequestBody LoginRequestDTO dto,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String key = "LOGIN:" + ip + ":" + dto.email;

        // 🔐 rate limiting + delay
        loginProtectionService.check(key);

        Utilisateur user = utilisateurRepository
                .findByEmail(dto.email)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"
                    )
                );
        if (!user.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Account not activated"
            );
        }

        AuthCredentials credentials = authRepo
                .findByUtilisateur(user)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"
                    )
                );

        boolean valid = PasswordHashUtil.verify(
                dto.password,
                credentials.getPasswordHash()
        );

        if (!valid) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
            );
        }

        // ✅ success → reset attempts
        loginProtectionService.success(key);

        Badge badge = user.getBadge();
        
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId().toString()
        );

        return new LoginResponseDTO(
                user.getId(),
                user.getRole(),
                user.getPrenom(),
                user.getNom(),
                user.getEmail(),
                badge != null ? badge.getBadgeUid() : null,
                badge != null ? badge.isActive() : null,
                user.getImagePath(),
                accessToken,
                refreshToken
        );
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO dto,
            HttpServletRequest request
    ) {

        forgotPasswordService.process(
                dto.getEmail(),
                dto.getNom(),
                dto.getPrenom(),
                dto.getBadgeUid(),
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(
                "Reset link sent successfully."
        );
    }
    @GetMapping("/me")
    public MeResponseDTO me(
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // userId محفوظ في JWT كـ subject
        Long userId = Long.parseLong(authentication.getName());

        Utilisateur user = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return new MeResponseDTO(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getPrenom(),
            user.getNom()
        );
    }
    @PostMapping("/refresh")
    public RefreshTokenResponseDTO refresh(
            @RequestBody RefreshTokenRequestDTO dto,
            HttpServletRequest request
    ) {
        String rawRefreshToken = dto.refreshToken;

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        return refreshTokenService.rotate(
                rawRefreshToken,
                ip,
                userAgent
        );
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody RefreshTokenRequestDTO dto
    ) {
        refreshTokenService.logout(dto.refreshToken);
        return ResponseEntity.noContent().build();
    }
}