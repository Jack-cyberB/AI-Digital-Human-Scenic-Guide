package com.jingqu.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RagFlowChatResponse {
    private String answer;
    private String emotion;
    private String action;
    private String avatarTarget;
    private String sessionId;
    private String visitorId;
    private String scenicSpot;
    private List<String> citations;
    private String source;
    private LocalDateTime timestamp;
    private String dailyRoutes;  // 路线数据JSON
    private String mode;         // "city" 或 "scenic"
}
