package com.example.gestionpointage.service;

import com.example.gestionpointage.entity.Vacance;
import com.example.gestionpointage.repository.VacanceRepository;

// استيراد واجهة CommandLineRunner
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
// إضافة implements CommandLineRunner
public class VacanceInitService implements CommandLineRunner { 

    private final VacanceRepository vacanceRepository;

    public VacanceInitService(VacanceRepository vacanceRepository) {
        this.vacanceRepository = vacanceRepository;
    }

    // تم حذف @PostConstruct
    // هذه الدالة (run) ستعمل تلقائياً بعد أن يكتمل تشغيل التطبيق وتصبح قاعدة البيانات جاهزة
    @Override
    public void run(String... args) throws Exception {
        initMoroccanHolidays();
    }

    public void initMoroccanHolidays() {

        if (vacanceRepository.count() > 0) return;

        add("2027-01-01", "Nouvel An");
        add("2026-01-11", "Manifeste de l'Indépendance");
        add("2026-05-01", "Fête du Travail");
        add("2026-07-30", "Fête du Trône");
        add("2026-08-14", "Allégeance Oued Ed-Dahab");
        add("2026-08-20", "Révolution du Roi et du Peuple");
        add("2026-08-21", "Fête de la Jeunesse");
        add("2026-11-06", "Marche Verte");
        add("2026-11-18", "Fête de l'Indépendance");
    }

    private void add(String dateStr, String nom) {
        LocalDate date = LocalDate.parse(dateStr);
        if (!vacanceRepository.existsByDate(date)) {
            vacanceRepository.save(new Vacance(date, nom));
        }
    }
}