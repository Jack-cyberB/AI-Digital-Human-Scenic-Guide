package com.jingqu.dto;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class RagDocument {
    private String datasetName;
    private String documentName;
    private String content;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
