package com.healthchatbot.service;

import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.entity.User;
import com.healthchatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbreak Scheduler Service — Feature #3
 *
 * Implements automated real-time outbreak alerts using Spring's @Scheduled jobs.
 *
 * Scheduled Jobs:
 * 1. dailyDistrictSurveillanceScan  — Runs daily at 8:00 AM IST
 *    Scans all 30 Odisha districts, fetches simulated IDSP surveillance data,
 *    detects disease spikes, creates alerts, and broadcasts via Twilio SMS/WhatsApp
 *    to all registered users in affected districts.
 *
 * 2. weeklySurveillanceSummary      — Runs every Monday at 9:00 AM IST
 *    Sends a weekly health bulletin to all users summarizing active outbreaks
 *    and key prevention messages.
 *
 * 3. highRiskDistrictFocusScan      — Runs every 6 hours for endemic districts
 *    More frequent monitoring for Malaria-endemic districts in tribal areas.
 *
 * Cron format: "seconds minutes hours day-of-month month day-of-week"
 * IST = UTC+5:30 → 8:00 AM IST = 2:30 AM UTC = "0 30 2 * * *"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutbreakSchedulerService {

    private final OutbreakMonitorService outbreakMonitorService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ─── Job 1: Daily District Surveillance Scan ────────────────────────────
    // Runs every day at 8:00 AM IST (2:30 AM UTC)
    @Scheduled(cron = "${app.scheduler.daily-scan-cron:0 30 2 * * *}")
    public void dailyDistrictSurveillanceScan() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("  🔍 DAILY DISEASE SURVEILLANCE SCAN — {}", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")));
        log.info("  Source: IDSP Odisha / State Disease Monitoring Unit");
        log.info("═══════════════════════════════════════════════════════");

        AtomicInteger alertsCreated = new AtomicInteger(0);
        AtomicInteger notificationsSent = new AtomicInteger(0);

        List<String> diseases = List.of("Malaria", "Dengue", "Diarrhoea", "Cholera", "Typhoid");

        for (String district : OutbreakMonitorService.ODISHA_DISTRICTS) {
            try {
                // Fetch simulated surveillance data (replace with real IDSP API call)
                Map<String, Integer> weeklyData = outbreakMonitorService.getSurveillanceData(district);

                for (String disease : diseases) {
                    int cases = weeklyData.getOrDefault(disease, 0);

                    // Check if threshold is exceeded
                    if (outbreakMonitorService.exceedsThreshold(disease, cases)) {

                        // Avoid duplicate alerts for same district+disease
                        if (!outbreakMonitorService.hasActiveAlert(district, disease)) {

                            OutbreakAlert.SeverityLevel severity =
                                outbreakMonitorService.determineSeverity(disease, cases);

                            // Create and save the outbreak alert
                            OutbreakAlert alert = outbreakMonitorService.createSpikeAlert(
                                district, disease, cases, severity);

                            alertsCreated.incrementAndGet();
                            log.warn("🚨 NEW ALERT: {} in {} — {} cases [{}]",
                                disease, district, cases, severity);

                            // Broadcast to users in affected district
                            int sent = broadcastAlertToDistrict(alert, district);
                            notificationsSent.addAndGet(sent);

                            // For CRITICAL alerts, also notify health workers region-wide
                            if (severity == OutbreakAlert.SeverityLevel.CRITICAL ||
                                severity == OutbreakAlert.SeverityLevel.HIGH) {
                                int hwSent = broadcastToHealthWorkers(alert);
                                notificationsSent.addAndGet(hwSent);
                            }
                        } else {
                            log.debug("⏭️ Alert already exists for {} in {}. Skipping.", disease, district);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("❌ Error scanning district {}: {}", district, e.getMessage());
            }
        }

        log.info("═══════════════════════════════════════════════════════");
        log.info("  ✅ Scan Complete: {} new alerts | {} notifications sent",
            alertsCreated.get(), notificationsSent.get());
        log.info("═══════════════════════════════════════════════════════");
    }

    // ─── Job 2: Weekly Health Bulletin ──────────────────────────────────────
    // Runs every Monday at 9:00 AM IST (3:30 AM UTC)
    @Scheduled(cron = "${app.scheduler.weekly-bulletin-cron:0 30 3 * * MON}")
    public void weeklySurveillanceSummary() {
        log.info("📋 WEEKLY HEALTH BULLETIN — {}", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy")));

        String weeklyBulletin = buildWeeklyBulletin();

        List<User> allUsers = userRepository.findByRoleAndPhoneIsNotNull(User.Role.USER);
        int sent = 0;
        for (User user : allUsers) {
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                // Send WhatsApp bulletin (preferred for rural users)
                boolean ok = notificationService.sendWhatsApp(user.getPhone(), weeklyBulletin);
                if (ok) sent++;
            }
        }
        log.info("📢 Weekly bulletin sent to {}/{} users.", sent, allUsers.size());
    }

    // ─── Job 3: High-Risk District Intensive Scan ────────────────────────────
    // Malaria-endemic tribal districts — scanned every 6 hours during monsoon
    @Scheduled(cron = "${app.scheduler.high-risk-cron:0 0 */6 * * *}")
    public void highRiskDistrictFocusScan() {
        // Only run during monsoon season (June - October)
        int month = LocalDateTime.now().getMonthValue();
        if (month < 6 || month > 10) {
            log.debug("⏭️ High-risk scan skipped (non-monsoon month: {})", month);
            return;
        }

        List<String> highRiskDistricts = List.of(
            "Malkangiri", "Koraput", "Gajapati", "Rayagada", "Nabarangpur",
            "Kandhamal", "Kalahandi"
        );

        log.info("🦟 HIGH-RISK DISTRICT SCAN (Malaria/JE focus) — {}",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM HH:mm")));

        for (String district : highRiskDistricts) {
            Map<String, Integer> data = outbreakMonitorService.getSurveillanceData(district);
            int malariaCases = data.getOrDefault("Malaria", 0);

            if (malariaCases >= 15 && !outbreakMonitorService.hasActiveAlert(district, "Malaria")) {
                OutbreakAlert.SeverityLevel severity =
                    outbreakMonitorService.determineSeverity("Malaria", malariaCases);
                OutbreakAlert alert = outbreakMonitorService.createSpikeAlert(
                    district, "Malaria", malariaCases, severity);

                log.warn("🦟 MALARIA SPIKE in {} — {} cases [{}]", district, malariaCases, severity);
                broadcastAlertToDistrict(alert, district);
                broadcastToHealthWorkers(alert);
            }
        }
    }

    // ─── Notification Helpers ────────────────────────────────────────────────

    /**
     * Broadcasts an outbreak alert to all registered users in the affected district.
     * Falls back to region-wide broadcast if no district-specific users found.
     */
    private int broadcastAlertToDistrict(OutbreakAlert alert, String district) {
        // Find users registered in this district
        List<User> usersInDistrict = userRepository.findByDistrictAndPhoneIsNotNull(district);

        // Fallback: if no users have set district, notify all users in region
        if (usersInDistrict.isEmpty()) {
            usersInDistrict = userRepository.findByRoleAndPhoneIsNotNull(User.Role.USER);
        }

        if (usersInDistrict.isEmpty()) {
            log.info("📭 No registered users with phone numbers for district: {}", district);
            return 0;
        }

        String message = buildAlertMessage(alert);
        int sent = 0;

        for (User user : usersInDistrict) {
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                // Send in user's preferred language
                String localizedMessage = buildLocalizedAlertMessage(alert, user.getPreferredLanguage());
                boolean ok = notificationService.sendBoth(user.getPhone(), localizedMessage);
                if (ok) sent++;
            }
        }

        log.info("📢 Alert '{}' broadcast to {}/{} users in {}.",
            alert.getTitle(), sent, usersInDistrict.size(), district);
        return sent;
    }

    /**
     * Notifies HEALTH_WORKER role users for critical/high severity alerts.
     */
    private int broadcastToHealthWorkers(OutbreakAlert alert) {
        List<User> healthWorkers = userRepository.findByRoleAndPhoneIsNotNull(User.Role.HEALTH_WORKER);

        if (healthWorkers.isEmpty()) return 0;

        String hwMessage = buildHealthWorkerMessage(alert);
        int sent = 0;

        for (User hw : healthWorkers) {
            if (hw.getPhone() != null && !hw.getPhone().isBlank()) {
                boolean ok = notificationService.sendBoth(hw.getPhone(), hwMessage);
                if (ok) sent++;
            }
        }

        log.info("🏥 Health worker alert sent to {}/{} workers.", sent, healthWorkers.size());
        return sent;
    }

    // ─── Message Builders ────────────────────────────────────────────────────

    private String buildAlertMessage(OutbreakAlert alert) {
        return String.format(
            "🚨 *AROGYABOT HEALTH ALERT*\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📍 *District:* %s, Odisha\n" +
            "🦠 *Disease:* %s\n" +
            "📊 *Cases this week:* %d\n" +
            "⚠️ *Severity:* %s\n\n" +
            "✅ *What to do:*\n%s\n\n" +
            "📞 *Helpline:* %s | 108 (Ambulance)\n" +
            "🏥 Free treatment at nearest PHC\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "ArogyaBot | Odisha Health Dept",
            alert.getDistrict(),
            alert.getDisease(),
            alert.getReportedCases(),
            alert.getSeverity().name(),
            alert.getPrecautions(),
            alert.getContactNumber() != null ? alert.getContactNumber() : "104"
        );
    }

    private String buildLocalizedAlertMessage(OutbreakAlert alert, String lang) {
        if ("hi".equals(lang)) {
            return String.format(
                "🚨 *अरोग्यबॉट स्वास्थ्य चेतावनी*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📍 *जिला:* %s, ओडिशा\n" +
                "🦠 *बीमारी:* %s\n" +
                "📊 *इस सप्ताह मामले:* %d\n" +
                "⚠️ *गंभीरता:* %s\n\n" +
                "✅ *क्या करें:*\n%s\n\n" +
                "📞 हेल्पलाइन: *%s* | 108 (एम्बुलेंस)\n" +
                "🏥 निकटतम PHC में मुफ्त इलाज\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "अरोग्यबॉट | ओडिशा स्वास्थ्य विभाग",
                alert.getDistrict(),
                alert.getTitleHi() != null ? alert.getDisease() : alert.getDisease(),
                alert.getReportedCases(),
                getSeverityLabelHi(alert.getSeverity()),
                alert.getPrecautions(),
                alert.getContactNumber() != null ? alert.getContactNumber() : "104"
            );
        } else if ("or".equals(lang)) {
            return String.format(
                "🚨 *ଆରୋଗ୍ୟବଟ ସ୍ୱାସ୍ଥ୍ୟ ଚେତାବନୀ*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📍 *ଜିଲ୍ଲା:* %s, ଓଡ଼ିଶା\n" +
                "🦠 *ରୋଗ:* %s\n" +
                "📊 *ଏ ସପ୍ତାହ ଘଟଣା:* %d\n" +
                "⚠️ *ଗୁରୁତ୍ୱ:* %s\n\n" +
                "✅ *କ'ଣ କରିବେ:*\n%s\n\n" +
                "📞 ସାହାୟ୍ୟ ନମ୍ୱର: *%s* | 108 (ଆମ୍ବୁଲାନ୍ସ)\n" +
                "🏥 ନିକଟ PHCରେ ମାଗଣା ଚିକିତ୍ସା\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "ଆରୋଗ୍ୟବଟ | ଓଡ଼ିଶା ସ୍ୱାସ୍ଥ୍ୟ ବିଭାଗ",
                alert.getDistrict(),
                alert.getDisease(),
                alert.getReportedCases(),
                getSeverityLabelOr(alert.getSeverity()),
                alert.getPrecautions(),
                alert.getContactNumber() != null ? alert.getContactNumber() : "104"
            );
        }
        // Default: English
        return buildAlertMessage(alert);
    }

    private String buildHealthWorkerMessage(OutbreakAlert alert) {
        return String.format(
            "🏥 *[HEALTH WORKER ALERT] ArogyaBot SDMU*\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "⚠️ *ACTION REQUIRED* — %s severity\n" +
            "📍 District: %s | Disease: %s\n" +
            "📊 Cases reported this week: %d\n\n" +
            "ACTION CHECKLIST:\n" +
            "□ Alert local PHC Medical Officer\n" +
            "□ Ensure drug stock (antimalarials / ORS / antibiotics)\n" +
            "□ Conduct house-to-house survey in affected blocks\n" +
            "□ Activate Rapid Response Team if CRITICAL\n" +
            "□ Report to District Surveillance Officer\n\n" +
            "📞 SDMU Odisha: 0674-2390640\n" +
            "📞 State EOC: 104\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Reported: %s",
            alert.getSeverity().name(),
            alert.getDistrict(),
            alert.getDisease(),
            alert.getReportedCases(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
        );
    }

    private String buildWeeklyBulletin() {
        return String.format(
            "📋 *AROGYABOT WEEKLY HEALTH BULLETIN*\n" +
            "Week of %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "🦟 *MALARIA*: Monsoon peak. Sleep under nets.\n" +
            "🦟 *DENGUE*: Empty water containers weekly.\n" +
            "💧 *DIARRHOEA*: Boil water before drinking.\n" +
            "💉 *VACCINATION*: Free at all PHCs.\n\n" +
            "🏥 *FREE SERVICES NEAR YOU:*\n" +
            "• Malaria test & treatment: PHC\n" +
            "• Vaccination: PHC, Anganwadi\n" +
            "• ORS packets: ASHA Worker\n\n" +
            "📞 *HELPLINES:*\n" +
            "• Health: 104 (24x7 Free)\n" +
            "• Ambulance: 108 (Free)\n" +
            "• COVID: 1075\n" +
            "• TB (Nikshay): 1800-116-666\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "ArogyaBot | Odisha NHM | nhm.odisha.gov.in",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
        );
    }

    private String getSeverityLabelHi(OutbreakAlert.SeverityLevel severity) {
        return switch (severity) {
            case LOW -> "कम";
            case MEDIUM -> "मध्यम";
            case HIGH -> "उच्च";
            case CRITICAL -> "अत्यंत गंभीर";
        };
    }

    private String getSeverityLabelOr(OutbreakAlert.SeverityLevel severity) {
        return switch (severity) {
            case LOW -> "ନ˙ˣ";
            case MEDIUM -> "ˣ˙ˣˣ˙ˣ";
            case HIGH -> "ˣ˙ˣˣˣ";
            case CRITICAL -> "ˣ˙ˣˣˣ˙ˣˣ";
        };
    }
}
