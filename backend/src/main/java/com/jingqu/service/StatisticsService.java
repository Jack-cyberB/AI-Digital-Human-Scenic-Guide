package com.jingqu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jingqu.dto.DashboardData;
import com.jingqu.entity.DailyStatistics;
import com.jingqu.entity.OnlineVisitor;
import com.jingqu.entity.VisitorInteraction;
import com.jingqu.mapper.DailyStatisticsMapper;
import com.jingqu.mapper.OnlineVisitorMapper;
import com.jingqu.mapper.VisitorInteractionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsService {

    @Autowired
    private DailyStatisticsMapper statisticsMapper;

    @Autowired
    private VisitorInteractionMapper interactionMapper;

    @Autowired
    private OnlineVisitorMapper onlineVisitorMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取今日统计数据
     */
    public DailyStatistics getTodayStatistics() {
        LocalDate today = LocalDate.now();
        
        QueryWrapper<DailyStatistics> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("stat_date", today);
        
        DailyStatistics statistics = statisticsMapper.selectOne(queryWrapper);
        
        if (statistics == null) {
            // 创建今日统计记录
            statistics = new DailyStatistics();
            statistics.setStatDate(today);
            statistics.setTotalInteractions(0);
            statistics.setTotalVisitors(0);
            statistics.setPeakHour(0);
            statistics.setPopularQa("[]");
            statistics.setHotspotSpots("[]");
            statistics.setHourlyData("{}");
            statisticsMapper.insert(statistics);
        }
        
        return statistics;
    }

    /**
     * 获取实时大屏数据
     */
    public DashboardData getRealtimeDashboardData() {
        DashboardData dashboard = new DashboardData();
        
        // 获取今日统计
        DailyStatistics todayStats = getTodayStatistics();
        
        // 获取在线访客数
        QueryWrapper<OnlineVisitor> onlineQuery = new QueryWrapper<>();
        onlineQuery.eq("is_online", 1);
        long onlineCount = onlineVisitorMapper.selectCount(onlineQuery);
        
        // 设置基本数据
        dashboard.setTotalInteractions(todayStats.getTotalInteractions());
        dashboard.setOnlineVisitors((int) onlineCount);
        dashboard.setTodayVisitors(todayStats.getTotalVisitors());
        dashboard.setSatisfactionRate(98.5); // 模拟满意度
        
        // 解析热门问答
        try {
            dashboard.setPopularQA(objectMapper.readValue(
                todayStats.getPopularQa(),
                new TypeReference<List<Map<String, Object>>>() {}
            ));
        } catch (Exception e) {
            dashboard.setPopularQA(new ArrayList<>());
        }
        
        // 解析热门景点
        try {
            dashboard.setHotspotSpots(objectMapper.readValue(
                todayStats.getHotspotSpots(),
                new TypeReference<List<Map<String, Object>>>() {}
            ));
        } catch (Exception e) {
            dashboard.setHotspotSpots(new ArrayList<>());
        }
        
        // 解析每小时数据
        try {
            dashboard.setHourlyData(objectMapper.readValue(
                todayStats.getHourlyData(),
                new TypeReference<List<Map<String, Object>>>() {}
            ));
        } catch (Exception e) {
            dashboard.setHourlyData(new ArrayList<>());
        }
        
        // 获取最近交互记录
        dashboard.setRecentInteractions(getRecentInteractions(10));
        
        dashboard.setUpdateTime(LocalDateTime.now());
        
        return dashboard;
    }

    /**
     * 获取最近交互记录
     */
    public List<DashboardData.RecentInteraction> getRecentInteractions(int limit) {
        QueryWrapper<VisitorInteraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.orderByDesc("interaction_time");
        queryWrapper.last("LIMIT " + limit);
        
        List<VisitorInteraction> interactions = interactionMapper.selectList(queryWrapper);
        
        return interactions.stream()
            .map(interaction -> new DashboardData.RecentInteraction(
                interaction.getVisitorId(),
                interaction.getQuestion(),
                interaction.getAnswer(),
                interaction.getScenicSpot(),
                interaction.getFinalAnswerSource(),
                interaction.getFallbackUsed() != null && interaction.getFallbackUsed() == 1,
                interaction.getInteractionTime()
            ))
            .collect(Collectors.toList());
    }

    public void recordAnswerMetrics(String question, String answer, String scenicSpot) {
        incrementTodayInteractions();
        updateHourlyData(LocalDateTime.now().getHour());
        updatePopularQA(question, answer);
        updateHotspotSpots(scenicSpot);
    }

    /**
     * 获取历史统计数据
     */
    public List<DailyStatistics> getHistoryStatistics(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<DailyStatistics> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("stat_date", startDate);
        queryWrapper.le("stat_date", endDate);
        queryWrapper.orderByDesc("stat_date");
        
        return statisticsMapper.selectList(queryWrapper);
    }

    /**
     * 今日交互次数+1
     */
    public void incrementTodayInteractions() {
        DailyStatistics statistics = getTodayStatistics();
        statistics.setTotalInteractions(statistics.getTotalInteractions() + 1);
        statisticsMapper.updateById(statistics);
    }

    /**
     * 今日访客数+1
     */
    public void incrementTodayVisitors() {
        DailyStatistics statistics = getTodayStatistics();
        statistics.setTotalVisitors(statistics.getTotalVisitors() + 1);
        statisticsMapper.updateById(statistics);
    }

    /**
     * 更新高峰时段
     */
    public void updatePeakHour(int hour) {
        DailyStatistics statistics = getTodayStatistics();
        
        try {
            Map<Integer, Integer> hourlyData = objectMapper.readValue(
                statistics.getHourlyData(),
                new TypeReference<Map<Integer, Integer>>() {}
            );
            
            int currentCount = hourlyData.getOrDefault(hour, 0);
            hourlyData.put(hour, currentCount + 1);
            
            // 更新高峰时段
            int maxHour = statistics.getPeakHour();
            int maxCount = hourlyData.getOrDefault(maxHour, 0);
            if (currentCount + 1 > maxCount) {
                statistics.setPeakHour(hour);
            }
            
            statistics.setHourlyData(objectMapper.writeValueAsString(hourlyData));
            statisticsMapper.updateById(statistics);
        } catch (Exception e) {
            log.error("更新高峰时段失败", e);
        }
    }

    /**
     * 更新每小时数据
     */
    public void updateHourlyData(int hour) {
        DailyStatistics statistics = getTodayStatistics();
        
        try {
            Map<Integer, Integer> hourlyData;
            if (statistics.getHourlyData() != null && !statistics.getHourlyData().isEmpty()) {
                hourlyData = objectMapper.readValue(
                    statistics.getHourlyData(),
                    new TypeReference<Map<Integer, Integer>>() {}
                );
            } else {
                hourlyData = new HashMap<>();
            }
            
            hourlyData.put(hour, hourlyData.getOrDefault(hour, 0) + 1);
            
            // 更新高峰时段
            int maxHour = statistics.getPeakHour();
            int maxCount = hourlyData.getOrDefault(maxHour, 0);
            if (hourlyData.get(hour) > maxCount) {
                statistics.setPeakHour(hour);
            }
            
            statistics.setHourlyData(objectMapper.writeValueAsString(hourlyData));
            statisticsMapper.updateById(statistics);
        } catch (Exception e) {
            log.error("更新每小时数据失败", e);
        }
    }

    /**
     * 更新热门问答
     */
    public void updatePopularQA(String question, String answer) {
        DailyStatistics statistics = getTodayStatistics();
        
        try {
            List<Map<String, Object>> popularQA;
            if (statistics.getPopularQa() != null && !statistics.getPopularQa().isEmpty()) {
                popularQA = objectMapper.readValue(
                    statistics.getPopularQa(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                popularQA = new ArrayList<>();
            }
            
            // 查找是否已存在该问题
            boolean found = false;
            for (Map<String, Object> qa : popularQA) {
                if (question.equals(qa.get("question"))) {
                    qa.put("count", ((Number) qa.get("count")).intValue() + 1);
                    qa.put("answer", answer);
                    found = true;
                    break;
                }
            }
            
            // 如果不存在，添加新的
            if (!found) {
                Map<String, Object> newQA = new HashMap<>();
                newQA.put("question", question);
                newQA.put("answer", answer);
                newQA.put("count", 1);
                popularQA.add(newQA);
            }
            
            // 按出现次数排序，只保留TOP10
            popularQA.sort((a, b) -> 
                ((Number) b.get("count")).intValue() - ((Number) a.get("count")).intValue()
            );
            
            if (popularQA.size() > 10) {
                popularQA = popularQA.subList(0, 10);
            }
            
            statistics.setPopularQa(objectMapper.writeValueAsString(popularQA));
            statisticsMapper.updateById(statistics);
        } catch (Exception e) {
            log.error("更新热门问答失败", e);
        }
    }

    /**
     * 更新热门景点
     */
    public void updateHotspotSpots(String scenicSpot) {
        if (scenicSpot == null || scenicSpot.isEmpty()) {
            return;
        }
        
        DailyStatistics statistics = getTodayStatistics();
        
        try {
            List<Map<String, Object>> hotspotSpots;
            if (statistics.getHotspotSpots() != null && !statistics.getHotspotSpots().isEmpty()) {
                hotspotSpots = objectMapper.readValue(
                    statistics.getHotspotSpots(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                hotspotSpots = new ArrayList<>();
            }
            
            // 查找是否已存在该景点
            boolean found = false;
            for (Map<String, Object> spot : hotspotSpots) {
                if (scenicSpot.equals(spot.get("name"))) {
                    spot.put("count", ((Number) spot.get("count")).intValue() + 1);
                    found = true;
                    break;
                }
            }
            
            // 如果不存在，添加新的
            if (!found) {
                Map<String, Object> newSpot = new HashMap<>();
                newSpot.put("name", scenicSpot);
                newSpot.put("count", 1);
                hotspotSpots.add(newSpot);
            }
            
            // 按访问次数排序
            hotspotSpots.sort((a, b) -> 
                ((Number) b.get("count")).intValue() - ((Number) a.get("count")).intValue()
            );
            
            statistics.setHotspotSpots(objectMapper.writeValueAsString(hotspotSpots));
            statisticsMapper.updateById(statistics);
        } catch (Exception e) {
            log.error("更新热门景点失败", e);
        }
    }

    /**
     * 获取交互记录（分页）
     */
    public List<VisitorInteraction> getInteractionRecords(int page, int size, String visitorId, String startDate, String endDate) {
        QueryWrapper<VisitorInteraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        
        if (visitorId != null && !visitorId.isEmpty()) {
            queryWrapper.eq("visitor_id", visitorId);
        }
        
        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("interaction_time", startDate);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("interaction_time", endDate);
        }
        
        queryWrapper.orderByDesc("interaction_time");
        queryWrapper.last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        
        return interactionMapper.selectList(queryWrapper);
    }
}
