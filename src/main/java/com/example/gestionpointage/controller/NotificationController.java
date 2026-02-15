package com.example.gestionpointage.controller;

import com.example.gestionpointage.entity.AppNotification;

import com.example.gestionpointage.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/site/{siteId}")
    public List<AppNotification> getBySite(
            @PathVariable Long siteId
    ) {
        return service.getBySite(siteId);
    }
    
    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        service.deleteNotification(id);
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
    }
}
