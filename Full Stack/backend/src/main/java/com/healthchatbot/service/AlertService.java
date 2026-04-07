package com.healthchatbot.service;

import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.entity.User;
import com.healthchatbot.repository.OutbreakAlertRepository;
import com.healthchatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final OutbreakAlertRepository alertRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public List<OutbreakAlert> getAllActiveAlerts() {
        return alertRepository.findByActiveTrue();
    }

    public List<OutbreakAlert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public OutbreakAlert getAlertById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
    }

    public List<OutbreakAlert> getAlertsByRegion(String region) {
        return alertRepository.findByRegionIgnoreCaseAndActiveTrue(region);
    }

    public List<OutbreakAlert> getAlertsBySeverity(String severity) {
        OutbreakAlert.SeverityLevel level = OutbreakAlert.SeverityLevel.valueOf(severity.toUpperCase());
        return alertRepository.findBySeverityAndActiveTrue(level);
    }

    public OutbreakAlert saveAlert(OutbreakAlert alert) {
        OutbreakAlert savedAlert = alertRepository.save(alert);

        // Send SMS + WhatsApp notifications for HIGH severity alerts
        if (alert.getSeverity() != null && "HIGH".equalsIgnoreCase(alert.getSeverity().name())) {
            String message = "🚨 OUTBREAK ALERT: " + alert.getTitle()
                    + "\nDisease: " + alert.getDisease()
                    + "\nRegion: " + alert.getRegion()
                    + "\nPrecautions: " + alert.getPrecautions()
                    + "\nFor help call: " + (alert.getContactNumber() != null ? alert.getContactNumber() : "104");

            List<User> usersToNotify = userRepository.findByRoleAndPhoneIsNotNull(User.Role.USER);
            int notified = 0;
            for (User user : usersToNotify) {
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    boolean sent = notificationService.sendBoth(user.getPhone(), message);
                    if (sent)
                        notified++;
                }
            }
            System.out.println("Alert notifications sent to " + notified + "/" + usersToNotify.size() + " users.");
        }

        return savedAlert;
    }

    public OutbreakAlert updateAlert(Long id, OutbreakAlert details) {
        OutbreakAlert alert = getAlertById(id);
        alert.setTitle(details.getTitle());
        alert.setDescription(details.getDescription());
        alert.setDisease(details.getDisease());
        alert.setRegion(details.getRegion());
        alert.setDistrict(details.getDistrict());
        alert.setSeverity(details.getSeverity());
        alert.setReportedCases(details.getReportedCases());
        alert.setActive(details.isActive());
        alert.setPrecautions(details.getPrecautions());
        alert.setContactNumber(details.getContactNumber());
        alert.setTitleHi(details.getTitleHi());
        alert.setTitleOr(details.getTitleOr());
        alert.setDescriptionHi(details.getDescriptionHi());
        alert.setDescriptionOr(details.getDescriptionOr());
        return alertRepository.save(alert);
    }

    public void deleteAlert(Long id) {
        alertRepository.deleteById(id);
    }
}
