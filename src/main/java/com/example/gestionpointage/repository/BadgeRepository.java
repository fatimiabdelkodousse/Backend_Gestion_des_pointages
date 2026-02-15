package com.example.gestionpointage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gestionpointage.model.Badge;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
}
