package com.example.gestionpointage.controller;

import com.example.gestionpointage.entity.Pointage;
import com.example.gestionpointage.model.PointageType;
import com.example.gestionpointage.service.PointageService;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

import com.example.gestionpointage.dto.DailyReportDTO;
import com.example.gestionpointage.dto.DailyAttendanceDTO;
import com.example.gestionpointage.dto.AttendanceStatsDTO;

import java.util.List;

@RestController
@RequestMapping("/pointages")
public class PointageController {

    private final PointageService pointageService;

    public PointageController(PointageService pointageService) {
        this.pointageService = pointageService;
    }

    @PostMapping
    public Pointage create(
            @RequestParam Long userId,
            @RequestParam Long siteId,
            @RequestParam PointageType type
    ) {
        return pointageService.createPointage(userId, siteId, type);
    }

    @GetMapping("/user/{userId}")
    public List<Pointage> byUser(@PathVariable Long userId) {
        return pointageService.getByUser(userId);
    }

    @GetMapping("/site/{siteId}")
    public List<Pointage> bySite(@PathVariable Long siteId) {
        return pointageService.getBySite(siteId);
    }
    
    @GetMapping("/site/{siteId}/today")
    public List<Pointage> getTodayBySite(@PathVariable Long siteId) {
        return pointageService.getTodayBySite(siteId);
    }
    
    @GetMapping("/user/{userId}/daily")
    public DailyReportDTO getDailyReport(
            @PathVariable Long userId,
            @RequestParam String date
    ) {
        return pointageService.getDailyReport(
                userId,
                LocalDate.parse(date)
        );
    }
    @GetMapping("/attendance/{userId}")
    public DailyAttendanceDTO getAttendance(
            @PathVariable Long userId,
            @RequestParam String date
    ) {
        return pointageService.getDailyAttendance(
                userId,
                LocalDate.parse(date)
        );
    }
    
    @GetMapping("/stats")
    public AttendanceStatsDTO getStats(
            @RequestParam Long siteId,
            @RequestParam String date
    ) {
        return pointageService.getDailyStatsBySite(
                siteId,
                LocalDate.parse(date)
        );
    }
    
    @GetMapping("/attendance-list")
    public List<DailyAttendanceDTO> getAttendanceBySite(
            @RequestParam Long siteId,
            @RequestParam String date
    ) {
        return pointageService.getDailyAttendanceBySite(
                siteId,
                LocalDate.parse(date)
        );
    }

}
