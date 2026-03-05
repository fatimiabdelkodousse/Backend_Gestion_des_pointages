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
            // ✅ فقط التحقق من صلاحية الـ token
            setPasswordService.validateToken(token);
            return "set-password";
        } catch (Exception e) {
            return "token-invalid";
        }
    }
}