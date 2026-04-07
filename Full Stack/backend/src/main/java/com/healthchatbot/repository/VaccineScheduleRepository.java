package com.healthchatbot.repository;

import com.healthchatbot.entity.VaccineSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaccineScheduleRepository extends JpaRepository<VaccineSchedule, Long> {
    List<VaccineSchedule> findByDisease(String disease);

    List<VaccineSchedule> findByMandatoryUnderNHMTrue();

    List<VaccineSchedule> findByTargetAgeContainingIgnoreCase(String ageGroup);
}
