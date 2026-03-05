package com.example.gestionpointage.controller;

import com.example.gestionpointage.service.SetPasswordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;

@Controller
public class SetPasswordPageController {

    private final SetPasswordService setPasswordService;

    public SetPasswordPageController(SetPasswordService setPasswordService) {
        this.setPasswordService = setPasswordService;
    }

    @GetMapping("/reset-password")
    public String showSetPasswordPage(
            @RequestParam String token
    ) {
        try {
            System.out.println("🔑 Reset page requested with token length: "
                    + token.length());

            setPasswordService.validateToken(token);

            System.out.println("✅ Token validated successfully");
            return "set-password";

        } catch (Exception e) {
            System.out.println("❌ Token validation failed: " + e.getMessage());
            return "token-invalid";
        }
    }
}