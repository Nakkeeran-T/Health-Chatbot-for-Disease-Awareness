package com.healthchatbot.service;

import com.healthchatbot.entity.DiseaseInfo;
import com.healthchatbot.repository.DiseaseInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiseaseService {

    private final DiseaseInfoRepository diseaseInfoRepository;

    public List<DiseaseInfo> getAllDiseases() {
        return diseaseInfoRepository.findAll();
    }

    public DiseaseInfo getDiseaseById(Long id) {
        return diseaseInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disease not found with id: " + id));
    }

    public Optional<DiseaseInfo> findByName(String name) {
        return diseaseInfoRepository.findByNameIgnoreCase(name);
    }

    public List<DiseaseInfo> getDiseasesByCategory(String category) {
        return diseaseInfoRepository.findByCategory(category);
    }

    /**
     * Full-text search across disease name and symptoms.
     * Used by chatbot to provide database-backed answers instead of hardcoded strings.
     */
    public List<DiseaseInfo> searchDiseases(String keyword) {
        return diseaseInfoRepository.searchByKeyword(keyword);
    }

    public DiseaseInfo saveDisease(DiseaseInfo disease) {
        return diseaseInfoRepository.save(disease);
    }

    public DiseaseInfo updateDisease(Long id, DiseaseInfo diseaseDetails) {
        DiseaseInfo disease = getDiseaseById(id);
        disease.setName(diseaseDetails.getName());
        disease.setCategory(diseaseDetails.getCategory());
        disease.setIcdCode(diseaseDetails.getIcdCode());
        disease.setSymptoms(diseaseDetails.getSymptoms());
        disease.setPrevention(diseaseDetails.getPrevention());
        disease.setTreatment(diseaseDetails.getTreatment());
        disease.setContagious(diseaseDetails.getContagious());
        disease.setAffectedAgeGroup(diseaseDetails.getAffectedAgeGroup());
        disease.setDescription(diseaseDetails.getDescription());
        disease.setNameHi(diseaseDetails.getNameHi());
        disease.setSymptomsHi(diseaseDetails.getSymptomsHi());
        disease.setPreventionHi(diseaseDetails.getPreventionHi());
        disease.setNameOr(diseaseDetails.getNameOr());
        disease.setSymptomsOr(diseaseDetails.getSymptomsOr());
        disease.setPreventionOr(diseaseDetails.getPreventionOr());
        return diseaseInfoRepository.save(disease);
    }

    public void deleteDisease(Long id) {
        diseaseInfoRepository.deleteById(id);
    }
}

