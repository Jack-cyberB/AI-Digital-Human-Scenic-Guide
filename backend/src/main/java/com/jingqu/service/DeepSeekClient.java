package com.jingqu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingqu.config.AiProperties;
import com.jingqu.dto.RagDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public boolean isConfigured() {
        return hasText(aiProperties.getDeepseek().getApiKey());
    }

    public String answer(String question, List<RagDocument> documents, String scenicSpot) {
        if (!isConfigured() || documents == null || documents.isEmpty()) {
            return null;
        }

        String context = buildContext(documents);
        String userPrompt = buildUserPrompt(question, scenicSpot, context);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", aiProperties.getDeepseek().getModel());
        payload.put("temperature", 0.2);
        payload.put("messages", Arrays.asList(
            message("system", systemPrompt()),
            message("user", userPrompt)
        ));

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers());
            ResponseEntity<String> response = restTemplate.exchange(
                trimTrailingSlash(aiProperties.getDeepseek().getBaseUrl()) + "/chat/completions",
                HttpMethod.POST,
                entity,
                String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String answer = content.asText(null);
            return hasText(answer) ? answer.trim() : null;
        } catch (Exception e) {
            log.warn("DeepSeek completion failed", e);
            return null;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getDeepseek().getApiKey());
        return headers;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> item = new HashMap<>();
        item.put("role", role);
        item.put("content", content);
        return item;
    }

    private String systemPrompt() {
        return "You are a scenic digital human guide for Lingshan Scenic Area. "
            + "Respond in Simplified Chinese. Use only the retrieved facts. "
            + "Do not invent ticket prices, opening hours, realtime conditions, or unsupported details. "
            + "If the knowledge base does not provide enough evidence, explicitly say: 当前知识库未提供该信息。";
    }

    private String buildUserPrompt(String question, String scenicSpot, String context) {
        StringBuilder builder = new StringBuilder();
        builder.append("Visitor question: ").append(question).append("\n");
        builder.append("Current scenic spot: ").append(scenicSpot == null ? "unknown" : scenicSpot).append("\n\n");
        builder.append("Retrieved knowledge:\n").append(context).append("\n\n");
        builder.append("Answer requirements:\n");
        builder.append("1. Answer only about Lingshan Scenic Area.\n");
        builder.append("2. Keep the tone like a scenic digital human guide.\n");
        builder.append("3. If the evidence is insufficient, say 当前知识库未提供该信息。\n");
        builder.append("4. If the question is about realtime status, advise checking onsite notice or realtime services.\n");
        return builder.toString();
    }

    private String buildContext(List<RagDocument> documents) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (RagDocument document : documents) {
            builder.append("[").append(index++).append("] ");
            builder.append(document.getDocumentName()).append(" | ").append(document.getDatasetName()).append("\n");
            if (document.getMetadata() != null && !document.getMetadata().isEmpty()) {
                builder.append("Metadata: ").append(document.getMetadata()).append("\n");
            }
            builder.append(document.getContent()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String trimTrailingSlash(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
