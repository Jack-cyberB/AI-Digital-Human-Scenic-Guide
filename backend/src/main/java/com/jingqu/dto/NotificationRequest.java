package com.jingqu.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationRequest {
    private String title;
    private String content;
    private String notificationType;
    private String targetScope;
    private String targetSpot;
    private LocalDateTime expiryTime;
    private String adminId;
}
