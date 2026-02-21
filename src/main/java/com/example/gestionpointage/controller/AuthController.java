package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.LoginRequestDTO;

import com.example.gestionpointage.entity.UserDevice;
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
import com.example.gestionpointage.repository.UserDeviceRepository;
import com.example.gestionpointage.service.EmailService;
import com.example.gestionpointage.security.DeviceFingerprintUtil;
import java.util.Optional;
import java.time.LocalDateTime;

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
    private final UserDeviceRepository userDeviceRepository;
    private final EmailService emailService;

    public AuthController(
            UtilisateurRepository utilisateurRepository,
            AuthCredentialsRepository authRepo,
            LoginProtectionService loginProtectionService,
            ForgotPasswordService forgotPasswordService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDeviceRepository userDeviceRepository,
            EmailService emailService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.authRepo = authRepo;
        this.loginProtectionService = loginProtectionService;
        this.forgotPasswordService = forgotPasswordService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDeviceRepository = userDeviceRepository;
        this.emailService = emailService;
        
    }

    @PostMapping("/login")
    public LoginResponseDTO login(
            @RequestBody LoginRequestDTO dto,
            HttpServletRequest request
    ) {

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String key = "LOGIN:" + ip + ":" + dto.email;

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

        loginProtectionService.success(key);

        // ==========================================
        // 🔐 Suspicious Login Detection هنا بالضبط
        // ==========================================

        String deviceHash =
                DeviceFingerprintUtil.generate(ip, userAgent);

        Optional<UserDevice> deviceOpt =
                userDeviceRepository.findByUserAndDeviceHash(
                        user,
                        deviceHash
                );

        if (deviceOpt.isEmpty()) {

            // 🚨 جهاز جديد
            UserDevice newDevice = new UserDevice();
            newDevice.setUser(user);
            newDevice.setDeviceHash(deviceHash);
            newDevice.setIpAddress(ip);
            newDevice.setUserAgent(userAgent);
            newDevice.setFirstSeen(LocalDateTime.now());
            newDevice.setLastSeen(LocalDateTime.now());
            newDevice.setTrusted(false);

            userDeviceRepository.save(newDevice);

            // 📧 إرسال تنبيه
            emailService.sendSuspiciousLoginAlert(
                    user.getEmail(),
                    ip,
                    userAgent
            );

        } else {

            UserDevice device = deviceOpt.get();
            device.setLastSeen(LocalDateTime.now());
            userDeviceRepository.save(device);
        }

        // ==========================================
        // 🔐 إنشاء التوكنات
        // ==========================================

        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole().name()
        );

        String refreshToken = refreshTokenService.createRefreshToken(
                user,
                ip,
                userAgent
        );

        Badge badge = user.getBadge();

        return new LoginResponseDTO(
                user.getId(),
                user.getRole(),
                user.getPrenom(),
                user.getNom(),
                user.getEmail(),
                badge != null ? badge.getBadgeUid() : null,
                badge != null ? badge.isActive() : null,
                user.getImagePath(),
                user.getSite() != null ? user.getSite().getId() : null,  // ← جديد
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