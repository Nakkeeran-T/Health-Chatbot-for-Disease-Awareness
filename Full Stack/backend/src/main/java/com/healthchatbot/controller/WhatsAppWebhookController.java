package com.healthchatbot.controller;

import com.healthchatbot.service.NLPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WhatsApp / SMS Inbound Webhook Controller
 *
 * Twilio sends a POST request to this endpoint whenever a user sends a
 * WhatsApp or SMS message to the ArogyaBot number.
 *
 * Webhook URL (configure in Twilio console):
 *   https://<your-ngrok-or-domain>/api/webhook/whatsapp
 *
 * For local testing, use ngrok:
 *   ngrok http 8080
 *   → set Twilio sandbox webhook to: https://<ngrok-id>.ngrok.io/api/webhook/whatsapp
 *
 * Twilio sends form-encoded params (not JSON), hence consumes = FORM.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final NLPService nlpService;

    /**
     * POST /api/webhook/whatsapp
     *
     * Twilio sends: From, To, Body, MessageSid, etc.
     * We respond with TwiML XML to send a reply back.
     */
    @PostMapping(
        value = "/whatsapp",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<String> handleWhatsApp(
            @RequestParam(required = false) String From,
            @RequestParam(required = false) String To,
            @RequestParam(required = false) String Body,
            @RequestParam(required = false) String MessageSid
    ) {
        String from = From != null ? From : "unknown";
        String body = Body != null ? Body.trim() : "";

        log.info("📲 WhatsApp/SMS inbound | From: {} | Message: '{}'", from, body);

        if (body.isEmpty()) {
            return ResponseEntity.ok(twiml("No message received. Please type your health question."));
        }

        // Detect language and intent using NLP service
        String lang = detectLang(body);
        String intent = nlpService.detectIntent(body);
        String reply  = nlpService.generateResponse(body, intent, lang);

        // Append helpline footer
        reply = reply + "\n\n📞 Health Helpline: *104* (Free) | 🚑 Ambulance: *108*\n"
                + "_ArogyaBot | Odisha Health Dept_";

        log.info("🤖 WhatsApp reply to {}: intent={}, lang={}", from, intent, lang);
        return ResponseEntity.ok(twiml(reply));
    }

    /**
     * POST /api/webhook/sms
     *
     * Same handler for inbound SMS messages.
     */
    @PostMapping(
        value = "/sms",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<String> handleSms(
            @RequestParam(required = false) String From,
            @RequestParam(required = false) String Body
    ) {
        return handleWhatsApp(From, null, Body, null);
    }

    /**
     * GET /api/webhook/status
     * Returns webhook status for health checks and admin verification.
     */
    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Object>> webhookStatus() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "active",
            "endpoint", "/api/webhook/whatsapp",
            "description", "ArogyaBot WhatsApp/SMS Inbound Webhook",
            "setup", "Configure in Twilio Console → WhatsApp Sandbox → 'When a message comes in'",
            "note", "For local testing, use ngrok: ngrok http 8080"
        ));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Wraps a reply string in Twilio TwiML <Message> format */
    private String twiml(String message) {
        // Escape XML special chars
        String safe = message
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
             + "<Response><Message>" + safe + "</Message></Response>";
    }

    /** Simple language detection for WhatsApp messages (Hindi/Odia/English) */
    private String detectLang(String text) {
        if (text == null) return "en";
        // Detect Hindi (Devanagari Unicode block: U+0900–U+097F)
        if (text.codePoints().anyMatch(c -> c >= 0x0900 && c <= 0x097F)) return "hi";
        // Detect Odia (Oriya Unicode block: U+0B00–U+0B7F)
        if (text.codePoints().anyMatch(c -> c >= 0x0B00 && c <= 0x0B7F)) return "or";
        return "en";
    }
}
