package com.jingqu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingqu.config.RagFlowProperties;
import com.jingqu.dto.RagFlowChatRequest;
import com.jingqu.dto.RagFlowChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagFlowService {
    private final RagFlowProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagFlowChatResponse chat(RagFlowChatRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", request.getMessage());
        payload.put("session_id", request.getSessionId());
        payload.put("user_id", request.getVisitorId());
        payload.put("stream", false);
        payload.put("datasets", request.getDatasetIds() != null ? request.getDatasetIds() : properties.getDatasets());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String url = normalizeBaseUrl(properties.getBaseUrl()) + properties.getCompletionPath();

        log.info("调用 RagFlow: url={}, payload={}", url, payload);
        String raw = restTemplate.postForObject(url, entity, String.class);
        log.info("RagFlow 原始响应: {}", raw);
        return parseResponse(raw, request);
    }

    private RagFlowChatResponse parseResponse(String raw, RagFlowChatRequest request) {
        RagFlowChatResponse response = new RagFlowChatResponse();
        response.setVisitorId(request.getVisitorId());
        response.setSessionId(request.getSessionId());
        response.setScenicSpot(request.getScenicSpot());
        response.setSource("ragflow");
        response.setTimestamp(LocalDateTime.now());
        response.setAvatarTarget("admin");
        response.setEmotion("neutral");
        response.setAction(properties.getDefaultAvatarAction());

        if (raw == null || raw.isBlank()) {
            response.setAnswer("暂时没有获取到回答，请稍后重试。");
            return response;
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            log.info("RagFlow 解析后的 JSON: {}", root.toPrettyString());
            String answer = findText(root, "answer", "data", "text", "content", "response", "output");
            if (answer == null) answer = root.path("data").path("answer").asText(null);
            if (answer == null) answer = root.path("data").path("output").asText(null);
            if (answer == null) answer = raw;
            response.setAnswer(answer);
            String emotion = findText(root, "emotion");
            if (emotion != null) response.setEmotion(emotion);
            String action = findText(root, "action");
            if (action != null) response.setAction(action);
            JsonNode citations = root.path("citations");
            if (citations.isArray()) {
                List<String> items = new ArrayList<>();
                for (JsonNode node : citations) items.add(node.asText());
                response.setCitations(items);
            }
            return response;
        } catch (Exception e) {
            log.warn("解析 RAGFlow 返回失败，使用原始文本", e);
            response.setAnswer(raw);
            return response;
        }
    }

    private String findText(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.path(key);
            if (!node.isMissingNode() && !node.isNull()) {
                if (node.isTextual()) return node.asText();
                if (node.isObject()) {
                    String val = node.path("text").asText(null);
                    if (val == null) val = node.path("content").asText(null);
                    if (val != null) return val;
                }
            }
        }
        return null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
