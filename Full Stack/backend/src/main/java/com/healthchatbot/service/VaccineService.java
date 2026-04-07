package com.healthchatbot.service;

import com.healthchatbot.entity.VaccineSchedule;
import com.healthchatbot.repository.VaccineScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VaccineService {

    private final VaccineScheduleRepository vaccineRepository;

    public List<VaccineSchedule> getAllVaccines() {
        return vaccineRepository.findAll();
    }

    public VaccineSchedule getVaccineById(Long id) {
        return vaccineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaccine not found with id: " + id));
    }

    public List<VaccineSchedule> getMandatoryVaccines() {
        return vaccineRepository.findByMandatoryUnderNHMTrue();
    }

    public VaccineSchedule saveVaccine(VaccineSchedule vaccine) {
        return vaccineRepository.save(vaccine);
    }

    public VaccineSchedule updateVaccine(Long id, VaccineSchedule details) {
        VaccineSchedule vaccine = getVaccineById(id);
        vaccine.setVaccineName(details.getVaccineName());
        vaccine.setDescription(details.getDescription());
        vaccine.setTargetAge(details.getTargetAge());
        vaccine.setDoseSchedule(details.getDoseSchedule());
        vaccine.setDisease(details.getDisease());
        vaccine.setNumberOfDoses(details.getNumberOfDoses());
        vaccine.setAdministrationRoute(details.getAdministrationRoute());
        vaccine.setMandatoryUnderNHM(details.isMandatoryUnderNHM());
        vaccine.setAvailability(details.getAvailability());
        vaccine.setVaccineNameHi(details.getVaccineNameHi());
        vaccine.setVaccineNameOr(details.getVaccineNameOr());
        return vaccineRepository.save(vaccine);
    }

    public void deleteVaccine(Long id) {
        vaccineRepository.deleteById(id);
    }
}
