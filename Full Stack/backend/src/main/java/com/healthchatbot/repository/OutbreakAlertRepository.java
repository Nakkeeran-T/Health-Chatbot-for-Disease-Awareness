package com.healthchatbot.repository;

import com.healthchatbot.entity.OutbreakAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutbreakAlertRepository extends JpaRepository<OutbreakAlert, Long> {
    List<OutbreakAlert> findByActiveTrue();

    List<OutbreakAlert> findByRegionIgnoreCaseAndActiveTrue(String region);

    List<OutbreakAlert> findByDistrictIgnoreCaseAndActiveTrue(String district);

    List<OutbreakAlert> findBySeverityAndActiveTrue(OutbreakAlert.SeverityLevel severity);

    List<OutbreakAlert> findByDiseaseIgnoreCaseAndActiveTrue(String disease);
}
