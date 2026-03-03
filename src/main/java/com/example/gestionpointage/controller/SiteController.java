package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.SiteDTO;
import com.example.gestionpointage.entity.Site;
import com.example.gestionpointage.repository.SiteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/sites")
public class SiteController {

    private final SiteRepository siteRepository;

    public SiteController(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
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

        site.setActive(!site.isActive());
        Site saved = siteRepository.save(site);

        return new SiteDTO(
                saved.getId(),
                saved.getName(),
                saved.getAddress(),
                saved.isActive()
        );
    }
}