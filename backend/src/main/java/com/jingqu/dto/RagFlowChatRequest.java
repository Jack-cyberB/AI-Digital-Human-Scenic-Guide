package com.jingqu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagFlowChatRequest {
    private String sessionId;
    private String visitorId;
    private String message;
    private String scenicSpot;
    private List<String> datasetIds;
    private boolean stream;
}
