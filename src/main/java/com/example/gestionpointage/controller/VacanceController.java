package com.example.gestionpointage.controller;

import com.example.gestionpointage.dto.VacanceDTO;
import com.example.gestionpointage.entity.Vacance;
import com.example.gestionpointage.repository.VacanceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/vacances")
public class VacanceController {

    private final VacanceRepository vacanceRepository;

    public VacanceController(VacanceRepository vacanceRepository) {
        this.vacanceRepository = vacanceRepository;
    }

    @GetMapping
    public List<VacanceDTO> getAll() {
        return vacanceRepository.findAllByOrderByDateAsc()
                .stream()
                .map(v -> new VacanceDTO(v.getId(), v.getDate(), v.getNom()))
                .toList();
    }

    @PostMapping
    public VacanceDTO create(@RequestBody VacanceDTO dto) {

        if (vacanceRepository.existsByDate(dto.getDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une vacance existe déjà pour cette date"
            );
        }

        Vacance v = new Vacance(dto.getDate(), dto.getNom());
        Vacance saved = vacanceRepository.save(v);

        return new VacanceDTO(saved.getId(), saved.getDate(), saved.getNom());
    }

    @PutMapping("/{id}")
    public VacanceDTO update(
            @PathVariable Long id,
            @RequestBody VacanceDTO dto
    ) {

        Vacance v = vacanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vacance introuvable"
                ));

        v.setDate(dto.getDate());
        v.setNom(dto.getNom());

        Vacance saved = vacanceRepository.save(v);

        return new VacanceDTO(saved.getId(), saved.getDate(), saved.getNom());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        if (!vacanceRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Vacance introuvable"
            );
        }

        vacanceRepository.deleteById(id);
    }
}