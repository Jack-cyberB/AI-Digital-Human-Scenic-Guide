package com.jingqu.controller;

import com.jingqu.dto.DashboardData;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.entity.DailyStatistics;
import com.jingqu.entity.VisitorInteraction;
import com.jingqu.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取今日统计数据
     */
    @GetMapping("/today")
    public ResponseDTO<DailyStatistics> getTodayStatistics() {
        DailyStatistics statistics = statisticsService.getTodayStatistics();
        return ResponseDTO.success(statistics);
    }

    /**
     * 获取实时大屏数据
     */
    @GetMapping("/realtime")
    public ResponseDTO<DashboardData> getRealtimeData() {
        DashboardData dashboard = statisticsService.getRealtimeDashboardData();
        return ResponseDTO.success(dashboard);
    }

    /**
     * 获取历史统计数据
     */
    @GetMapping("/history")
    public ResponseDTO<List<DailyStatistics>> getHistoryStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        List<DailyStatistics> statistics = statisticsService.getHistoryStatistics(startDate, endDate);
        return ResponseDTO.success(statistics);
    }

    /**
     * 获取交互记录列表
     */
    @GetMapping("/interactions")
    public ResponseDTO<Map<String, Object>> getInteractionRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String visitorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        List<VisitorInteraction> records = statisticsService.getInteractionRecords(page, size, visitorId, startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("current", page);
        result.put("size", size);
        
        return ResponseDTO.success(result);
    }
}
