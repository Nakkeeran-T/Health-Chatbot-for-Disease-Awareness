package com.healthchatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "disease_info")
public class DiseaseInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String prevention;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    @Column
    private String category;

    @Column
    private String icdCode;

    @Column
    private Boolean contagious;

    @Column
    private String affectedAgeGroup;

    // Hindi translation
    @Column(name = "name_hi")
    private String nameHi;

    @Column(name = "symptoms_hi", columnDefinition = "TEXT")
    private String symptomsHi;

    @Column(name = "prevention_hi", columnDefinition = "TEXT")
    private String preventionHi;

    // Odia translation
    @Column(name = "name_or")
    private String nameOr;

    @Column(name = "symptoms_or", columnDefinition = "TEXT")
    private String symptomsOr;

    @Column(name = "prevention_or", columnDefinition = "TEXT")
    private String preventionOr;

    @Column
    private String imageUrl;
}
