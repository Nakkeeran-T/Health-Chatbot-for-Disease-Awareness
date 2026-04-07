package com.healthchatbot.service;

import com.healthchatbot.dto.AnalyticsDTO;
import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.entity.User;
import com.healthchatbot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;
    private final DiseaseInfoRepository diseaseInfoRepository;
    private final VaccineScheduleRepository vaccineRepository;
    private final OutbreakAlertRepository alertRepository;
    private final NotificationService notificationService;

    public AnalyticsDTO getFullAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);
        LocalDateTime prevMonthStart = now.minusDays(60);

        // --- User stats ---
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::isActive).count();

        // --- Message stats ---
        long totalMessages = messageRepository.countByType(com.healthchatbot.entity.ChatMessage.MessageType.USER);
        long totalSessions = sessionRepository.count();
        long messagesToday = messageRepository.countByTypeAndSentAtBetween(com.healthchatbot.entity.ChatMessage.MessageType.USER, todayStart, now);
        long messagesThisWeek = messageRepository.countByTypeAndSentAtBetween(com.healthchatbot.entity.ChatMessage.MessageType.USER, weekStart, now);
        long messagesThisMonth = messageRepository.countByTypeAndSentAtBetween(com.healthchatbot.entity.ChatMessage.MessageType.USER, monthStart, now);
        long messagesPrevMonth = messageRepository.countByTypeAndSentAtBetween(com.healthchatbot.entity.ChatMessage.MessageType.USER, prevMonthStart, monthStart);

        double growthPercent = 0.0;
        if (messagesPrevMonth > 0) {
            growthPercent = ((double)(messagesThisMonth - messagesPrevMonth) / messagesPrevMonth) * 100.0;
        } else if (messagesThisMonth > 0) {
            growthPercent = 100.0; // 100% growth from zero
        }

        // --- Disease / Vaccine ---
        long totalDiseases = diseaseInfoRepository.count();
        long totalVaccines = vaccineRepository.count();

        // --- Alert stats ---
        long activeAlerts = alertRepository.findByActiveTrue().size();
        long totalAlerts = alertRepository.count();

        Map<String, Long> alertsBySeverity = new LinkedHashMap<>();
        for (OutbreakAlert.SeverityLevel level : OutbreakAlert.SeverityLevel.values()) {
            long count = alertRepository.findBySeverityAndActiveTrue(level).size();
            alertsBySeverity.put(level.name(), count);
        }

        // --- Intent breakdown ---
        List<Map<String, Object>> topIntents = new ArrayList<>();
        try {
            List<Object[]> intentRows = messageRepository.countByIntent(com.healthchatbot.entity.ChatMessage.MessageType.BOT);
            int limit = Math.min(intentRows.size(), 8);
            for (int i = 0; i < limit; i++) {
                Object[] row = intentRows.get(i);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("intent", row[0] != null ? row[0].toString() : "unknown");
                entry.put("count", ((Number) row[1]).longValue());
                topIntents.add(entry);
            }
        } catch (Exception e) {
            log.warn("Could not compute intent breakdown: {}", e.getMessage());
        }

        // --- Language breakdown ---
        Map<String, Long> languageBreakdown = new LinkedHashMap<>();
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            String lang = u.getPreferredLanguage() != null ? u.getPreferredLanguage() : "en";
            languageBreakdown.merge(lang, 1L, Long::sum);
        }

        // --- District chart (top districts by user region) ---
        Map<String, Long> districtCount = new LinkedHashMap<>();
        for (User u : allUsers) {
            if (u.getDistrict() != null && !u.getDistrict().isBlank()) {
                districtCount.merge(u.getDistrict().trim(), 1L, Long::sum);
            }
        }
        List<Map<String, Object>> topDistricts = districtCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("district", e.getKey());
                    m.put("users", e.getValue());
                    return m;
                })
                .toList();

        return AnalyticsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalMessages(totalMessages)
                .totalSessions(totalSessions)
                .totalDiseases(totalDiseases)
                .totalVaccines(totalVaccines)
                .activeAlerts(activeAlerts)
                .totalAlerts(totalAlerts)
                .messagesToday(messagesToday)
                .messagesThisWeek(messagesThisWeek)
                .messagesThisMonth(messagesThisMonth)
                .awarenessGrowthPercent(Math.round(growthPercent * 100.0) / 100.0)
                .topIntents(topIntents)
                .topDistricts(topDistricts)
                .languageBreakdown(languageBreakdown)
                .alertsBySeverity(alertsBySeverity)
                .mlApiOnline(true)
                .twilioReady(notificationService.isTwilioReady())
                .lastScanTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")))
                .build();
    }
}
