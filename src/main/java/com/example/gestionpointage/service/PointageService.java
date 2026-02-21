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
import com.example.gestionpointage.model.Role;
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
    // 🔥 CENTRALIZED ATTENDANCE LOGIC (IMPORTANT)
    // =====================================================

    private AttendanceStatus resolveAttendanceStatus(
            Utilisateur user,
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(23, 59, 59);

        LocalTime workStart      = LocalTime.of(9, 0);
        LocalTime toleranceLimit = LocalTime.of(9, 5);
        LocalTime absenceLimit   = LocalTime.of(18, 0);

        LocalDateTime now = LocalDateTime.now();
        boolean isToday = date.equals(LocalDate.now());
        boolean afterWorkDay = !isToday || now.isAfter(date.atTime(absenceLimit));

        var firstEntryOpt =
                pointageRepository
                        .findTopByUserAndSiteIdAndTypeAndTimestampBetweenOrderByTimestampAsc(
                                user,
                                siteId,
                                PointageType.ENTREE,
                                start,
                                end
                        );

        if (firstEntryOpt.isEmpty()) {
            if (afterWorkDay) {
                return AttendanceStatus.ABSENT;
            }
            return AttendanceStatus.ABSENT; 
        }

        LocalTime arrival =
                firstEntryOpt.get()
                        .getTimestamp()
                        .toLocalTime();

        if (arrival.isBefore(workStart)) {
            return AttendanceStatus.EARLY;
        }

        if (!arrival.isAfter(toleranceLimit)) {
            return AttendanceStatus.ON_TIME;
        }

        return AttendanceStatus.LATE;
    }

    // =====================================================
    // CREATE POINTAGE
    // =====================================================

 // =====================================================
 // CREATE POINTAGE - FIXED (منع الموظفين المحذوفين)
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
                                "Utilisateur introuvable (id=" + userId + ")"
                        )
                );

        if (user.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "RAISON: Utilisateur supprimé (id=" + userId + ")"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "RAISON: Utilisateur désactivé (id=" + userId + ")"
            );
        }

        if (user.getRole() != Role.EMPLOYE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "RAISON: Role=" + user.getRole() + " (seuls EMPLOYE peuvent pointer)"
            );
        }

        if (user.getBadge() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "RAISON: Badge null pour user id=" + userId
            );
        }

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
         if (last.getType() == type) {
             throw new ResponseStatusException(
                     HttpStatus.BAD_REQUEST,
                     type == PointageType.ENTREE
                             ? "Entrée déjà enregistrée"
                             : "Sortie déjà enregistrée"
             );
         }
     }

     Pointage pointage = new Pointage();
     pointage.setUser(user);
     pointage.setSite(site);
     pointage.setType(type);
     pointage.setTimestamp(LocalDateTime.now());

     Pointage saved = pointageRepository.save(pointage);

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
    // DAILY ATTENDANCE (FOR ONE USER)
    // =====================================================

    public DailyAttendanceDTO getDailyAttendance(
            Long userId,
            LocalDate date
    ) {

        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow();

        AttendanceStatus status =
                resolveAttendanceStatus(user, user.getSite().getId(), date);

        if (status == null) {
            status = AttendanceStatus.ABSENT;
        }

        return new DailyAttendanceDTO(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                status,
                0
        );
    }

    // =====================================================
    // DAILY STATS BY SITE (DASHBOARD)
    // =====================================================

    public AttendanceStatsDTO getDailyStatsBySite(
            Long siteId,
            LocalDate date
    ) {

        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        long total = users.size();
        long early = 0;
        long onTime = 0;
        long late = 0;
        long absent = 0;

        for (Utilisateur user : users) {

            AttendanceStatus status =
                    resolveAttendanceStatus(user, siteId, date);

            if (status == null) {
                absent++; 
                continue;
            }

            switch (status) {
                case EARLY -> early++;
                case ON_TIME -> onTime++;
                case LATE -> late++;
                case ABSENT -> absent++;
            }
        }

        long present = early + onTime + late;

        return new AttendanceStatsDTO(
                total,
                present,
                early,
                onTime,
                late,
                absent
        );
    }

    // =====================================================
    // ATTENDANCE LIST BY SITE
    // =====================================================

    
    public List<DailyAttendanceDTO> getDailyAttendanceBySite(
            Long siteId,
            LocalDate date,
            String statusFilter
    ) {

        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        List<DailyAttendanceDTO> result = new ArrayList<>();

        for (Utilisateur user : users) {

            AttendanceStatus status =
                    resolveAttendanceStatus(user, siteId, date);

            if (status == null) {
                status = AttendanceStatus.ABSENT;
            }

            // ═══ فلتر الحالة ═══
            if (statusFilter != null && !statusFilter.isEmpty()) {
                boolean match = false;

                switch (statusFilter.toUpperCase()) {
                    case "PRESENT":
                        // ═══ "Présent" = EARLY + ON_TIME + LATE ═══
                        match = (status == AttendanceStatus.EARLY
                                || status == AttendanceStatus.ON_TIME
                                || status == AttendanceStatus.LATE);
                        break;
                    case "EARLY":
                        match = (status == AttendanceStatus.EARLY);
                        break;
                    case "ON_TIME":
                        match = (status == AttendanceStatus.ON_TIME);
                        break;
                    case "LATE":
                        match = (status == AttendanceStatus.LATE);
                        break;
                    case "ABSENT":
                        match = (status == AttendanceStatus.ABSENT);
                        break;
                    default:
                        match = true;
                        break;
                }

                if (!match) continue;
            }

            result.add(
                    new DailyAttendanceDTO(
                            user.getId(),
                            user.getNom(),
                            user.getPrenom(),
                            status,
                            0
                    )
            );
        }

        return result;
    }
    
    public List<Pointage> getTodayBySite(Long siteId) {

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<Pointage> pointages = pointageRepository
            .findBySiteIdAndTimestampBetweenOrderByTimestampDesc(
                siteId, start, end
            );

        pointages.removeIf(p -> p.getUser().isDeleted());

        return pointages;
    }

    public List<DailyReportRowDTO> generateDailyReport(
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23,59,59);

        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

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
                utilisateurRepository.findActiveEmployeesBySite(siteId);

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
                utilisateurRepository.findActiveEmployeesBySite(siteId);

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
    
    public List<String> getAbsentUsersNames(
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(23,59,59);

        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

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
}
