package com.example.gestionpointage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gestionpointage.model.Badge;

import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByBadgeUid(String badgeUid);
}