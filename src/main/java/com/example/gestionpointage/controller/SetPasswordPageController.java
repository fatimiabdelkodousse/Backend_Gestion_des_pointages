package com.example.gestionpointage.controller;

import org.springframework.ui.Model;

import com.example.gestionpointage.model.TokenType;
import org.springframework.web.server.ResponseStatusException;
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
            @RequestParam String token,
            Model model
    ) {
        System.out.println("✅ RESET-PASSWORD PAGE HIT! token=" + token);

        try {
            TokenType type = setPasswordService.validateAndGetType(token);

            model.addAttribute("token", token);
            model.addAttribute("mode", type.name());

            return "set-password";

        } catch (ResponseStatusException e) {
            System.out.println("❌ Token invalid: " + e.getMessage());
            return "token-invalid";
        }
    }
}

