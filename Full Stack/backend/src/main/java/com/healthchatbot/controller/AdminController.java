package com.healthchatbot.controller;

import com.healthchatbot.dto.AnalyticsDTO;
import com.healthchatbot.dto.BroadcastRequest;
import com.healthchatbot.entity.User;
import com.healthchatbot.service.AnalyticsService;
import com.healthchatbot.service.BroadcastService;
import com.healthchatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final BroadcastService broadcastService;

    // ─── User Management ─────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<User> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestParam User.Role role) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    // ─── Analytics Dashboard ──────────────────────────────────────────────────

    /**
     * GET /api/admin/analytics
     * Returns full dashboard analytics: user counts, message volumes,
     * awareness growth %, intent breakdown, language split, alert stats.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsDTO> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getFullAnalytics());
    }

    // ─── Health Tips Broadcasting ─────────────────────────────────────────────

    /**
     * POST /api/admin/broadcast
     * Sends a health tip or announcement to users via SMS/WhatsApp.
     * Body: BroadcastRequest JSON
     */
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> broadcastMessage(@RequestBody BroadcastRequest req) {
        log.info("Admin broadcast request: title='{}', target={}, channel={}",
                req.getTitle(), req.getTargetAudience(), req.getChannel());
        Map<String, Object> result = broadcastService.broadcast(req);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/broadcast/test-vaccine-reminder
     * Manually triggers the weekly vaccination reminder (for demo/testing).
     */
    @PostMapping("/broadcast/test-vaccine-reminder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testVaccineReminder() {
        broadcastService.weeklyVaccinationReminder();
        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "message", "Vaccination reminder broadcast triggered for all users with phone numbers."
        ));
    }

    /**
     * POST /api/admin/broadcast/test-health-tip
     * Manually triggers the monthly health tips broadcast.
     */
    @PostMapping("/broadcast/test-health-tip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> testHealthTip() {
        broadcastService.monthlyHealthTips();
        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "message", "Monthly health tip broadcast triggered for all users."
        ));
    }
}
