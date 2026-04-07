package com.healthchatbot.controller;

import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.service.AlertService;
import com.healthchatbot.service.NotificationService;
import com.healthchatbot.service.OutbreakMonitorService;
import com.healthchatbot.service.OutbreakSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final NotificationService notificationService;
    private final OutbreakMonitorService outbreakMonitorService;
    private final OutbreakSchedulerService outbreakSchedulerService;

    // ─── Standard CRUD Endpoints ─────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<OutbreakAlert>> getAllActiveAlerts() {
        return ResponseEntity.ok(alertService.getAllActiveAlerts());
    }

    @GetMapping("/all")
    public ResponseEntity<List<OutbreakAlert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutbreakAlert> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/region/{region}")
    public ResponseEntity<List<OutbreakAlert>> getAlertsByRegion(@PathVariable String region) {
        return ResponseEntity.ok(alertService.getAlertsByRegion(region));
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<OutbreakAlert>> getAlertsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(alertService.getAlertsBySeverity(severity));
    }

    @PostMapping
    public ResponseEntity<OutbreakAlert> createAlert(@RequestBody OutbreakAlert alert) {
        return ResponseEntity.ok(alertService.saveAlert(alert));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OutbreakAlert> updateAlert(@PathVariable Long id, @RequestBody OutbreakAlert alert) {
        return ResponseEntity.ok(alertService.updateAlert(id, alert));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Twilio Test Endpoint ────────────────────────────────────────────────

    /**
     * POST /api/alerts/test-notify?phone=9876543210
     * Sends a test SMS + WhatsApp to verify Twilio credentials.
     * Requires ADMIN role.
     */
    @PostMapping("/test-notify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testNotify(
            @RequestParam String phone,
            @RequestParam(required = false) String message) {

        String finalMsg = (message != null && !message.trim().isEmpty())
                ? message
                : "ArogyaBot Test Message\nTwilio SMS & WhatsApp are working correctly!\nIgnore this message.";

        boolean smsOk = notificationService.sendSms(phone, finalMsg);
        boolean waOk = notificationService.sendWhatsApp(phone, finalMsg);
        return ResponseEntity.ok(Map.of(
                "phone", phone,
                "smsSent", smsOk,
                "whatsAppSent", waOk,
                "twilioReady", notificationService.isTwilioReady()));
    }

    // ─── Feature #3: Admin Endpoints for Outbreak Surveillance ───────────────

    /**
     * POST /api/alerts/admin/run-scan
     * Manually triggers the daily district surveillance scan.
     * Useful for demo/testing — normally scheduled to run at 8 AM IST daily.
     * Requires ADMIN role.
     */
    @PostMapping("/admin/run-scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> manualSurveillanceScan() {
        long before = alertService.getAllAlerts().size();
        outbreakSchedulerService.dailyDistrictSurveillanceScan();
        long after = alertService.getAllAlerts().size();
        return ResponseEntity.ok(Map.of(
            "message", "District surveillance scan completed successfully.",
            "newAlertsCreated", after - before,
            "totalActiveAlerts", alertService.getAllActiveAlerts().size()
        ));
    }

    /**
     * POST /api/alerts/admin/run-weekly-bulletin
     * Manually triggers the weekly health bulletin to all registered users.
     * Requires ADMIN role.
     */
    @PostMapping("/admin/run-weekly-bulletin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> manualWeeklyBulletin() {
        outbreakSchedulerService.weeklySurveillanceSummary();
        return ResponseEntity.ok(Map.of(
            "message", "Weekly health bulletin dispatched to all registered users via SMS/WhatsApp."
        ));
    }

    // ─── Feature #2: Government Health Data / Surveillance Endpoints ──────────

    /**
     * GET /api/alerts/surveillance/{district}
     * Returns simulated IDSP surveillance data for a specific district.
     * Shows weekly case counts and active alerts for that district.
     */
    @GetMapping("/surveillance/{district}")
    public ResponseEntity<Map<String, Object>> getSurveillanceData(@PathVariable String district) {
        Map<String, Integer> data = outbreakMonitorService.getSurveillanceData(district);
        List<OutbreakAlert> activeAlerts = alertService.getAlertsByRegion("Odisha").stream()
            .filter(a -> a.getDistrict().equalsIgnoreCase(district) && a.isActive())
            .toList();

        return ResponseEntity.ok(Map.of(
            "district", district,
            "region", "Odisha",
            "weeklyDiseaseCounts", data,
            "activeAlerts", activeAlerts.size(),
            "dataSource", "IDSP Odisha / State Disease Monitoring Unit (simulated)",
            "note", "In production, this data is fetched from the IDSP API and updated weekly."
        ));
    }

    /**
     * GET /api/alerts/districts
     * Returns list of all 30 Odisha districts tracked by the surveillance system.
     */
    @GetMapping("/districts")
    public ResponseEntity<Map<String, Object>> getOdishaDistricts() {
        return ResponseEntity.ok(Map.of(
            "state", "Odisha",
            "totalDistricts", OutbreakMonitorService.ODISHA_DISTRICTS.size(),
            "districts", OutbreakMonitorService.ODISHA_DISTRICTS,
            "monitoredDiseases", List.of("Malaria", "Dengue", "Diarrhoea", "Cholera", "Typhoid",
                "Japanese Encephalitis", "Tuberculosis", "COVID-19")
        ));
    }
}
