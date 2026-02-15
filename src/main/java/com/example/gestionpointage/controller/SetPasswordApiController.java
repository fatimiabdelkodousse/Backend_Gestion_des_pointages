package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.SetPasswordDTO;
import com.example.gestionpointage.service.SetPasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class SetPasswordApiController {

    private final SetPasswordService service;

    public SetPasswordApiController(SetPasswordService service) {
        this.service = service;
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(
            @RequestBody SetPasswordDTO dto
    ) {
        service.activateAccount(dto.token(), dto.password());
        return ResponseEntity.ok().build();
    }
}
