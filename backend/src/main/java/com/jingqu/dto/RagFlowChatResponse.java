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
}
