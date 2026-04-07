package com.healthchatbot.service;

import com.healthchatbot.dto.BroadcastRequest;
import com.healthchatbot.entity.User;
import com.healthchatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Broadcast a health tip or announcement to all users via SMS/WhatsApp.
     * Runs asynchronously so the HTTP response returns immediately.
     */
    @Async
    public Map<String, Object> broadcast(BroadcastRequest req) {
        List<User> targets = switch (req.getTargetAudience() != null ? req.getTargetAudience() : "ALL") {
            case "USERS"          -> userRepository.findByRoleAndPhoneIsNotNull(User.Role.USER);
            case "HEALTH_WORKERS" -> userRepository.findByRoleAndPhoneIsNotNull(User.Role.HEALTH_WORKER);
            default               -> userRepository.findAll().stream()
                    .filter(u -> u.getPhone() != null && !u.getPhone().isBlank() && u.isActive())
                    .toList();
        };

        if (req.getDistrict() != null && !req.getDistrict().isBlank()) {
            targets = targets.stream()
                    .filter(u -> req.getDistrict().equalsIgnoreCase(u.getDistrict()))
                    .toList();
        }

        AtomicInteger sent = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (User user : targets) {
            if (user.getPhone() == null || user.getPhone().isBlank()) continue;

            String message = resolveMessage(req, user.getPreferredLanguage());
            boolean ok = switch (req.getChannel() != null ? req.getChannel() : "BOTH") {
                case "SMS"      -> notificationService.sendSms(user.getPhone(), message);
                case "WHATSAPP" -> notificationService.sendWhatsApp(user.getPhone(), message);
                default         -> notificationService.sendBoth(user.getPhone(), message);
            };

            if (ok) sent.incrementAndGet(); else failed.incrementAndGet();
        }

        log.info("Broadcast '{}': sent={}, failed={}, total={}", req.getTitle(), sent.get(), failed.get(), targets.size());
        return Map.of(
            "totalTargeted", targets.size(),
            "sent", sent.get(),
            "failed", failed.get(),
            "twilioReady", notificationService.isTwilioReady()
        );
    }

    private String resolveMessage(BroadcastRequest req, String lang) {
        String title = req.getTitle() != null ? req.getTitle() + "\n\n" : "";
        return switch (lang != null ? lang : "en") {
            case "hi" -> title + (req.getMessageHi() != null && !req.getMessageHi().isBlank()
                    ? req.getMessageHi() : req.getMessageEn());
            case "or" -> title + (req.getMessageOr() != null && !req.getMessageOr().isBlank()
                    ? req.getMessageOr() : req.getMessageEn());
            default   -> title + (req.getMessageEn() != null ? req.getMessageEn() : "");
        };
    }

    // ─── Vaccination Reminder Scheduler ────────────────────────────────────────
    // Every Sunday at 10 AM IST (4:30 AM UTC) — weekly vaccine reminder
    @Scheduled(cron = "0 30 4 * * SUN")
    public void weeklyVaccinationReminder() {
        log.info("💉 VACCINATION REMINDER — {}", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MMM-yyyy")));

        BroadcastRequest req = new BroadcastRequest();
        req.setTitle("💉 ArogyaBot — Vaccination Reminder");
        req.setMessageEn(
            "💉 *WEEKLY VACCINATION REMINDER*\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "📌 Is your child's vaccination up to date?\n\n" +
            "🗓️ *Key Vaccines (FREE at PHC):*\n" +
            "• At Birth: BCG, OPV-0, Hepatitis B\n" +
            "• 6 Weeks: DPT-1, IPV-1, Rotavirus\n" +
            "• 9 Months: MR Vaccine\n" +
            "• 16-24 Months: Booster doses\n\n" +
            "🏥 Visit your nearest *Anganwadi or PHC*\n" +
            "📞 Call *104* for vaccination info\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "ArogyaBot | Odisha NHM"
        );
        req.setMessageHi(
            "💉 *साप्ताहिक टीकाकरण अनुस्मारक*\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "📌 क्या आपके बच्चे का टीकाकरण पूरा है?\n\n" +
            "🗓️ *मुख्य टीके (PHC में मुफ्त):*\n" +
            "• जन्म पर: BCG, OPV-0, हेपेटाइटिस बी\n" +
            "• 6 सप्ताह: DPT-1, IPV-1, रोटावायरस\n" +
            "• 9 माह: MR टीका\n" +
            "• 16-24 माह: बूस्टर खुराक\n\n" +
            "🏥 नजदीकी *आंगनवाड़ी या PHC* जाएं\n" +
            "📞 *104* पर कॉल करें\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "अरोग्यबॉट | ओडिशा NHM"
        );
        req.setMessageOr(
            "💉 *ସାପ୍ତାହିକ ଟୀକାକରଣ ସ୍ମାରକ*\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "📌 ଆପଣଙ୍କ ଶିଶୁର ଟୀକାକରଣ ସମ୍ପୂର୍ଣ ହୋଇଛି କି?\n\n" +
            "🗓️ *ମୁଖ୍ୟ ଟୀକା (PHCରେ ମାଗଣା):*\n" +
            "• ଜନ୍ମ ସମୟ: BCG, OPV-0, Hepatitis B\n" +
            "• 6 ସପ୍ତାହ: DPT-1, IPV-1, Rotavirus\n" +
            "• 9 ମାସ: MR ଟୀକା\n" +
            "• 16-24 ମାସ: Booster dose\n\n" +
            "🏥 ନିକଟ *ଅଙ୍ଗନୱାଡ଼ି ବା PHC* ଯାଆନ୍ତୁ\n" +
            "📞 *104* କୁ ଫୋନ୍ କରନ୍ତୁ\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "ଆରୋଗ୍ୟବଟ | ଓଡ଼ିଶା NHM"
        );
        req.setTargetAudience("USERS");
        req.setChannel("BOTH");

        broadcast(req);
    }

    // Monthly health tips — 1st of every month at 10 AM IST (4:30 AM UTC)
    @Scheduled(cron = "0 30 4 1 * *")
    public void monthlyHealthTips() {
        log.info("🌿 MONTHLY HEALTH TIPS BROADCAST — {}", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMM-yyyy")));

        String[] tips = {
            "🧼 Wash hands with soap for 20 seconds before eating and after using the toilet.",
            "💧 Always drink boiled or filtered water to prevent waterborne diseases.",
            "🦟 Use mosquito nets while sleeping to prevent Malaria and Dengue.",
            "🥗 Eat a balanced diet with fruits and vegetables to boost immunity.",
            "💉 Keep your vaccination schedule updated — it's FREE at PHCs.",
            "🚭 Avoid smoking and tobacco — it increases risk of TB and cancer.",
            "😴 Get at least 7-8 hours of sleep for a strong immune system.",
            "🏃 30 minutes of daily physical activity prevents diabetes and heart disease."
        };

        int tipIndex = (LocalDateTime.now().getMonthValue() - 1) % tips.length;
        String tip = tips[tipIndex];

        BroadcastRequest req = new BroadcastRequest();
        req.setTitle("🌿 ArogyaBot Monthly Health Tip");
        req.setMessageEn(
            "🌿 *MONTHLY HEALTH TIP*\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            tip + "\n\n" +
            "📞 Health Helpline: *104* (Free, 24x7)\n" +
            "🚑 Ambulance: *108* (Free)\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "ArogyaBot | Odisha Health Dept"
        );
        req.setMessageHi(req.getMessageEn());
        req.setMessageOr(req.getMessageEn());
        req.setTargetAudience("ALL");
        req.setChannel("WHATSAPP");

        broadcast(req);
    }
}
