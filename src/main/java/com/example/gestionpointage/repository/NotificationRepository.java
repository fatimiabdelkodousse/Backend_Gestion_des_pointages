package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface NotificationRepository
        extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findBySiteIdOrderByCreatedAtDesc(Long siteId);
    
    boolean existsBySiteIdAndCreatedAtBetween(
    	    Long siteId,
    	    LocalDateTime start,
    	    LocalDateTime end
    	);
}
