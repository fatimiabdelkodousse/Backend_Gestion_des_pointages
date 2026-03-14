package com.example.gestionpointage.service;

import com.example.gestionpointage.entity.Pointage;
import com.example.gestionpointage.repository.BadgeRepository;
import com.example.gestionpointage.model.Badge;
import com.example.gestionpointage.entity.Site;
import com.example.gestionpointage.model.PointageType;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.PointageRepository;
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

    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    private final PointageRepository pointageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final BadgeRepository badgeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PointageService(
            PointageRepository pointageRepository,
            UtilisateurRepository utilisateurRepository,
            BadgeRepository badgeRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.pointageRepository = pointageRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.badgeRepository = badgeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    // =====================================================
    // ✅ ELIGIBILITY HELPER
    // =====================================================

    /**
     * هل الموظف مؤهل للمحاسبة على الغياب في تاريخ معين؟
     * ⚠️ هذا يُستخدم فقط عندما لا يوجد pointage
     *     إذا كان عنده pointage، يُعرض دائماً
     */
    private boolean isEligibleForDate(Utilisateur user, LocalDate date) {
        return user.getEligibleFrom() == null
                || !date.isBefore(user.getEligibleFrom());
    }

    // =====================================================
    // 🔥 CENTRALIZED ATTENDANCE LOGIC
    // =====================================================

    /**
     * ✅ يرجع null إذا الموظف غير مؤهل ولا عنده pointage
     *    (يعني يجب تجاهله بالكامل)
     */
    private AttendanceStatus resolveAttendanceStatus(
            Utilisateur user,
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = startOfDay(date);
        LocalDateTime end   = endOfDay(date);

        LocalTime workStart      = LocalTime.of(9, 0);
        LocalTime toleranceLimit = LocalTime.of(9, 5);

        var firstEntryOpt =
                pointageRepository
                        .findTopByUserAndTypeAndTimestampBetweenOrderByTimestampAsc(
                                user,
                                PointageType.ENTREE,
                                start,
                                end
                        );

        if (firstEntryOpt.isEmpty()) {
            // ══════════════════════════════════════════════════════
            // ✅ لا يوجد pointage → نتحقق من الأهلية
            //    غير مؤهل → null (تجاهل)
            //    مؤهل → نحدد الحالة
            // ══════════════════════════════════════════════════════
            if (!isEligibleForDate(user, date)) {
                return null;  // ✅ تجاهل — لا يُحسب لا غائب ولا حاضر
            }

            if (date.equals(LocalDate.now())
                    && LocalTime.now().isBefore(WORK_END)) {
                return AttendanceStatus.NOT_YET;
            }
            return AttendanceStatus.ABSENT;
        }

        // ══════════════════════════════════════════════════════
        // ✅ يوجد pointage → يُعالج عادي بغض النظر عن eligibleFrom
        // ══════════════════════════════════════════════════════
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
    // ✅ CREATE POINTAGE
    // =====================================================

    public Pointage createPointageByBadge(
            String badgeUid,
            LocalDateTime timestamp
    ) {

        if (!timestamp.toLocalDate().equals(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le pointage doit être pour la date d'aujourd'hui ("
                            + LocalDate.now() + ")"
            );
        }

        Badge badge = badgeRepository.findByBadgeUid(badgeUid)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Badge introuvable: " + badgeUid
                        )
                );

        if (!badge.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Badge désactivé"
            );
        }

        Utilisateur user = badge.getUtilisateur();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Aucun utilisateur associé à ce badge"
            );
        }

        if (user.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Utilisateur supprimé"
            );
        }

        if (user.getRole() != Role.EMPLOYE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seuls les employés peuvent pointer"
            );
        }

        Site site = user.getSite();
        if (site == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun site associé à cet utilisateur"
            );
        }

        if (!site.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Le site « " + site.getName() + " » est désactivé"
            );
        }

        LocalDate pointageDate = timestamp.toLocalDate();
        LocalDateTime dayStart = startOfDay(pointageDate);
        LocalDateTime dayEnd   = endOfDay(pointageDate);

        Optional<Pointage> lastSameDayOpt =
                pointageRepository
                        .findTopByUserAndTimestampBetweenOrderByTimestampDesc(
                                user,
                                dayStart,
                                dayEnd
                        );

        PointageType type;

        if (lastSameDayOpt.isEmpty()) {
            type = PointageType.ENTREE;
        } else {
            Pointage lastToday = lastSameDayOpt.get();
            if (lastToday.getType() == PointageType.ENTREE) {
                type = PointageType.SORTIE;
            } else {
                type = PointageType.ENTREE;
            }
        }

        Pointage pointage = new Pointage();
        pointage.setUser(user);
        pointage.setSite(site);
        pointage.setType(type);
        pointage.setTimestamp(timestamp);

        Pointage saved = pointageRepository.save(pointage);

        Long siteId = site.getId();

        messagingTemplate.convertAndSend(
                "/topic/stats/" + siteId,
                getDailyStatsBySite(siteId, pointageDate)
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

        // ✅ إذا null (غير مؤهل بدون pointage)
        if (status == null) {
            status = AttendanceStatus.NOT_YET;
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
    // ✅ DAILY STATS BY SITE (DASHBOARD)
    // =====================================================

    public AttendanceStatsDTO getDailyStatsBySite(
            Long siteId,
            LocalDate date
    ) {

        // ✅ كل الموظفين النشطين (بدون فلتر eligibleFrom مسبق)
        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        long total  = 0;    // ✅ يُحسب فقط للمؤهلين
        long early  = 0;
        long onTime = 0;
        long late   = 0;
        long absent = 0;
        long notYet = 0;

        for (Utilisateur user : users) {

            AttendanceStatus status =
                    resolveAttendanceStatus(user, siteId, date);

            // ✅ null = غير مؤهل ولا عنده pointage → تجاهل
            if (status == null) continue;

            total++;

            switch (status) {
                case EARLY   -> early++;
                case ON_TIME -> onTime++;
                case LATE    -> late++;
                case ABSENT  -> absent++;
                case NOT_YET -> notYet++;
            }
        }

        long present = early + onTime + late;

        return new AttendanceStatsDTO(
                total,
                present,
                early,
                onTime,
                late,
                absent,
                notYet
        );
    }

    // =====================================================
    // ✅ ATTENDANCE LIST BY SITE
    // =====================================================

    public List<DailyAttendanceDTO> getDailyAttendanceBySite(
            Long siteId,
            LocalDate date,
            String statusFilter
    ) {

        // ✅ كل الموظفين النشطين
        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        List<DailyAttendanceDTO> result = new ArrayList<>();

        for (Utilisateur user : users) {

            AttendanceStatus status =
                    resolveAttendanceStatus(user, siteId, date);

            // ✅ null = تجاهل
            if (status == null) continue;

            if (statusFilter != null && !statusFilter.isEmpty()) {
                boolean match = switch (statusFilter.toUpperCase()) {
                    case "PRESENT" ->
                            status == AttendanceStatus.EARLY
                         || status == AttendanceStatus.ON_TIME
                         || status == AttendanceStatus.LATE;
                    case "EARLY"   -> status == AttendanceStatus.EARLY;
                    case "ON_TIME" -> status == AttendanceStatus.ON_TIME;
                    case "LATE"    -> status == AttendanceStatus.LATE;
                    case "ABSENT"  -> status == AttendanceStatus.ABSENT;
                    case "NOT_YET" -> status == AttendanceStatus.NOT_YET;
                    default        -> true;
                };

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

    // =====================================================
    // TODAY BY SITE
    // =====================================================

    public List<Pointage> getTodayBySite(Long siteId) {

        LocalDate today = LocalDate.now();
        LocalDateTime start = startOfDay(today);
        LocalDateTime end   = endOfDay(today);

        List<Pointage> pointages = pointageRepository
                .findBySiteIdAndTimestampBetweenOrderByTimestampDesc(
                        siteId, start, end
                );

        pointages.removeIf(p -> p.getUser().isDeleted());

        return pointages;
    }

    // =====================================================
    // ✅ DAILY REPORT
    // =====================================================

    public List<DailyReportRowDTO> generateDailyReport(
            Long siteId,
            LocalDate date
    ) {

        if (date.isAfter(LocalDate.now())) {
            return new ArrayList<>();
        }

        LocalDateTime start = startOfDay(date);
        LocalDateTime end   = endOfDay(date);

        // ✅ كل الموظفين النشطين (بدون فلتر eligibleFrom)
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

                // ══════════════════════════════════════════════════
                // ✅ لا يوجد pointage → نتحقق من الأهلية
                //    غير مؤهل → تجاهل (لا يظهر في التقرير)
                // ══════════════════════════════════════════════════
                if (!isEligibleForDate(user, date)) {
                    continue;
                }

                String statut;
                if (date.equals(LocalDate.now())
                        && LocalTime.now().isBefore(WORK_END)) {
                    statut = "En attente";
                } else {
                    statut = "Absent";
                }

                rows.add(
                        new DailyReportRowDTO(
                                user.getId(),
                                user.getNom(),
                                user.getPrenom(),
                                null,
                                null,
                                0,
                                statut
                        )
                );
                continue;
            }

            // ══════════════════════════════════════════════════
            // ✅ يوجد pointage → يُعالج عادي (بغض النظر عن eligibleFrom)
            // ══════════════════════════════════════════════════

            LocalTime entree =
                    pointages.get(0).getTimestamp().toLocalTime();

            LocalTime sortie =
                    pointages.get(pointages.size() - 1)
                            .getTimestamp()
                            .toLocalTime();

            long totalMinutes = 0;

            for (int i = 0; i < pointages.size() - 1; i++) {
                if (pointages.get(i).getType() == PointageType.ENTREE
                        && pointages.get(i + 1).getType() == PointageType.SORTIE) {

                    totalMinutes +=
                            Duration.between(
                                    pointages.get(i).getTimestamp(),
                                    pointages.get(i + 1).getTimestamp()
                            ).toMinutes();
                }
            }

            String statut = "Présent";
            if (entree.isAfter(LocalTime.of(9, 5))) {
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

    // =====================================================
    // ✅ WEEKLY REPORT
    // =====================================================

    public List<WeeklyReportDTO> generateWeeklyReport(
            Long siteId,
            LocalDate date
    ) {

        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek   = startOfWeek.plusDays(6);

        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = endOfWeek.isAfter(today) ? today : endOfWeek;

        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        List<WeeklyReportDTO> result = new ArrayList<>();

        for (Utilisateur user : users) {

            long presence     = 0;
            long absence      = 0;
            long totalMinutes = 0;
            long retards      = 0;

            for (LocalDate d = startOfWeek;
                 !d.isAfter(effectiveEnd);
                 d = d.plusDays(1)) {

                // ✅ لا نفلتر بـ isEligibleForDate هنا
                //    generateDailyReport يتكفل بالفلترة

                var daily =
                        generateDailyReport(siteId, d)
                                .stream()
                                .filter(r ->
                                        r.getUserId()
                                                .equals(user.getId()))
                                .findFirst()
                                .orElse(null);

                // ══════════════════════════════════════════════════
                // ✅ null = الموظف غير مؤهل لهذا اليوم (لا بيانات)
                //    → لا نحسبه لا حضور ولا غياب
                // ══════════════════════════════════════════════════
                if (daily == null) {
                    continue;
                }

                if (daily.getStatut().equals("Absent")) {
                    absence++;
                } else if (daily.getStatut().equals("En attente")) {
                    continue;
                } else {
                    presence++;
                    totalMinutes += daily.getTotalMinutes();
                    if (daily.getStatut().equals("Retard")) {
                        retards++;
                    }
                }
            }

            if (presence + absence == 0) continue;

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

    // =====================================================
    // ✅ MONTHLY REPORT
    // =====================================================

    public List<MonthlyReportDTO> generateMonthlyReport(
            Long siteId,
            int year,
            int month
    ) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());

        LocalDate today = LocalDate.now();
        if (end.isAfter(today)) {
            end = today;
        }

        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        List<MonthlyReportDTO> result = new ArrayList<>();

        for (Utilisateur user : users) {

            long totalMinutes = 0;
            long totalDays    = 0;
            long absence      = 0;

            for (LocalDate d = start;
                 !d.isAfter(end);
                 d = d.plusDays(1)) {

                // ✅ لا نفلتر بـ isEligibleForDate هنا

                var daily =
                        generateDailyReport(siteId, d)
                                .stream()
                                .filter(r ->
                                        r.getUserId()
                                                .equals(user.getId()))
                                .findFirst()
                                .orElse(null);

                // ✅ null = غير مؤهل لهذا اليوم → تجاهل
                if (daily == null) {
                    continue;
                }

                if (daily.getStatut().equals("Absent")) {
                    absence++;
                } else if (daily.getStatut().equals("En attente")) {
                    continue;
                } else {
                    totalDays++;
                    totalMinutes += daily.getTotalMinutes();
                }
            }

            if (totalDays + absence == 0) continue;

            double taux =
                    ((double) totalDays / (totalDays + absence)) * 100;

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

    // =====================================================
    // ✅ ABSENT USERS
    // =====================================================

    public List<String> getAbsentUsersNames(
            Long siteId,
            LocalDate date
    ) {

        LocalDateTime start = startOfDay(date);
        LocalDateTime end   = endOfDay(date);

        // ✅ كل الموظفين النشطين
        List<Utilisateur> users =
                utilisateurRepository.findActiveEmployeesBySite(siteId);

        List<String> absentNames = new ArrayList<>();

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

            if (firstEntryOpt.isEmpty()) {

                // ✅ لا pointage → نتحقق من الأهلية
                if (!isEligibleForDate(user, date)) {
                    continue;
                }

                boolean isConfirmedAbsent =
                        !date.equals(LocalDate.now())
                     || !LocalTime.now().isBefore(WORK_END);

                if (isConfirmedAbsent) {
                    absentNames.add(
                            user.getNom() + " " + user.getPrenom()
                    );
                }
            }
        }

        return absentNames;
    }

    // =====================================================
    // DAILY REPORT (SINGLE USER)
    // =====================================================

    public DailyReportDTO getDailyReport(Long userId, LocalDate date) {

        LocalDateTime start = startOfDay(date);
        LocalDateTime end   = endOfDay(date);

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
            Pointage next    = pointages.get(i + 1);

            if (current.getType() == PointageType.ENTREE
                    && next.getType() == PointageType.SORTIE) {

                totalMinutes += Duration.between(
                        current.getTimestamp(),
                        next.getTimestamp()
                ).toMinutes();
            }
        }

        return new DailyReportDTO(date, totalMinutes);
    }
}