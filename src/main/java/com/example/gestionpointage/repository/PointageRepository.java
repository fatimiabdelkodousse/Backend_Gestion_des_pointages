package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gestionpointage.model.Utilisateur;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import com.example.gestionpointage.model.PointageType;

public interface PointageRepository extends JpaRepository<Pointage, Long> {

    List<Pointage> findByUserId(Long userId);

    List<Pointage> findBySiteId(Long siteId);
    
    Optional<Pointage> findTopByUserOrderByTimestampDesc(Utilisateur user);
    
    List<Pointage> findByUserIdAndTimestampBetweenOrderByTimestampAsc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );
    
    Optional<Pointage> findTopByUserAndTypeAndTimestampBetweenOrderByTimestampAsc(
            Utilisateur user,
            PointageType type,
            LocalDateTime start,
            LocalDateTime end
    );
    
    Optional<Pointage> findTopByUserAndSiteIdAndTypeAndTimestampBetweenOrderByTimestampAsc(
            Utilisateur user,
            Long siteId,
            PointageType type,
            LocalDateTime start,
            LocalDateTime end
    );
    
    List<Pointage> findBySiteIdAndTimestampBetweenOrderByTimestampDesc(
            Long siteId,
            LocalDateTime start,
            LocalDateTime end
    );
    
}
