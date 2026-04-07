package com.healthchatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDTO {

    // Overview counts
    private long totalUsers;
    private long activeUsers;
    private long totalMessages;
    private long totalSessions;
    private long totalDiseases;
    private long totalVaccines;
    private long activeAlerts;
    private long totalAlerts;

    // Awareness KPI (target: 20% increase)
    private long messagesToday;
    private long messagesThisWeek;
    private long messagesThisMonth;
    private double awarenessGrowthPercent;   // % growth vs last month

    // Top disease queries
    private List<Map<String, Object>> topIntents;
    private List<Map<String, Object>> topDistricts;

    // Language breakdown
    private Map<String, Long> languageBreakdown;

    // Alert stats
    private Map<String, Long> alertsBySeverity;

    // System health
    private boolean mlApiOnline;
    private boolean twilioReady;
    private String lastScanTime;
}
