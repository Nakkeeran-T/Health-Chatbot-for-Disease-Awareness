package com.healthchatbot.controller;

import com.healthchatbot.entity.VaccineSchedule;
import com.healthchatbot.service.VaccineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaccines")
@RequiredArgsConstructor
public class VaccineController {

    private final VaccineService vaccineService;

    @GetMapping
    public ResponseEntity<List<VaccineSchedule>> getAllVaccines() {
        return ResponseEntity.ok(vaccineService.getAllVaccines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaccineSchedule> getVaccineById(@PathVariable Long id) {
        return ResponseEntity.ok(vaccineService.getVaccineById(id));
    }

    @GetMapping("/mandatory")
    public ResponseEntity<List<VaccineSchedule>> getMandatoryVaccines() {
        return ResponseEntity.ok(vaccineService.getMandatoryVaccines());
    }

    @GetMapping("/disease/{disease}")
    public ResponseEntity<List<VaccineSchedule>> getVaccinesByDisease(@PathVariable String disease) {
        return ResponseEntity.ok(vaccineService.getVaccinesByDisease(disease));
    }

    @PostMapping
    public ResponseEntity<VaccineSchedule> createVaccine(@RequestBody VaccineSchedule vaccine) {
        return ResponseEntity.ok(vaccineService.saveVaccine(vaccine));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VaccineSchedule> updateVaccine(@PathVariable Long id, @RequestBody VaccineSchedule vaccine) {
        return ResponseEntity.ok(vaccineService.updateVaccine(id, vaccine));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVaccine(@PathVariable Long id) {
        vaccineService.deleteVaccine(id);
        return ResponseEntity.noContent().build();
    }
}
