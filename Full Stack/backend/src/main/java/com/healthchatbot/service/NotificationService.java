package com.healthchatbot.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromSmsNumber;

    @Value("${twilio.whatsapp.number}")
    private String fromWhatsAppNumber;

    private boolean twilioReady = false;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isEmpty()
                && !accountSid.equals("YOUR_ACCOUNT_SID")) {
            try {
                Twilio.init(accountSid, authToken);
                twilioReady = true;
                log.info("✅ Twilio initialized. SMS from: {} | WhatsApp from: {}",
                        fromSmsNumber, fromWhatsAppNumber);
            } catch (Exception e) {
                log.error("❌ Twilio initialization failed: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ Twilio not configured — notifications will be logged only.");
        }
    }

    /**
     * Sends an SMS. Phone number must be E.164 format: +919876543210
     */
    public boolean sendSms(String toPhoneNumber, String messageBody) {
        String normalized = normalizePhone(toPhoneNumber);
        if (!twilioReady) {
            log.info("[DRY-RUN] SMS → {} : {}", normalized, messageBody);
            return false;
        }
        try {
            Message msg = Message.creator(
                    new PhoneNumber(normalized),
                    new PhoneNumber(fromSmsNumber),
                    messageBody).create();
            log.info("✅ SMS sent to {} | SID: {}", normalized, msg.getSid());
            return true;
        } catch (ApiException e) {
            log.error("❌ SMS failed to {} | Code: {} | Msg: {}", normalized, e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ SMS error to {}: {}", normalized, e.getMessage());
            return false;
        }
    }

    /**
     * Sends a WhatsApp message via Twilio Sandbox.
     * Phone number must be E.164 format: +919876543210
     *
     * NOTE: For the Twilio Sandbox, the recipient MUST first send
     * "join <sandbox-keyword>" to whatsapp:+14155238886
     */
    public boolean sendWhatsApp(String toPhoneNumber, String messageBody) {
        String normalized = normalizePhone(toPhoneNumber);
        if (!twilioReady) {
            log.info("[DRY-RUN] WhatsApp → {} : {}", normalized, messageBody);
            return false;
        }
        try {
            // Ensure the 'to' number has the whatsapp: prefix
            String toWhatsApp = normalized.startsWith("whatsapp:")
                    ? normalized
                    : "whatsapp:" + normalized;

            // Ensure 'from' number has the whatsapp: prefix
            String fromWhatsApp = fromWhatsAppNumber.startsWith("whatsapp:")
                    ? fromWhatsAppNumber
                    : "whatsapp:" + fromWhatsAppNumber;

            Message msg = Message.creator(
                    new PhoneNumber(toWhatsApp),
                    new PhoneNumber(fromWhatsApp),
                    messageBody).create();
            log.info("✅ WhatsApp sent to {} | SID: {}", normalized, msg.getSid());
            return true;
        } catch (ApiException e) {
            log.error("❌ WhatsApp failed to {} | Code: {} | Msg: {}", normalized, e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ WhatsApp error to {}: {}", normalized, e.getMessage());
            return false;
        }
    }

    /**
     * Sends BOTH SMS and WhatsApp to the same number.
     * Returns true if at least one succeeded.
     */
    public boolean sendBoth(String toPhoneNumber, String messageBody) {
        boolean sms = sendSms(toPhoneNumber, messageBody);
        boolean whatsApp = sendWhatsApp(toPhoneNumber, messageBody);
        return sms || whatsApp;
    }

    /**
     * Ensures phone number is in E.164 format.
     * If it's a 10-digit Indian number without country code, prepends +91.
     */
    private String normalizePhone(String phone) {
        if (phone == null)
            return "";
        phone = phone.trim().replaceAll("[\\s\\-()]", ""); // strip spaces/dashes

        // Already in E.164 (starts with +)
        if (phone.startsWith("+"))
            return phone;

        // Indian 10-digit number
        if (phone.length() == 10 && (phone.startsWith("6") || phone.startsWith("7")
                || phone.startsWith("8") || phone.startsWith("9"))) {
            return "+91" + phone;
        }

        // Number with country code but no + (e.g. 919876543210)
        if (phone.length() == 12 && phone.startsWith("91")) {
            return "+" + phone;
        }

        // Fallback — return as-is with + prefix
        return "+" + phone;
    }

    public boolean isTwilioReady() {
        return twilioReady;
    }
}
