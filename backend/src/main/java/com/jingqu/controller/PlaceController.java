package com.jingqu.controller;

import com.jingqu.dto.PlaceDetailDTO;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/place")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    /**
     * 获取POI完整详情（Amap + AI）
     */
    @PostMapping("/detail")
    public ResponseDTO<PlaceDetailDTO> getPlaceDetail(@RequestBody Map<String, Object> request) {
        String keyword = (String) request.getOrDefault("keyword", "");
        String city = (String) request.getOrDefault("city", "");
        String amapId = (String) request.getOrDefault("amapId", null);
        Double lat = request.get("lat") instanceof Number ? ((Number) request.get("lat")).doubleValue() : null;
        Double lng = request.get("lng") instanceof Number ? ((Number) request.get("lng")).doubleValue() : null;

        if (keyword.isBlank()) {
            return ResponseDTO.error(400, "keyword不能为空");
        }

        try {
            PlaceDetailDTO detail = placeService.getPlaceDetail(keyword, city, amapId, lat, lng);
            return ResponseDTO.success(detail);
        } catch (Exception e) {
            log.error("获取POI详情失败: {}", e.getMessage());
            return ResponseDTO.error("获取详情失败: " + e.getMessage());
        }
    }

    /**
     * 周边POI搜索
     */
    @GetMapping("/nearby")
    public ResponseDTO<List<Map<String, Object>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") int radius,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Map<String, Object>> pois = placeService.searchNearby(lat, lng, radius, type, limit);
            return ResponseDTO.success(pois);
        } catch (Exception e) {
            log.error("周边搜索失败: {}", e.getMessage());
            return ResponseDTO.error("周边搜索失败: " + e.getMessage());
        }
    }

    /**
     * 仅AI增强（客户端已有Amap基础数据）
     */
    @PostMapping("/enrich")
    public ResponseDTO<PlaceDetailDTO> enrichPlace(@RequestBody Map<String, Object> request) {
        String keyword = (String) request.getOrDefault("keyword", "");
        String city = (String) request.getOrDefault("city", "");

        if (keyword.isBlank()) {
            return ResponseDTO.error(400, "keyword不能为空");
        }

        try {
            PlaceDetailDTO detail = placeService.enrichPlace(keyword, city);
            return ResponseDTO.success(detail);
        } catch (Exception e) {
            log.error("AI增强失败: {}", e.getMessage());
            return ResponseDTO.error("AI增强失败: " + e.getMessage());
        }
    }
}
