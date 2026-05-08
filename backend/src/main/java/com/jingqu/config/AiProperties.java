package com.jingqu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enableRag = true;

    private boolean enableFallback = true;

    private int timeoutMs = 15000;

    private Ragflow ragflow = new Ragflow();

    private Deepseek deepseek = new Deepseek();

    @Data
    public static class Ragflow {
        private String baseUrl = "http://localhost:9390";
        private String apiKey;
        private Dataset dataset = new Dataset();
        private Chat chat = new Chat();
    }

    @Data
    public static class Dataset {
        private String spotStructured = "scenic-spot-structured";
        private String historyRoute = "history-culture-route";
    }

    @Data
    public static class Chat {
        private String spotStructuredId;
        private String historyRouteId;
    }

    @Data
    public static class Deepseek {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey;
        private String model = "deepseek-chat";
    }
}
