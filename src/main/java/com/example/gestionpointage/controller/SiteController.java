package com.example.gestionpointage.controller;

import com.example.gestionpointage.entity.Site;
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
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }
}
