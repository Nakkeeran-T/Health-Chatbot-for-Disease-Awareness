package com.healthchatbot.repository;

import com.healthchatbot.entity.DiseaseInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseInfoRepository extends JpaRepository<DiseaseInfo, Long> {
    Optional<DiseaseInfo> findByNameIgnoreCase(String name);

    List<DiseaseInfo> findByCategory(String category);

    @Query("SELECT d FROM DiseaseInfo d WHERE " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.symptoms) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.treatment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DiseaseInfo> searchByKeyword(@Param("keyword") String keyword);
}
