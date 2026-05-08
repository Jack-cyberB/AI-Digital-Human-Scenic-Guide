package com.jingqu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jingqu.dto.AiAnswerResult;
import com.jingqu.dto.DashboardData;
import com.jingqu.dto.NotificationRequest;
import com.jingqu.dto.VisitorMessageRequest;
import com.jingqu.dto.VisitorMessageResponse;
import com.jingqu.entity.*;
import com.jingqu.mapper.*;
import com.jingqu.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WebSocketService {

    @Autowired
    private VisitorInteractionMapper interactionMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeMapper;

    @Autowired
    private EmergencyNotificationMapper notificationMapper;

    @Autowired
    private OnlineVisitorMapper onlineVisitorMapper;

    @Autowired
    private DailyStatisticsMapper statisticsMapper;

    @Autowired
    private AiAnswerService aiAnswerService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 存储在线访客信息: visitorId -> sessionId
    private final Map<String, String> onlineVisitors = new ConcurrentHashMap<>();
    
    // 存储会话ID到访客ID的映射: sessionId -> visitorId
    private final Map<String, String> sessionToVisitor = new ConcurrentHashMap<>();

    /**
     * 注册访客连接
     */
    public void registerVisitor(String visitorId, String sessionId) {
        log.info("注册访客: visitorId={}, sessionId={}", visitorId, sessionId);
        
        onlineVisitors.put(visitorId, sessionId);
        sessionToVisitor.put(sessionId, visitorId);
        
        // 更新数据库在线记录
        OnlineVisitor onlineVisitor = new OnlineVisitor();
        onlineVisitor.setVisitorId(visitorId);
        onlineVisitor.setSessionId(sessionId);
        onlineVisitor.setConnectedTime(LocalDateTime.now());
        onlineVisitor.setLastActiveTime(LocalDateTime.now());
        onlineVisitor.setIsOnline(1);
        
        QueryWrapper<OnlineVisitor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("visitor_id", visitorId);
        OnlineVisitor existing = onlineVisitorMapper.selectOne(queryWrapper);
        
        if (existing != null) {
            onlineVisitor.setId(existing.getId());
            onlineVisitorMapper.updateById(onlineVisitor);
        } else {
            onlineVisitorMapper.insert(onlineVisitor);
        }
        
        // 更新今日统计
        statisticsService.incrementTodayVisitors();
        
        // 广播更新到管理员大屏
        broadcastDashboardUpdate();
    }

    /**
     * 移除访客连接
     */
    public void removeVisitorBySession(String sessionId) {
        String visitorId = sessionToVisitor.remove(sessionId);
        if (visitorId != null) {
            onlineVisitors.remove(visitorId);
            
            // 更新数据库
            QueryWrapper<OnlineVisitor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("visitor_id", visitorId);
            OnlineVisitor onlineVisitor = onlineVisitorMapper.selectOne(queryWrapper);
            if (onlineVisitor != null) {
                onlineVisitor.setIsOnline(0);
                onlineVisitor.setLastActiveTime(LocalDateTime.now());
                onlineVisitorMapper.updateById(onlineVisitor);
            }
            
            log.info("移除访客连接: visitorId={}", visitorId);
            
            // 广播更新到管理员大屏
            broadcastDashboardUpdate();
        }
    }

    /**
     * 处理访客消息并生成AI回复
     */
    public void processVisitorMessage(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();

            String visitorId = payload.get("visitorId") != null ? payload.get("visitorId").toString() : null;
            String sessionId = payload.get("sessionId") != null ? payload.get("sessionId").toString() : null;
            String userMessage = payload.get("message") != null ? payload.get("message").toString() : null;
            String scenicSpot = payload.get("scenicSpot") != null ? payload.get("scenicSpot").toString() : "景区入口";

            if (visitorId == null || visitorId.isBlank() || userMessage == null || userMessage.isBlank()) {
                throw new IllegalArgumentException("访客消息缺少必要参数");
            }

            // 保存交互记录
            VisitorInteraction interaction = new VisitorInteraction();
            interaction.setVisitorId(visitorId);
            interaction.setSessionId(sessionId);
            interaction.setQuestion(userMessage);
            interaction.setScenicSpot(scenicSpot);
            interaction.setInteractionTime(LocalDateTime.now());
            interaction.setInteractionType("QA");
            interactionMapper.insert(interaction);

            // 查询知识库获取回答
            AiAnswerResult result = aiAnswerService.answer(userMessage, scenicSpot);
            String answer = result.getAnswer();

            // 更新交互记录的回答
            interaction.setAnswer(answer);
            interaction.setRouteTarget(result.getRouteTarget());
            interaction.setRetrievedDocsCount(result.getRetrievedDocsCount());
            interaction.setFallbackUsed(result.isFallbackUsed() ? 1 : 0);
            interaction.setModelLatencyMs(result.getModelLatencyMs());
            interaction.setFinalAnswerSource(result.getFinalAnswerSource());
            interaction.setKnowledgeSources(String.join(" | ", result.getKnowledgeSources()));
            interactionMapper.updateById(interaction);

            // 构建回复消息
            VisitorMessageResponse response = new VisitorMessageResponse();
            response.setVisitorId(visitorId);
            response.setAnswer(answer);
            response.setSessionId(sessionId);
            response.setMessageType("AI_RESPONSE");
            response.setTimestamp(LocalDateTime.now());
            response.setScenicSpot(scenicSpot);

            // 发送回复给对应访客
            WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.TYPE_AI_RESPONSE,
                response
            );
            messagingTemplate.convertAndSend(
                "/topic/visitor/" + visitorId,
                wsMessage
            );

            // 同时提供 HTTP 轮询兼容的游客端通知
            messagingTemplate.convertAndSend(
                "/queue/visitor/" + visitorId,
                wsMessage
            );

            // 更新统计数据
            statisticsService.recordAnswerMetrics(userMessage, answer, scenicSpot);

            // 广播更新到管理员大屏
            broadcastDashboardUpdate();

            log.info("处理访客消息完成: visitorId={}, question={}", visitorId, userMessage);

        } catch (Exception e) {
            log.error("处理访客消息失败", e);

            WebSocketMessage errorMessage = new WebSocketMessage(
                WebSocketMessage.TYPE_ERROR,
                "处理消息失败，请稍后重试"
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Object visitorIdValue = payload != null ? payload.get("visitorId") : null;
            String visitorId = visitorIdValue != null ? visitorIdValue.toString() : null;
            if (visitorId != null && !visitorId.isBlank()) {
                messagingTemplate.convertAndSend(
                    "/topic/visitor/" + visitorId,
                    errorMessage
                );
            }
        }
    }

    /**
     * 广播通知给所有在线访客
     */
    public void broadcastNotification(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            
            String title = (String) payload.get("title");
            String content = (String) payload.get("content");
            String notificationType = (String) payload.getOrDefault("notificationType", "INFO");
            String targetScope = (String) payload.getOrDefault("targetScope", "ALL");
            
            // 保存通知到数据库
            EmergencyNotification notification = new EmergencyNotification();
            notification.setTitle(title);
            notification.setContent(content);
            notification.setNotificationType(notificationType);
            notification.setTargetScope(targetScope);
            notification.setPushTime(LocalDateTime.now());
            notification.setStatus(1);
            notificationMapper.insert(notification);
            
            // 构建推送消息
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("notificationId", notification.getId());
            notificationData.put("title", title);
            notificationData.put("content", content);
            notificationData.put("type", notificationType);
            notificationData.put("timestamp", LocalDateTime.now());
            
            WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.TYPE_NOTIFICATION,
                notificationData
            );
            
            // 广播给所有访客
            messagingTemplate.convertAndSend("/topic/all-visitors", wsMessage);
            
            log.info("广播通知完成: title={}, onlineCount={}", title, onlineVisitors.size());
            
        } catch (Exception e) {
            log.error("广播通知失败", e);
        }
    }

    /**
     * 广播知识库更新给所有访客
     */
    public void broadcastKnowledgeUpdate(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        try {
            WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.TYPE_KNOWLEDGE_UPDATE,
                message.getPayload()
            );
            
            // 广播给所有访客
            messagingTemplate.convertAndSend("/topic/knowledge-update", wsMessage);
            
            log.info("广播知识库更新完成");
            
        } catch (Exception e) {
            log.error("广播知识库更新失败", e);
        }
    }

    /**
     * 广播管理员大屏数据更新
     */
    public void broadcastDashboardUpdate() {
        try {
            DashboardData dashboardData = statisticsService.getRealtimeDashboardData();
            
            WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.TYPE_DASHBOARD_UPDATE,
                dashboardData
            );
            
            // 广播给所有管理员
            messagingTemplate.convertAndSend("/topic/admin/dashboard", wsMessage);
            
            log.debug("广播大屏数据更新完成");
            
        } catch (Exception e) {
            log.error("广播大屏数据更新失败", e);
        }
    }

    /**
     * 获取当前在线访客数
     */
    public int getOnlineVisitorCount() {
        return onlineVisitors.size();
    }

    public void recordInteraction(VisitorMessageRequest request, AiAnswerResult result) {
        VisitorInteraction interaction = new VisitorInteraction();
        interaction.setVisitorId(request.getVisitorId());
        interaction.setSessionId(request.getSessionId());
        interaction.setQuestion(request.getMessage());
        interaction.setAnswer(result.getAnswer());
        interaction.setInteractionType("QA");
        interaction.setScenicSpot(request.getScenicSpot());
        interaction.setInteractionTime(LocalDateTime.now());
        interaction.setRouteTarget(result.getRouteTarget());
        interaction.setRetrievedDocsCount(result.getRetrievedDocsCount());
        interaction.setFallbackUsed(result.isFallbackUsed() ? 1 : 0);
        interaction.setModelLatencyMs(result.getModelLatencyMs());
        interaction.setFinalAnswerSource(result.getFinalAnswerSource());
        interaction.setKnowledgeSources(String.join(" | ", result.getKnowledgeSources()));
        interactionMapper.insert(interaction);
        broadcastDashboardUpdate();
    }

    /**
     * 心跳检测定时任务
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeatCheck() {
        WebSocketMessage heartbeat = new WebSocketMessage(
            WebSocketMessage.TYPE_HEARTBEAT,
            LocalDateTime.now()
        );
        
        // 发送给所有在线访客
        for (String visitorId : onlineVisitors.keySet()) {
            try {
                messagingTemplate.convertAndSend(
                    "/topic/visitor/" + visitorId,
                    heartbeat
                );
            } catch (Exception e) {
                log.warn("心跳发送失败: visitorId={}", visitorId);
            }
        }
        
        log.debug("心跳检测完成, 在线人数: {}", onlineVisitors.size());
    }
}
