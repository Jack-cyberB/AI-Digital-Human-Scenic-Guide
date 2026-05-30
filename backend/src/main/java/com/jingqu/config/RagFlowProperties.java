package com.jingqu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "ragflow")
public class RagFlowProperties {
    /**
     * Example: http://localhost:8080/v1
     */
    private String baseUrl;

    /**
     * Bearer token without the "Bearer " prefix
     */
    private String apiKey;

    /**
     * Example: /completion
     */
    private String completionPath = "/completion";

    /**
     * Dataset URLs or IDs, passed through to upstream as metadata
     */
    private List<String> datasets;

    /**
     * Default avatar action when upstream does not provide one
     */
    private String defaultAvatarAction = "根据回答内容自动匹配";
}
