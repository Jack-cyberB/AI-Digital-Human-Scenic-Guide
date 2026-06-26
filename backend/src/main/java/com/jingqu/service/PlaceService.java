package com.jingqu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingqu.dto.PlaceDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    @Value("${amap.api-key:4f92d1e69d842f517f46af6b62ef0e80}")
    private String amapApiKey;

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${deepseek.model}")
    private String deepseekModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存：key = "keyword|city", 24h TTL
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final PlaceDetailDTO data;
        final long createdAt;
        CacheEntry(PlaceDetailDTO data) { this.data = data; this.createdAt = System.currentTimeMillis(); }
        boolean isExpired() { return System.currentTimeMillis() - createdAt > 24 * 60 * 60 * 1000; }
    }

    /**
     * 获取完整POI详情：Amap数据 + AI介绍 + AI评价
     */
    public PlaceDetailDTO getPlaceDetail(String keyword, String city, String amapId, Double lat, Double lng) {
        String cacheKey = keyword + "|" + (city != null ? city : "");
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("PlaceService: cache hit for {}", cacheKey);
            return cached.data;
        }

        PlaceDetailDTO dto = new PlaceDetailDTO();
        dto.setName(keyword);
        dto.setCity(city);
        if (lat != null) dto.setLat(lat);
        if (lng != null) dto.setLng(lng);

        // 1. 调用 Amap Web API 获取基础数据
        try {
            enrichFromAmap(dto, keyword, city, amapId);
        } catch (Exception e) {
            log.warn("Amap API 调用失败，继续AI增强: {}", e.getMessage());
        }

        // 2. DeepSeek 生成介绍 + 评价
        try {
            enrichWithAI(dto);
        } catch (Exception e) {
            log.warn("AI增强失败: {}", e.getMessage());
        }

        cache.put(cacheKey, new CacheEntry(dto));
        return dto;
    }

    /**
     * 仅AI增强（客户端已有Amap基础数据）
     */
    public PlaceDetailDTO enrichPlace(String keyword, String city) {
        PlaceDetailDTO dto = new PlaceDetailDTO();
        dto.setName(keyword);
        dto.setCity(city);
        try {
            enrichWithAI(dto);
        } catch (Exception e) {
            log.warn("AI增强失败: {}", e.getMessage());
            dto.setAiDescription("暂无详细介绍");
            dto.setAiReviews(Collections.emptyList());
        }
        return dto;
    }

    /**
     * 周边POI搜索（代理Amap Web API）
     */
    public List<Map<String, Object>> searchNearby(double lat, double lng, int radius, String type, int limit) {
        try {
            String url = String.format(
                "https://restapi.amap.com/v5/place/around?location=%.6f,%.6f&radius=%d&types=%s&extensions=all&page_size=%d&key=%s",
                lng, lat, radius, type, Math.min(limit, 25), amapApiKey
            );
            log.info("PlaceService: nearby search url={}", url);
            String raw = restTemplate.getForObject(url, String.class);
            if (raw != null) {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode pois = root.path("pois");
                List<Map<String, Object>> results = new ArrayList<>();
                if (pois.isArray()) {
                    for (JsonNode poi : pois) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", poi.path("name").asText(""));
                        item.put("address", poi.path("paddress").asText(poi.path("address").asText("")));
                        String loc = poi.path("location").asText("");
                        if (!loc.isEmpty()) {
                            String[] parts = loc.split(",");
                            if (parts.length == 2) {
                                item.put("lng", Double.parseDouble(parts[0]));
                                item.put("lat", Double.parseDouble(parts[1]));
                            }
                        }
                        item.put("type", poi.path("type").asText(""));
                        item.put("typecode", poi.path("typecode").asText(""));
                        JsonNode rating = poi.path("biz_ext").path("rating");
                        if (!rating.isMissingNode() && !rating.isNull()) item.put("rating", rating.asDouble());
                        JsonNode cost = poi.path("biz_ext").path("cost");
                        if (!cost.isMissingNode() && !cost.isNull()) item.put("cost", cost.asText());
                        JsonNode photos = poi.path("photos");
                        if (photos.isArray()) {
                            List<String> photoUrls = new ArrayList<>();
                            for (JsonNode p : photos) {
                                String photoUrl = p.path("url").asText(null);
                                if (photoUrl == null) photoUrl = p.asText();
                                if (photoUrl != null && !photoUrl.isEmpty()) photoUrls.add(photoUrl);
                            }
                            if (!photoUrls.isEmpty()) item.put("photos", photoUrls);
                        }
                        results.add(item);
                    }
                }
                log.info("PlaceService: nearby search returned {} results", results.size());
                return results;
            }
        } catch (Exception e) {
            log.error("Amap nearby search 失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private void enrichFromAmap(PlaceDetailDTO dto, String keyword, String city, String amapId) throws Exception {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://restapi.amap.com/v5/place/detail?extensions=all&key=").append(amapApiKey);
        if (amapId != null && !amapId.isEmpty()) {
            urlBuilder.append("&id=").append(amapId);
        } else {
            urlBuilder.append("&keywords=").append(java.net.URLEncoder.encode(keyword, "UTF-8"));
            if (city != null && !city.isEmpty()) {
                urlBuilder.append("&city=").append(java.net.URLEncoder.encode(city, "UTF-8"));
            }
        }

        String url = urlBuilder.toString();
        log.info("PlaceService: Amap detail url={}", url);
        String raw = restTemplate.getForObject(url, String.class);
        if (raw == null) return;

        JsonNode root = objectMapper.readTree(raw);
        JsonNode pois = root.path("pois");
        if (!pois.isArray() || pois.size() == 0) return;
        JsonNode poi = pois.get(0);

        dto.setName(poi.path("name").asText(dto.getName()));
        dto.setAddress(poi.path("paddress").asText(poi.path("address").asText("")));
        String loc = poi.path("location").asText("");
        if (!loc.isEmpty()) {
            String[] parts = loc.split(",");
            if (parts.length == 2) {
                if (dto.getLat() == null) dto.setLat(Double.parseDouble(parts[1]));
                if (dto.getLng() == null) dto.setLng(Double.parseDouble(parts[0]));
            }
        }
        dto.setCategoryTag(poi.path("type").asText(""));

        // 映射Amap分类到我们的五大类
        String typeCode = poi.path("typecode").asText("");
        dto.setCategory(mapAmapCategory(typeCode, dto.getCategoryTag()));

        JsonNode bizExt = poi.path("biz_ext");
        if (!bizExt.isMissingNode()) {
            JsonNode ratingNode = bizExt.path("rating");
            if (!ratingNode.isMissingNode() && !ratingNode.isNull()) dto.setRating(ratingNode.asDouble());
            JsonNode costNode = bizExt.path("cost");
            if (!costNode.isMissingNode() && !costNode.isNull()) dto.setCost(costNode.asText());
        }
        JsonNode deepInfo = poi.path("deep_info");
        if (!deepInfo.isMissingNode()) {
            dto.setOpeningHours(deepInfo.path("opentime").asText(""));
        }
        dto.setPhone(poi.path("tel").asText(""));

        // 提取照片
        JsonNode photos = poi.path("photos");
        if (photos.isArray()) {
            List<String> photoUrls = new ArrayList<>();
            for (JsonNode p : photos) {
                String photoUrl = p.path("url").asText(null);
                if (photoUrl == null) photoUrl = p.asText();
                if (photoUrl != null && !photoUrl.isEmpty()) photoUrls.add(photoUrl);
            }
            dto.setPhotos(photoUrls);
        }
    }

    private void enrichWithAI(PlaceDetailDTO dto) throws Exception {
        // 生成景点介绍
        String descPrompt = "你是「" + dto.getName() + "」的官方景区导览员。请写一段150字以内的专业景点介绍，要求：\n" +
            "- 用客观专业的语言介绍历史背景和文化内涵\n" +
            "- 突出1-2个核心亮点或必看内容\n" +
            "- 如适用，包含开放时间、最佳游览季节等实用信息\n" +
            "- 语气正式专业但不生硬，让游客感到权威可信\n" +
            "只输出介绍正文，不要任何前缀。";
        dto.setAiDescription(callDeepSeek(descPrompt));

        // 生成3条小红书风格模拟评价
        String reviewPrompt = "请生成3条小红书风格的「" + dto.getName() + "」游客短评，每条80字以内，包含真实感受和实用小建议。" +
            "每条附一个中文昵称和点赞数（500-5000）。" +
            "输出纯JSON数组格式（不要markdown包裹）：" +
            "[{\"userName\":\"昵称\",\"content\":\"评价内容\",\"likeCount\":1234,\"rating\":5}]";
        String reviewRaw = callDeepSeek(reviewPrompt);
        if (reviewRaw != null && !reviewRaw.isEmpty()) {
            try {
                // clean markdown if any
                String cleaned = reviewRaw.trim();
                if (cleaned.startsWith("```")) {
                    int start = cleaned.indexOf("\n");
                    int end = cleaned.lastIndexOf("```");
                    if (start >= 0 && end > start) cleaned = cleaned.substring(start, end).trim();
                }
                JsonNode arr = objectMapper.readTree(cleaned);
                List<PlaceDetailDTO.ReviewCard> reviews = new ArrayList<>();
                if (arr.isArray()) {
                    for (JsonNode r : arr) {
                        PlaceDetailDTO.ReviewCard card = new PlaceDetailDTO.ReviewCard();
                        card.setUserName(r.path("userName").asText("游客"));
                        card.setContent(r.path("content").asText(""));
                        card.setLikeCount(r.path("likeCount").asInt(1000));
                        card.setRating(r.path("rating").asInt(5));
                        card.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + card.getUserName());
                        reviews.add(card);
                    }
                }
                dto.setAiReviews(reviews);
            } catch (Exception e) {
                log.warn("解析AI评价JSON失败: {}", e.getMessage());
                dto.setAiReviews(Collections.emptyList());
            }
        }
    }

    private String callDeepSeek(String prompt) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", deepseekModel);
        payload.put("stream", false);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        payload.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepseekApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String url = (deepseekBaseUrl.endsWith("/") ? deepseekBaseUrl.substring(0, deepseekBaseUrl.length() - 1) : deepseekBaseUrl)
            + "/v1/chat/completions";
        String raw = restTemplate.postForObject(url, entity, String.class);
        if (raw != null) {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText("");
            }
        }
        return "";
    }

    private String mapAmapCategory(String typecode, String typeTag) {
        if (typecode == null && typeTag == null) return "景点";
        String combined = (typecode != null ? typecode : "") + "|" + (typeTag != null ? typeTag : "");

        if (combined.contains("06") || combined.contains("风景名胜") || combined.contains("公园") || combined.contains("景点")) return "景点";
        if (combined.contains("05") || combined.contains("餐饮") || combined.contains("餐厅") || combined.contains("美食")) return "美食";
        if (combined.contains("茶艺") || combined.contains("咖啡") || combined.contains("冷饮") || combined.contains("糕饼")) return "饮品";
        if (combined.contains("06") && (combined.contains("购物") || combined.contains("商场") || combined.contains("商街"))) return "购物";
        if (combined.matches(".*06.*")) return "购物"; // 06 = 购物服务大类
        if (combined.contains("10") || combined.contains("住宿") || combined.contains("酒店") || combined.contains("宾馆")) return "住宿";
        if (combined.contains("08") || combined.contains("体育休闲")) return "景点"; // 体育休闲归入景点

        return "景点"; // default
    }
}
