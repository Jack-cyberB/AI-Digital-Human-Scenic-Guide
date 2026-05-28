package com.jingqu.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VisitorMessageRequest {
    private String visitorId;
    private String sessionId;
    private String message;
    private String scenicSpot;
    private LocalDateTime timestamp;
}
