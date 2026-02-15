package com.example.gestionpointage.controller;

// ✅ إضافة هذا الـ Import
import com.example.gestionpointage.model.Utilisateur; 
import com.example.gestionpointage.model.Role;
import com.example.gestionpointage.dto.BulkLogoutRequestDTO;
import com.example.gestionpointage.security.RefreshTokenService;
import com.example.gestionpointage.repository.UtilisateurRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/sessions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    private final RefreshTokenService refreshTokenService;
    private final UtilisateurRepository utilisateurRepository;

    public AdminSessionController(RefreshTokenService refreshTokenService, UtilisateurRepository utilisateurRepository) {
        this.refreshTokenService = refreshTokenService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @PostMapping("/logout/user/{userId}")
    public ResponseEntity<?> logoutUser(@PathVariable Long userId) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot logout another admin");
        }

        // ✅ الآن الدالة موجودة في الـ Service ومتاحة للطلب
        refreshTokenService.logoutAll(user);

        return ResponseEntity.ok("User logged out from all devices");
    }

    @PostMapping("/logout/users")
    public ResponseEntity<?> logoutUsers(@RequestBody BulkLogoutRequestDTO dto) {
        if (dto.userIds == null || dto.userIds.isEmpty()) {
            return ResponseEntity.badRequest().body("User IDs list is empty");
        }
        refreshTokenService.logoutAllEmployees(dto.userIds);
        return ResponseEntity.ok("Selected users logged out");
    }

    @PostMapping("/logout/all-employees")
    public ResponseEntity<?> logoutAllEmployees() {
        refreshTokenService.logoutAllEmployees();
        return ResponseEntity.ok("All employees logged out successfully");
    }
}