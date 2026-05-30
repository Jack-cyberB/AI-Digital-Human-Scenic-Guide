package com.jingqu.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AvatarStatusDTO {
    private String visitorId;
    private String sessionId;
    private String scenicSpot;
    private String emotion;
    private String action;
    private String lastQuestion;
    private String lastAnswer;
    private String currentSpot;
    private List<String> datasets;
    private LocalDateTime timestamp;
}
