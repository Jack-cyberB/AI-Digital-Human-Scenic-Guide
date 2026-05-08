package com.jingqu.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VisitorMessageResponse {
    private String visitorId;
    private String answer;
    private String sessionId;
    private String messageType;
    private LocalDateTime timestamp;
    private String scenicSpot;
}
