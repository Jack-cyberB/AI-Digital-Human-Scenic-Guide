package com.jingqu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagFlowHistoryItemDTO {
    private String visitorId;
    private String sessionId;
    private String question;
    private String answer;
    private String action;
    private String emotion;
    private String scenicSpot;
    private LocalDateTime timestamp;
}
