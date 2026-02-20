package com.example.gestionpointage.controller;
import com.example.gestionpointage.dto.SiteDTO;
import com.example.gestionpointage.repository.SiteRepository;
import org.springframework.web.bind.annotation.*;

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
                .map(s -> new SiteDTO(s.getId(), s.getName()))
                .toList();
    }
}
