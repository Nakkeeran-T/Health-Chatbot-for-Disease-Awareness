package com.healthchatbot.controller;

import com.healthchatbot.dto.ChatRequest;
import com.healthchatbot.dto.ChatResponse;
import com.healthchatbot.entity.ChatMessage;
import com.healthchatbot.entity.ChatSession;
import com.healthchatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized access."));
            }
            ChatResponse response = chatService.processMessage(request, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chat API error: {} - StackTrace: ", e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "language", required = false) String language,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized access."));
            }
            ChatResponse response = chatService.processImage(file, sessionId, userDetails.getUsername(), language);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chat API error: {} - StackTrace: ", e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getChatHistory(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized access."));
            }
            List<ChatMessage> history = chatService.getChatHistory(sessionId, userDetails.getUsername());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error fetching history.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized access."));
            }
            List<ChatSession> sessions = chatService.getUserSessions(userDetails.getUsername());
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error fetching sessions.";
            return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
        }
    }
}
