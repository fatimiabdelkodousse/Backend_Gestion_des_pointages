package com.example.gestionpointage.service;

import com.example.gestionpointage.entity.Pointage;


import com.example.gestionpointage.entity.Site;
import com.example.gestionpointage.model.PointageType;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.PointageRepository;
import com.example.gestionpointage.repository.SiteRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;
import com.example.gestionpointage.dto.DailyReportRowDTO;
import com.example.gestionpointage.dto.WeeklyReportDTO;
import com.example.gestionpointage.dto.MonthlyReportDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.time.DayOfWeek;

import com.example.gestionpointage.dto.DailyReportDTO;
import com.example.gestionpointage.model.AttendanceStatus;
import com.example.gestionpointage.dto.DailyAttendanceDTO;
import com.example.gestionpointage.dto.AttendanceStatsDTO;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@Transactional
public class PointageService {

    private final PointageRepository pointageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SiteRepository siteRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PointageService(
            PointageRepository pointageRepository,
            UtilisateurRepository utilisateurRepository,
            SiteRepository siteRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.pointageRepository = pointageRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.siteRepository = siteRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // =====================================================
    // CREATE POINTAGE
    // =====================================================

    public Pointage createPointage(
            Long userId,
            Long siteId,
            PointageType type
    ) {

        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Utilisateur introuvable"
                        )
                );

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Site introuvable"
                        )
                );

        Optional<Pointage> lastOpt =
                pointageRepository.findTopByUserOrderByTimestampDesc(user);

        if (lastOpt.isEmpty() && type == PointageType.SORTIE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Impossible de sortir sans entrée"
            );
        }

        if (lastOpt.isPresent()) {

            Pointage last = lastOpt.get();

            if (last.getType() == PointageType.ENTREE
                    && type == PointageType.ENTREE) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Entrée déjà enregistrée"
                );
            }

            if (last.getType() == PointageType.SORTIE
                    && type == PointageType.SORTIE) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sortie déjà enregistrée"
                );
            }
        }

        Pointage pointage = new Pointage();
        pointage.setUser(user);
        pointage.setSite(site);
        pointage.setType(type);
        pointage.setTimestamp(LocalDateTime.now());

        Pointage saved = pointageRepository.save(pointage);

        System.out.println("SENDING STATS TO /topic/stats/" + siteId);
        System.out.println("SENDING POINTAGE TO /topic/pointages/" + siteId);
        // 🔥 تحديث الإحصائيات مباشرة عبر WebSocket
        messagingTemplate.convertAndSend(
                "/topic/stats/" + siteId,
                getDailyStatsBySite(siteId, LocalDate.now())
        );
        
        messagingTemplate.convertAndSend(
        	    "/topic/pointages/" + siteId,
        	    saved
        	);

        return saved;
    }

    // =====================================================
    // GET BY USER / SITE
    // =====================================================

    public List<Pointage> getByUser(Long userId) {
        return pointageRepository.findByUserId(userId);
    }

    public List<Pointage> getBySite(Long siteId) {
        return pointageRepository.findBySiteId(siteId);
    }

    // =====================================================
    // DAILY REPORT (TOTAL WORK TIME)
    // =====================================================

    public DailyReportDTO getDailyReport(Long userId, LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Pointage> pointages =
                pointageRepository
                        .findByUserIdAndTimestampBetweenOrderByTimestampAsc(
                                userId,
                                start,
                                end
                        );

        long totalMinutes = 0;

        for (int i = 0; i < pointages.size() - 1; i++) {

            Pointage current = pointages.get(i);
            Pointage next = pointages.get(i + 1);

            if (current.getType() == PointageType.ENTREE &&
                    next.getType() == PointageType.SORTIE) {

                Duration duration = Duration.between(
                        current.getTimestamp(),
                        next.getTimestamp()
                );

                totalMinutes += duration.toMinutes();
            }
        }

        return new DailyReportDTO(date, totalMinutes);
    }

    // =====================================================
    // DAILY ATTENDANCE (FOR ONE USER)
    // =====================================================

    public DailyAttendanceDTO getDailyAttendance(
            Long userId,
            LocalDate date
    ) {

        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow();

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        var firstEntryOpt = pointageRepository
                .findTopByUserAndTypeAndTimestampBetweenOrderByTimestampAsc(
                        user,
                        PointageType.ENTREE,
                        start,
                        end
                );

        LocalTime workStart = LocalTime.of(9, 0);
        LocalTime toleranceLimit = LocalTime.of(9, 5);

        if (firstEntryOpt.isEmpty()) {
            return new DailyAttendanceDTO(
                    user.getId(),
                    user.getNom(),
                    user.getPrenom(),
                    AttendanceStatus.ABSENT,
                    0
            );
        }

        LocalTime arrivalTime =
                firstEntryOpt.get()
                        .getTimestamp()
                        .toLocalTime();

        if (arrivalTime.isBefore(workStart)) {
            return new DailyAttendanceDTO(
                    user.getId(),
                    user.getNom(),
                    user.getPrenom(),
                    AttendanceStatus.EARLY,
                    0
            );
        }

        if (!arrivalTime.isAfter(toleranceLimit)) {
            return new DailyAttendanceDTO(
                    user.getId(),
                    user.getNom(),
                    user.getPrenom(),
                    AttendanceStatus.ON_TIME,
                    0
            );
        }

        long lateMinutes =
                Duration.between(workStart, arrivalTime).toMinutes();

        return new DailyAttendanceDTO(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                AttendanceStatus.LATE,
                lateMinutes
        );
    }

    // =====================================================
    // DAILY STATS BY SITE (DASHBOARD)
    // =====================================================

    public AttendanceStatsDTO getDailyStatsBySite(
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(23, 59, 59);

        LocalTime workStart      = LocalTime.of(9, 0);
        LocalTime toleranceLimit = LocalTime.of(9, 5);
        LocalTime absenceLimit   = LocalTime.of(18, 0);

        List<Utilisateur> users = utilisateurRepository.findBySiteId(siteId);

        long total = users.size();
        long early = 0;
        long onTime = 0;
        long late = 0;
        long absent = 0;

        LocalDateTime now = LocalDateTime.now();

        boolean afterWorkDay =
                now.isAfter(date.atTime(absenceLimit));
        
        for (Utilisateur user : users) {

            var firstEntryOpt =
                    pointageRepository
                            .findTopByUserAndSiteIdAndTypeAndTimestampBetweenOrderByTimestampAsc(
                                    user,
                                    siteId,
                                    PointageType.ENTREE,
                                    start,
                                    end
                            );

            // ❌ لم يسجل دخول
            if (firstEntryOpt.isEmpty()) {

                // نحسبه غائب فقط بعد 18:00
                if (afterWorkDay) {
                    absent++;
                }

                continue;
            }

            LocalTime arrival =
                    firstEntryOpt.get()
                            .getTimestamp()
                            .toLocalTime();

            if (arrival.isBefore(workStart)) {
                early++;
            }
            else if (!arrival.isAfter(toleranceLimit)) {
                onTime++;
            }
            else {
                late++;
            }
        }

        long present = early + onTime + late;

        // قبل 18:00 لا نحسب الغياب النهائي
        if (!afterWorkDay) {
            absent = 0;
        }

        return new AttendanceStatsDTO(
                total,
                present,
                early,
                onTime,
                late,
                absent
        );
    }
    
    public List<String> getAbsentUsersNames(
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(23,59,59);

        List<Utilisateur> users =
                utilisateurRepository.findBySiteId(siteId);

        List<String> absentNames = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        boolean afterWorkDay =
                now.isAfter(date.atTime(18,0));

        for (Utilisateur user : users) {

            var firstEntryOpt =
                    pointageRepository
                            .findTopByUserAndSiteIdAndTypeAndTimestampBetweenOrderByTimestampAsc(
                                    user,
                                    siteId,
                                    PointageType.ENTREE,
                                    start,
                                    end
                            );

            if (firstEntryOpt.isEmpty() && afterWorkDay) {
                absentNames.add(
                        user.getNom() + " " + user.getPrenom()
                );
            }
        }

        return absentNames;
    }
    
    public List<Pointage> getTodayBySite(Long siteId) {

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23,59,59);

        return pointageRepository
            .findBySiteIdAndTimestampBetweenOrderByTimestampDesc(
                siteId,
                start,
                end
            );
    }
    
    public List<DailyReportRowDTO> generateDailyReport(
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23,59,59);

        List<Utilisateur> users =
                utilisateurRepository.findBySiteId(siteId);

        List<DailyReportRowDTO> rows = new ArrayList<>();

        for (Utilisateur user : users) {

            List<Pointage> pointages =
                    pointageRepository
                            .findByUserIdAndTimestampBetweenOrderByTimestampAsc(
                                    user.getId(),
                                    start,
                                    end
                            );

            if (pointages.isEmpty()) {
                rows.add(
                        new DailyReportRowDTO(
                                user.getId(),
                                user.getNom(),
                                user.getPrenom(),
                                null,
                                null,
                                0,
                                "Absent"
                        )
                );
                continue;
            }

            LocalTime entree =
                    pointages.get(0).getTimestamp().toLocalTime();

            LocalTime sortie =
                    pointages.get(pointages.size()-1)
                            .getTimestamp()
                            .toLocalTime();

            long totalMinutes = 0;

            for (int i=0; i<pointages.size()-1; i++) {

                if (pointages.get(i).getType()
                        == PointageType.ENTREE
                        &&
                    pointages.get(i+1).getType()
                        == PointageType.SORTIE) {

                    totalMinutes +=
                            Duration.between(
                                    pointages.get(i).getTimestamp(),
                                    pointages.get(i+1).getTimestamp()
                            ).toMinutes();
                }
            }

            String statut = "Présent";

            if (entree.isAfter(LocalTime.of(9,5))) {
                statut = "Retard";
            }

            rows.add(
                    new DailyReportRowDTO(
                            user.getId(),
                            user.getNom(),
                            user.getPrenom(),
                            entree,
                            sortie,
                            totalMinutes,
                            statut
                    )
            );
        }

        return rows;
    }
    
    public List<WeeklyReportDTO> generateWeeklyReport(
            Long siteId,
            LocalDate date
    ) {

        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<Utilisateur> users =
                utilisateurRepository.findBySiteId(siteId);

        List<WeeklyReportDTO> result = new ArrayList<>();

        for (Utilisateur user : users) {

            long presence = 0;
            long absence = 0;
            long totalMinutes = 0;
            long retards = 0;

            for (LocalDate d = startOfWeek;
                 !d.isAfter(endOfWeek);
                 d = d.plusDays(1)) {

                var daily =
                        generateDailyReport(siteId, d)
                                .stream()
                                .filter(r ->
                                        r.getUserId()
                                                .equals(user.getId()))
                                .findFirst()
                                .orElse(null);

                if (daily == null ||
                        daily.getStatut().equals("Absent")) {

                    absence++;
                } else {

                    presence++;
                    totalMinutes += daily.getTotalMinutes();

                    if (daily.getStatut()
                            .equals("Retard")) {
                        retards++;
                    }
                }
            }

            result.add(
                    new WeeklyReportDTO(
                            user.getId(),
                            user.getNom(),
                            user.getPrenom(),
                            presence,
                            absence,
                            totalMinutes,
                            retards
                    )
            );
        }

        return result;
    }
    
    public List<MonthlyReportDTO> generateMonthlyReport(
            Long siteId,
            int year,
            int month
    ) {

        LocalDate start =
                LocalDate.of(year, month, 1);

        LocalDate end =
                start.withDayOfMonth(start.lengthOfMonth());

        List<Utilisateur> users =
                utilisateurRepository.findBySiteId(siteId);

        List<MonthlyReportDTO> result =
                new ArrayList<>();

        for (Utilisateur user : users) {

            long totalMinutes = 0;
            long totalDays = 0;
            long absence = 0;

            for (LocalDate d = start;
                 !d.isAfter(end);
                 d = d.plusDays(1)) {

                var daily =
                        generateDailyReport(siteId, d)
                                .stream()
                                .filter(r ->
                                        r.getUserId()
                                                .equals(user.getId()))
                                .findFirst()
                                .orElse(null);

                if (daily == null ||
                        daily.getStatut().equals("Absent")) {

                    absence++;
                } else {
                    totalDays++;
                    totalMinutes += daily.getTotalMinutes();
                }
            }

            double taux =
                    (totalDays + absence) == 0
                            ? 0
                            : ((double) totalDays
                            / (totalDays + absence)) * 100;

            result.add(
                    new MonthlyReportDTO(
                            user.getId(),
                            user.getNom(),
                            user.getPrenom(),
                            totalDays,
                            totalMinutes,
                            taux,
                            absence
                    )
            );
        }

        return result;
    }
    
    public List<DailyAttendanceDTO> getDailyAttendanceBySite(
            Long siteId,
            LocalDate date
    ) {

        List<Utilisateur> users =
                utilisateurRepository.findBySiteId(siteId);

        List<DailyAttendanceDTO> result = new ArrayList<>();

        for (Utilisateur user : users) {

            DailyAttendanceDTO attendance =
                    getDailyAttendance(user.getId(), date);

            result.add(attendance);
        }

        return result;
    }

}
