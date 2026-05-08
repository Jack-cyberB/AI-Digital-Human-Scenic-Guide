package com.jingqu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingqu.config.AiProperties;
import com.jingqu.dto.RagDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagflowClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public boolean isConfigured() {
        return hasText(aiProperties.getRagflow().getApiKey());
    }

    public Map<String, String> resolveDatasets() {
        Map<String, String> mapping = new HashMap<>();
        if (!isConfigured()) {
            return mapping;
        }

        JsonNode data = get("/api/v1/datasets?page=1&page_size=100&name=");
        if (data == null || !data.isArray()) {
            return mapping;
        }

        String spotName = aiProperties.getRagflow().getDataset().getSpotStructured();
        String historyName = aiProperties.getRagflow().getDataset().getHistoryRoute();
        for (JsonNode item : data) {
            String name = item.path("name").asText();
            String id = item.path("id").asText();
            if (name.equalsIgnoreCase(spotName)) {
                mapping.put("spotStructured", id);
            } else if (name.equalsIgnoreCase(historyName)) {
                mapping.put("historyRoute", id);
            }
        }
        return mapping;
    }

    public List<RagDocument> search(String datasetKey, String question, int topN) {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        String datasetId = resolveDatasetId(datasetKey);
        if (!hasText(datasetId)) {
            log.warn("RAGFlow dataset id missing for datasetKey={}", datasetKey);
            return Collections.emptyList();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("dataset_ids", Collections.singletonList(datasetId));
        body.put("question", question);
        body.put("page", 1);
        body.put("page_size", topN);
        body.put("top_k", Math.max(topN, 10));
        body.put("similarity_threshold", 0.1);
        body.put("vector_similarity_weight", 0.3);
        body.put("highlight", false);

        JsonNode root = post("/api/v1/retrieval", body);
        if (root == null) {
            return Collections.emptyList();
        }

        JsonNode chunks = root.path("chunks");
        if (!chunks.isArray() || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<RagDocument> documents = new ArrayList<>();
        for (JsonNode chunk : chunks) {
            if (documents.size() >= topN) {
                break;
            }
            String content = firstNonBlank(
                chunk.path("content").asText(null),
                chunk.path("content_with_weight").asText(null),
                chunk.path("text").asText(null)
            );
            if (!hasText(content)) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            JsonNode metadataNode = chunk.path("document_metadata");
            if (metadataNode.isObject()) {
                metadataNode.fields().forEachRemaining(entry -> metadata.put(entry.getKey(), entry.getValue().asText()));
            }
            documents.add(RagDocument.builder()
                .datasetName(datasetKey)
                .documentName(firstNonBlank(chunk.path("document_name").asText(null), chunk.path("document_name_kwd").asText(null), chunk.path("name").asText(null), datasetKey))
                .content(content)
                .metadata(metadata)
                .build());
        }
        return documents;
    }

    private String resolveDatasetId(String datasetKey) {
        Map<String, String> datasets = resolveDatasets();
        return datasets.get(datasetKey);
    }

    private String resolveChatId(String datasetKey) {
        if ("spotStructured".equals(datasetKey)) {
            return aiProperties.getRagflow().getChat().getSpotStructuredId();
        }
        if ("historyRoute".equals(datasetKey)) {
            return aiProperties.getRagflow().getChat().getHistoryRouteId();
        }
        return null;
    }

    private JsonNode get(String path) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers());
            ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.GET, entity, String.class);
            return extractData(response.getBody());
        } catch (Exception e) {
            log.warn("RAGFlow GET failed: {}", path, e);
            return null;
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers());
            ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.POST, entity, String.class);
            return extractData(response.getBody());
        } catch (Exception e) {
            log.warn("RAGFlow POST failed: {}", path, e);
            return null;
        }
    }

    private JsonNode extractData(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        return root.has("data") ? root.get("data") : root;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getRagflow().getApiKey());
        return headers;
    }

    private String url(String path) {
        return trimTrailingSlash(aiProperties.getRagflow().getBaseUrl()) + path;
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

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (hasText(candidate)) {
                return candidate;
            }
        }
        return "";
    }
}
