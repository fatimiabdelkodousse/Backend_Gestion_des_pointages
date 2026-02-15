package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.DailyAbsenceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface DailyAbsenceLogRepository
        extends JpaRepository<DailyAbsenceLog, Long> {

    boolean existsBySiteIdAndDate(
            Long siteId,
            LocalDate date
    );
}
