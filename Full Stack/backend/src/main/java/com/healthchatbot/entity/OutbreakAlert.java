package com.healthchatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "outbreak_alerts")
public class OutbreakAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String disease;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severity;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private boolean active;

    @Column
    private int reportedCases;

    @Column
    private String precautions;

    @Column
    private String contactNumber;

    // Translations
    @Column(name = "title_hi")
    private String titleHi;

    @Column(name = "title_or")
    private String titleOr;

    @Column(name = "description_hi", columnDefinition = "TEXT")
    private String descriptionHi;

    @Column(name = "description_or", columnDefinition = "TEXT")
    private String descriptionOr;

    public enum SeverityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @PrePersist
    protected void onCreate() {
        this.reportedAt = LocalDateTime.now();
        this.active = true;
    }
}
