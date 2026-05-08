package com.jingqu.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AiAnswerResult {
    private String answer;
    private String routeTarget;
    private int retrievedDocsCount;
    private boolean fallbackUsed;
    private long modelLatencyMs;
    private String finalAnswerSource;
    @Builder.Default
    private List<String> knowledgeSources = new ArrayList<>();
}
