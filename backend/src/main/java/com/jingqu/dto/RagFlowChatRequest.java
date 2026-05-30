package com.jingqu.dto;

import lombok.Data;

import java.util.List;

@Data
public class RagFlowChatRequest {
    private String sessionId;
    private String visitorId;
    private String message;
    private String scenicSpot;
    private List<String> datasetIds;
    private boolean stream;
}
