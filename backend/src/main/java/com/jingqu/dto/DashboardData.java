package com.jingqu.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardData {
    private Integer totalInteractions;
    private Integer onlineVisitors;
    private Integer todayVisitors;
    private Double satisfactionRate;
    private List<Map<String, Object>> popularQA;
    private List<Map<String, Object>> hotspotSpots;
    private List<Map<String, Object>> hourlyData;
    private List<RecentInteraction> recentInteractions;
    private LocalDateTime updateTime;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentInteraction {
        private String visitorId;
        private String question;
        private String answer;
        private String scenicSpot;
        private String finalAnswerSource;
        private Boolean fallbackUsed;
        private LocalDateTime time;
    }
}
