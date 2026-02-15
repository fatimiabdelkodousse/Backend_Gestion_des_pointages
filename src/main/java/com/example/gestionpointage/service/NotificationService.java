package com.example.gestionpointage.service;

import com.example.gestionpointage.entity.AppNotification;

import com.example.gestionpointage.entity.Site;
import com.example.gestionpointage.entity.DailyAbsenceLog;
import com.example.gestionpointage.repository.NotificationRepository;
import com.example.gestionpointage.repository.SiteRepository;
import com.example.gestionpointage.repository.DailyAbsenceLogRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SiteRepository siteRepository;
    private final PointageService pointageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DailyAbsenceLogRepository dailyAbsenceLogRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            SiteRepository siteRepository,
            PointageService pointageService,
            SimpMessagingTemplate messagingTemplate,
            DailyAbsenceLogRepository dailyAbsenceLogRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.siteRepository = siteRepository;
        this.pointageService = pointageService;
        this.messagingTemplate = messagingTemplate;
        this.dailyAbsenceLogRepository = dailyAbsenceLogRepository;
    }

    // ⏰ يتحقق كل 5 دقائق
    @Scheduled(cron = "0 */5 * * * ?")
    public void generateDailyAbsenceNotifications() {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // لا ننفذ قبل 18:00
        if (now.isBefore(LocalTime.of(18, 0))) {
            return;
        }

        List<Site> sites = siteRepository.findAll();

        for (Site site : sites) {

            // 🔥 التحقق الحقيقي (من جدول log وليس جدول الإشعارات)
            boolean alreadyGenerated =
                    dailyAbsenceLogRepository
                            .existsBySiteIdAndDate(
                                    site.getId(),
                                    today
                            );

            if (alreadyGenerated) {
                continue;
            }

            var stats =
                    pointageService.getDailyStatsBySite(
                            site.getId(),
                            today
                    );

            long absentCount = stats.getAbsent();

            if (absentCount > 0) {

                List<String> absentNames =
                        pointageService.getAbsentUsersNames(
                                site.getId(),
                                today
                        );

                String title = "Absences du jour";

                String message =
                        absentCount +
                                " employés absents aujourd'hui";

                String details =
                        String.join(",", absentNames);

                AppNotification notification =
                        new AppNotification(
                                title,
                                message,
                                details,
                                site
                        );

                AppNotification saved =
                        notificationRepository.save(notification);

                // 🔥 نسجل أنه تم التوليد اليوم
                dailyAbsenceLogRepository.save(
                        new DailyAbsenceLog(today, site)
                );

                // 🔥 WebSocket push
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + site.getId(),
                        saved
                );
            }
        }
    }

    public List<AppNotification> getBySite(Long siteId) {
        return notificationRepository
                .findBySiteIdOrderByCreatedAtDesc(siteId);
    }

    public void markAsRead(Long id) {
        AppNotification n =
                notificationRepository.findById(id).orElseThrow();

        n.setReadStatus(true);
        notificationRepository.save(n);
    }

    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new RuntimeException("Notification introuvable");
        }
        notificationRepository.deleteById(id);
    }
}
