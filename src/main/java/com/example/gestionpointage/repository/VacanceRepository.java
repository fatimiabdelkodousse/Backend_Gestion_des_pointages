package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.Vacance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VacanceRepository extends JpaRepository<Vacance, Long> {

    boolean existsByDate(LocalDate date);

    List<Vacance> findAllByOrderByDateAsc();

    List<Vacance> findByDateBetweenOrderByDateAsc(
            LocalDate start, LocalDate end);
}