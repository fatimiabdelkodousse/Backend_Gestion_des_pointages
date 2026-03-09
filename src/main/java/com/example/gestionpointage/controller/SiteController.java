package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.SiteDTO;
import com.example.gestionpointage.entity.Site;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.repository.SiteRepository;
import com.example.gestionpointage.repository.UtilisateurRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/sites")
public class SiteController {

    private static final LocalTime ELIGIBILITY_CUTOFF = LocalTime.of(9, 0);

    private final SiteRepository siteRepository;
    private final UtilisateurRepository utilisateurRepository;

    // ═══ ✅ إضافة UtilisateurRepository كـ dependency ═══
    public SiteController(
            SiteRepository siteRepository,
            UtilisateurRepository utilisateurRepository
    ) {
        this.siteRepository = siteRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    public List<SiteDTO> getAllSites() {
        return siteRepository.findAll()
                .stream()
                .map(s -> new SiteDTO(
                        s.getId(),
                        s.getName(),
                        s.getAddress(),
                        s.isActive()
                ))
                .toList();
    }

    @PostMapping
    public SiteDTO create(@RequestBody SiteDTO dto) {

        Site site = new Site();
        site.setName(dto.getName());
        site.setAddress(dto.getAddress());
        site.setActive(true);

        Site saved = siteRepository.save(site);

        return new SiteDTO(
                saved.getId(),
                saved.getName(),
                saved.getAddress(),
                saved.isActive()
        );
    }

    @PutMapping("/{id}")
    public SiteDTO update(
            @PathVariable Long id,
            @RequestBody SiteDTO dto
    ) {

        Site site = siteRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Site introuvable"
                        )
                );

        site.setName(dto.getName());
        site.setAddress(dto.getAddress());
        site.setActive(dto.isActive());

        Site saved = siteRepository.save(site);

        return new SiteDTO(
                saved.getId(),
                saved.getName(),
                saved.getAddress(),
                saved.isActive()
        );
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        if (!siteRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Site introuvable"
            );
        }

        siteRepository.deleteById(id);
    }

    @PatchMapping("/{id}/toggle")
    public SiteDTO toggleActive(@PathVariable Long id) {

        Site site = siteRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Site introuvable"
                        )
                );

        boolean wasActive = site.isActive();
        site.setActive(!site.isActive());
        Site saved = siteRepository.save(site);

        // ═══════════════════════════════════════════════════════════
        // ✅ إعادة تفعيل الموقع → تحديث eligibleFrom لموظفيه
        // ═══════════════════════════════════════════════════════════
        if (!wasActive && saved.isActive()) {

            LocalDate eligibleFrom =
                    LocalTime.now().isBefore(ELIGIBILITY_CUTOFF)
                            ? LocalDate.now()
                            : LocalDate.now().plusDays(1);

            List<Utilisateur> users =
                    utilisateurRepository.findBySiteId(saved.getId());

            for (Utilisateur u : users) {
                if (!u.isDeleted()
                        && u.getBadge() != null
                        && u.getBadge().isActive()) {
                    u.setEligibleFrom(eligibleFrom);
                    utilisateurRepository.save(u);
                }
            }
        }

        return new SiteDTO(
                saved.getId(),
                saved.getName(),
                saved.getAddress(),
                saved.isActive()
        );
    }

    @GetMapping("/active")
    public List<SiteDTO> getActiveSites() {
        return siteRepository.findByActiveTrue()
                .stream()
                .map(s -> new SiteDTO(
                        s.getId(),
                        s.getName(),
                        s.getAddress(),
                        s.isActive()
                ))
                .toList();
    }
}