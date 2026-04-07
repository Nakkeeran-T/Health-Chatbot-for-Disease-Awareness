package com.healthchatbot.dto;

import com.healthchatbot.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long sessionId;
    private String userMessage;
    private String botResponse;
    private String intent;
    private Double confidenceScore;
    private LocalDateTime timestamp;
    private List<String> suggestions;
    private String language;
}
