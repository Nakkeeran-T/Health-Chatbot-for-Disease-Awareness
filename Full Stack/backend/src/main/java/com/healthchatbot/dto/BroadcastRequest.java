package com.healthchatbot.dto;

import lombok.Data;

@Data
public class BroadcastRequest {
    private String title;
    private String messageEn;
    private String messageHi;
    private String messageOr;
    private String targetAudience; // "ALL", "USERS", "HEALTH_WORKERS"
    private String channel;        // "SMS", "WHATSAPP", "BOTH"
    private String district;       // null = all districts
}
