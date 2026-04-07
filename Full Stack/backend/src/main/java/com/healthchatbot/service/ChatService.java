package com.healthchatbot.service;

import com.healthchatbot.dto.ChatRequest;
import com.healthchatbot.dto.ChatResponse;
import com.healthchatbot.entity.ChatMessage;
import com.healthchatbot.entity.ChatSession;
import com.healthchatbot.entity.User;
import com.healthchatbot.repository.ChatMessageRepository;
import com.healthchatbot.repository.ChatSessionRepository;
import com.healthchatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

        private final ChatSessionRepository sessionRepository;
        private final ChatMessageRepository messageRepository;
        private final UserRepository userRepository;
        private final NLPService nlpService;
        private final MLApiService mlApiService;
        private final NotificationService notificationService;

        @org.springframework.beans.factory.annotation.Value("${admin.emergency.phone:+919999999999}")
        private String adminEmergencyPhone;

        @Transactional
        public ChatResponse processMessage(ChatRequest request, String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String lang = request.getLanguage() != null
                                ? request.getLanguage()
                                : user.getPreferredLanguage();

                // Get or create session
                ChatSession session;
                if (request.getSessionId() != null) {
                        session = sessionRepository.findByIdAndUser(request.getSessionId(), user)
                                        .orElseGet(() -> createNewSession(user, lang));
                } else {
                        session = createNewSession(user, lang);
                }

                // Save user message
                ChatMessage userMsg = ChatMessage.builder()
                                .session(session)
                                .content(request.getMessage())
                                .type(ChatMessage.MessageType.USER)
                                .build();
                messageRepository.save(userMsg);

                // ── Step 1: Integrated DB-first & ML/NLP Lookup ──
                String botResponse;
                String intent = "general_query";
                double confidence = 0.5;
                List<String> suggestions = null;
                String detectedLang = lang;

                // Pre-calculate local intent to protect explicit intents from greedy DB matching
                String localIntent = nlpService.detectIntent(request.getMessage());
                boolean isExplicitIntent = List.of("vaccine", "greeting", "bye", "emergency", "child_health", "outbreak_alert").contains(localIntent);

                // Priority 1: Check Database directly (unless it's an explicit intent)
                String dbResponse = null;
                if (!isExplicitIntent) {
                    dbResponse = nlpService.resolveFromDatabase(request.getMessage(), "disease_symptoms", lang);
                }
                
                if (dbResponse != null) {
                        log.info("DB-driven response served for: '{}'", request.getMessage());
                        botResponse = dbResponse;
                        intent = "disease_info";
                        confidence = 0.95;
                        suggestions = nlpService.getSuggestions(intent, lang);
                } else {
                        // Priority 2: ML API (skip if we confidently detected a local explicit intent like vaccine)
                        MLApiService.ChatResult mlResult = null;
                        if (!isExplicitIntent) {
                                mlResult = mlApiService.chat(request.getMessage(), lang);
                        }
                        
                        if (mlResult != null) {
                                botResponse = mlResult.response();
                                intent = mlResult.intent();
                                confidence = mlResult.confidence();
                                suggestions = mlResult.suggestions();
                                detectedLang = mlResult.language();
                        } else {
                                // Priority 3: NLP Fallback
                                log.warn("Falling back to local NLP explicit handlers for: '{}'", request.getMessage());
                                intent = isExplicitIntent ? localIntent : nlpService.detectIntent(request.getMessage());
                                botResponse = nlpService.generateResponse(request.getMessage(), intent, lang);
                                confidence = isExplicitIntent ? 0.95 : 0.6;
                                suggestions = nlpService.getSuggestions(intent, lang);
                        }
                }

                // Final safety check to ensure botResponse is never null (prevents DB constraint 400s)
                if (botResponse == null) {
                        botResponse = "I'm not sure how to respond to that. Could you please rephrase?";
                }
                if (suggestions == null) {
                        suggestions = nlpService.getSuggestions(intent, lang);
                }

                // Save bot message
                ChatMessage botMsg = ChatMessage.builder()
                                .session(session)
                                .content(botResponse)
                                .type(ChatMessage.MessageType.BOT)
                                .intent(intent)
                                .confidenceScore(confidence)
                                .build();
                messageRepository.save(botMsg);

                // 🚨 Emergency SMS Escalation
                if ("emergency".equals(intent)) {
                    log.warn("🚨 EMERGENCY INTENT DETECTED! Firing SMS Escalation...");
                    String district = user.getDistrict() != null ? user.getDistrict() : "Unknown District";
                    String userPhone = user.getPhone() != null ? user.getPhone() : "N/A";
                    String alertText = String.format("🚨 AROGYABOT EMERGENCY: User %s (Phone: %s) in %s requested urgent help! Message: '%s'",
                            username, userPhone, district, request.getMessage());
                    
                    try {
                        boolean sent = notificationService.sendSms(adminEmergencyPhone, alertText);
                        if (sent) {
                            log.info("✅ Emergency SMS successfully dispatched to Admin: {}", adminEmergencyPhone);
                        } else {
                            log.warn("⚠️ Emergency SMS dispatch logged (Twilio not configured or sandbox mode restricted).");
                        }
                    } catch (Exception e) {
                        log.error("❌ Failed to dispatch emergency SMS: {}", e.getMessage());
                    }
                }

                return ChatResponse.builder()
                                .sessionId(session.getId())
                                .userMessage(request.getMessage())
                                .botResponse(botResponse)
                                .intent(intent)
                                .confidenceScore(confidence)
                                .timestamp(LocalDateTime.now())
                                .suggestions(suggestions)
                                .language(detectedLang)
                                .build();
        }

        @Transactional
        public ChatResponse processImage(MultipartFile file, Long sessionId, String username, String language) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String lang = language != null ? language : user.getPreferredLanguage();

                // Get or create session
                ChatSession session;
                if (sessionId != null) {
                        session = sessionRepository.findByIdAndUser(sessionId, user)
                                        .orElseGet(() -> createNewSession(user, lang));
                } else {
                        session = createNewSession(user, lang);
                }

                // Save user message
                ChatMessage userMsg = ChatMessage.builder()
                                .session(session)
                                .content("[Attached Image: " + file.getOriginalFilename() + "]")
                                .type(ChatMessage.MessageType.USER)
                                .build();
                messageRepository.save(userMsg);

                MLApiService.ImagePredictionResult mlResult = mlApiService.predictImage(file);

                String botResponse;
                String intent = "image_prediction";
                double confidence = 0.0;

                if (mlResult != null) {
                        confidence = mlResult.confidence();
                        botResponse = String.format("Based on the image analysis, my estimate is **%s** (Confidence: %.0f%%).\n\n" +
                                        "🛡️ **Prevention:** %s\n" +
                                        "💊 **Treatment:** %s\n" +
                                        "📞 **Helpline:** %s\n\n" +
                                        "⚠️ *Disclaimer: This is an AI prediction and not professional medical advice. Please consult a doctor.*",
                                        mlResult.disease(),
                                        confidence * 100,
                                        mlResult.prevention(),
                                        mlResult.treatment(),
                                        mlResult.helpline());
                } else {
                        botResponse = "Sorry, I could not process the image at this moment. Please ensure the ML service is running and try again.";
                }

                // Save bot message
                ChatMessage botMsg = ChatMessage.builder()
                                .session(session)
                                .content(botResponse)
                                .type(ChatMessage.MessageType.BOT)
                                .intent(intent)
                                .confidenceScore(confidence)
                                .build();
                messageRepository.save(botMsg);

                return ChatResponse.builder()
                                .sessionId(session.getId())
                                .userMessage("[Image Uploaded]")
                                .botResponse(botResponse)
                                .intent(intent)
                                .confidenceScore(confidence)
                                .timestamp(LocalDateTime.now())
                                .language(lang)
                                .build();
        }

        private ChatSession createNewSession(User user, String lang) {
                ChatSession session = ChatSession.builder()
                                .user(user)
                                .language(lang)
                                .build();
                return sessionRepository.save(session);
        }

        public List<ChatMessage> getChatHistory(Long sessionId, String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                sessionRepository.findByIdAndUser(sessionId, user)
                                .orElseThrow(() -> new RuntimeException("Session not found"));
                return messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
        }

        public List<ChatSession> getUserSessions(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                return sessionRepository.findByUserOrderByStartedAtDesc(user);
        }
}
