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
        try {
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

            log.info("调用 RagFlow: url={}", url);
            String raw = restTemplate.postForObject(url, entity, String.class);
            log.info("RagFlow 原始响应: {}", raw);
            if (raw != null && !raw.isBlank()) {
                RagFlowChatResponse resp = parseResponse(raw, request);
                extractRoutesFromAnswer(resp);
                return resp;
            }
        } catch (Exception e) {
            log.warn("RagFlow 调用失败，降级到 DeepSeek: {}", e.getMessage());
        }
        RagFlowChatResponse resp = chatWithDeepSeek(request);
        extractRoutesFromAnswer(resp);
        return resp;
    }

    @org.springframework.beans.factory.annotation.Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @org.springframework.beans.factory.annotation.Value("${deepseek.base-url}")
    private String deepseekBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${deepseek.model}")
    private String deepseekModel;

    private RagFlowChatResponse chatWithDeepSeek(RagFlowChatRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", deepseekModel);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "你是一个专业的旅游规划助手。请为用户提供详细的旅游攻略，包括：\\n" +
                "- 景点介绍和游览顺序（上午/下午安排）\\n" +
                "- 交通方式建议\\n" +
                "- 美食推荐\\n" +
                "- 实用的旅行贴士\\n" +
                "回答要详细具体，方便后续提取路线信息。"));
            messages.add(Map.of("role", "user", "content", request.getMessage() != null ? request.getMessage() : ""));
            payload.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = normalizeBaseUrl(deepseekBaseUrl) + "/v1/chat/completions";

            log.info("调用 DeepSeek: url={}", url);
            String raw = restTemplate.postForObject(url, entity, String.class);
            log.info("DeepSeek 原始响应: {}", raw);
            RagFlowChatResponse resp = parseResponse(raw, request);
            resp.setSource("deepseek");
            if (resp.getAnswer() == null && raw != null) {
                JsonNode root = objectMapper.readTree(raw);
                String answer = root.path("choices").get(0).path("message").path("content").asText(null);
                resp.setAnswer(answer != null ? answer : raw);
            }
            return resp;
        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            RagFlowChatResponse fallback = new RagFlowChatResponse();
            fallback.setVisitorId(request.getVisitorId());
            fallback.setSessionId(request.getSessionId());
            fallback.setAnswer("抱歉，AI服务暂时不可用，请稍后重试。");
            fallback.setSource("fallback");
            return fallback;
        }
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
            log.info("解析响应 JSON: {}", root.toPrettyString());
            String answer = findText(root, "answer", "data", "text", "content", "response", "output");
            if (answer == null) answer = root.path("data").path("answer").asText(null);
            if (answer == null) answer = root.path("data").path("output").asText(null);
            // Handle OpenAI/DeepSeek format: choices[0].message.content
            if (answer == null) {
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    answer = choices.get(0).path("message").path("content").asText(null);
                }
            }
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

    private static final Map<String, String> SCENIC_CITY = Map.of(
        "灵山胜境", "无锡市",
        "黄山", "黄山市",
        "故宫", "北京市",
        "西湖", "杭州市",
        "张家界", "张家界市"
    );

    private void extractRoutesFromAnswer(RagFlowChatResponse resp) {
        String answer = resp.getAnswer();
        String scenicSpot = resp.getScenicSpot();
        if (answer == null || answer.isBlank() || answer.length() < 20) return;
        String cityConstraint = "";
        if (scenicSpot != null && !scenicSpot.isBlank() && !"景区入口".equals(scenicSpot)) {
            String city = SCENIC_CITY.getOrDefault(scenicSpot, "");
            if (!city.isEmpty()) {
                cityConstraint = "关键约束：该攻略是关于「" + scenicSpot + "」的，该景区位于「" + city + "」。所有point的city字段必须统一填「" + city + "」，不得使用其他城市名或区域名。\\n";
            }
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", deepseekModel);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "提取攻略中所有可在地图上定位的地点，输出JSON。若无法提取任何地点则返回{\\\"dailyRoutes\\\":[],\\\"mode\\\":\\\"city\\\"}。\\n" +
                "格式：{\\\"dailyRoutes\\\":[{\\\"points\\\":[{\\\"keyword\\\":\\\"地点名\\\",\\\"city\\\":\\\"区域名\\\"}]}],\\\"mode\\\":\\\"city或scenic\\\"}\\n" +
                "规则：\\n" +
                "- keyword必须是地图可搜到的标准地名\\n" +
                "- 城市跨区游：city用市县名，mode用\\\"city\\\"，每个dailyRoutes元素代表一天\\n" +
                "- 单一景区内部游（如故宫、颐和园）：keyword用内部点位，city统一用景区所在城区，mode用\\\"scenic\\\"。按时间段分组：上午、下午各一组，点多的景区可加中午/晚上组。每组5-6个点，每组作为dailyRoutes的一个元素\\n" +
                cityConstraint +
                "仅输出纯JSON，不要markdown包裹。"));
            messages.add(Map.of("role", "user", "content", answer.length() > 4000 ? answer.substring(0, 4000) : answer));
            payload.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = normalizeBaseUrl(deepseekBaseUrl) + "/v1/chat/completions";
            String raw = restTemplate.postForObject(url, entity, String.class);

            if (raw != null && !raw.isBlank()) {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText(null);
                    if (content != null) {
                        // clean markdown
                        content = content.trim();
                        if (content.startsWith("```")) {
                            int start = content.indexOf("\n");
                            int end = content.lastIndexOf("```");
                            if (start >= 0 && end > start) content = content.substring(start, end).trim();
                        }
                        JsonNode routeJson = objectMapper.readTree(content);
                        if (routeJson.has("dailyRoutes") && routeJson.get("dailyRoutes").isArray()
                                && routeJson.get("dailyRoutes").size() > 0) {
                            resp.setDailyRoutes(content);
                            resp.setMode(routeJson.has("mode") ? routeJson.get("mode").asText("city") : "city");
                            log.info("路线数据提取成功: {} 条, mode={}, JSON: {}", routeJson.get("dailyRoutes").size(), resp.getMode(), content);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("路线提取失败（不影响对话）: {}", e.getMessage());
        }
    }
}
