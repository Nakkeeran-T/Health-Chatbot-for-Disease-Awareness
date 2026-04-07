package com.healthchatbot.controller;

import com.healthchatbot.entity.DiseaseInfo;
import com.healthchatbot.service.DiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diseases")
@RequiredArgsConstructor
public class DiseaseController {

    private final DiseaseService diseaseService;

    @GetMapping
    public ResponseEntity<List<DiseaseInfo>> getAllDiseases() {
        return ResponseEntity.ok(diseaseService.getAllDiseases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiseaseInfo> getDiseaseById(@PathVariable Long id) {
        return ResponseEntity.ok(diseaseService.getDiseaseById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<DiseaseInfo>> getDiseasesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(diseaseService.getDiseasesByCategory(category));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DiseaseInfo>> searchDiseases(@RequestParam String q) {
        return ResponseEntity.ok(diseaseService.searchDiseases(q));
    }

    @PostMapping
    public ResponseEntity<DiseaseInfo> createDisease(@RequestBody DiseaseInfo disease) {
        return ResponseEntity.ok(diseaseService.saveDisease(disease));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiseaseInfo> updateDisease(@PathVariable Long id, @RequestBody DiseaseInfo disease) {
        return ResponseEntity.ok(diseaseService.updateDisease(id, disease));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisease(@PathVariable Long id) {
        diseaseService.deleteDisease(id);
        return ResponseEntity.noContent().build();
    }
}
