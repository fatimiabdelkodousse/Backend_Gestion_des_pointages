package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findByActiveTrue();
}