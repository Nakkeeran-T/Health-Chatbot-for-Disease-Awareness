package com.healthchatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vaccine_schedules")
public class VaccineSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vaccineName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String targetAge;

    @Column
    private String doseSchedule;

    @Column
    private String disease;

    @Column
    private int numberOfDoses;

    @Column
    private String administrationRoute;

    @Column
    private boolean mandatoryUnderNHM;

    @Column
    private String availability;

    // Translations
    @Column(name = "vaccine_name_hi")
    private String vaccineNameHi;

    @Column(name = "vaccine_name_or")
    private String vaccineNameOr;

    @Column(name = "description_hi", columnDefinition = "TEXT")
    private String descriptionHi;

    @Column(name = "description_or", columnDefinition = "TEXT")
    private String descriptionOr;
}
