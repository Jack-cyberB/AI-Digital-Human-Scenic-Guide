package com.jingqu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jingqu.dto.*;
import com.jingqu.entity.*;
import com.jingqu.mapper.*;
import com.jingqu.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebSocketService {
    @Autowired private VisitorInteractionMapper interactionMapper;
    @Autowired private KnowledgeBaseMapper knowledgeMapper;
    @Autowired private EmergencyNotificationMapper notificationMapper;
    @Autowired private OnlineVisitorMapper onlineVisitorMapper;
    @Autowired private DailyStatisticsMapper statisticsMapper;
    @Autowired private KnowledgeService knowledgeService;
    @Autowired private NotificationService notificationService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private RagFlowService ragFlowService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private final Map<String, String> onlineVisitors = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToVisitor = new ConcurrentHashMap<>();

    public void broadcastRagFlowUpdate(RagFlowChatResponse response) {
        try {
            WebSocketMessage wsMessage = new WebSocketMessage("RAGFLOW_UPDATE", response);
            messagingTemplate.convertAndSend("/topic/admin/ragflow", wsMessage);
            messagingTemplate.convertAndSend("/topic/admin/avatar", wsMessage);
            if (response.getVisitorId() != null) messagingTemplate.convertAndSend("/topic/visitor/" + response.getVisitorId(), wsMessage);
        } catch (Exception e) { log.error("广播 RAGFlow 更新失败", e); }
    }

    public void registerVisitor(String visitorId, String sessionId) { /* unchanged */
        log.info("注册访客: visitorId={}, sessionId={}", visitorId, sessionId);
        onlineVisitors.put(visitorId, sessionId);
        sessionToVisitor.put(sessionId, visitorId);
        OnlineVisitor onlineVisitor = new OnlineVisitor();
        onlineVisitor.setVisitorId(visitorId);
        onlineVisitor.setSessionId(sessionId);
        onlineVisitor.setConnectedTime(LocalDateTime.now());
        onlineVisitor.setLastActiveTime(LocalDateTime.now());
        onlineVisitor.setIsOnline(1);
        QueryWrapper<OnlineVisitor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("visitor_id", visitorId);
        OnlineVisitor existing = onlineVisitorMapper.selectOne(queryWrapper);
        if (existing != null) { onlineVisitor.setId(existing.getId()); onlineVisitorMapper.updateById(onlineVisitor); } else { onlineVisitorMapper.insert(onlineVisitor); }
        statisticsService.incrementTodayVisitors();
        broadcastDashboardUpdate();
    }

    public void removeVisitorBySession(String sessionId) { /* unchanged */
        String visitorId = sessionToVisitor.remove(sessionId);
        if (visitorId != null) {
            onlineVisitors.remove(visitorId);
            QueryWrapper<OnlineVisitor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("visitor_id", visitorId);
            OnlineVisitor onlineVisitor = onlineVisitorMapper.selectOne(queryWrapper);
            if (onlineVisitor != null) { onlineVisitor.setIsOnline(0); onlineVisitor.setLastActiveTime(LocalDateTime.now()); onlineVisitorMapper.updateById(onlineVisitor); }
            log.info("移除访客连接: visitorId={}", visitorId);
            broadcastDashboardUpdate();
        }
    }

    public void processVisitorMessage(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            String visitorId = payload.get("visitorId") != null ? payload.get("visitorId").toString() : null;
            String sessionId = payload.get("sessionId") != null ? payload.get("sessionId").toString() : null;
            String userMessage = payload.get("message") != null ? payload.get("message").toString() : null;
            String scenicSpot = payload.get("scenicSpot") != null ? payload.get("scenicSpot").toString() : "景区入口";
            if (visitorId == null || visitorId.isBlank() || userMessage == null || userMessage.isBlank()) throw new IllegalArgumentException("访客消息缺少必要参数");

            VisitorInteraction interaction = new VisitorInteraction();
            interaction.setVisitorId(visitorId);
            interaction.setSessionId(sessionId);
            interaction.setQuestion(userMessage);
            interaction.setScenicSpot(scenicSpot);
            interaction.setInteractionTime(LocalDateTime.now());
            interaction.setInteractionType("QA");
            interactionMapper.insert(interaction);

            RagFlowChatRequest ragReq = new RagFlowChatRequest();
            ragReq.setVisitorId(visitorId);
            ragReq.setSessionId(sessionId);
            ragReq.setMessage(userMessage);
            ragReq.setScenicSpot(scenicSpot);
            ragReq.setStream(false);
            ragReq.setDatasetIds(null);
            RagFlowChatResponse ragResp = ragFlowService.chat(ragReq);

            interaction.setAnswer(ragResp.getAnswer());
            interactionMapper.updateById(interaction);

            VisitorMessageResponse response = new VisitorMessageResponse();
            response.setVisitorId(visitorId);
            response.setAnswer(ragResp.getAnswer());
            response.setSessionId(sessionId);
            response.setMessageType("AI_RESPONSE");
            response.setTimestamp(LocalDateTime.now());
            response.setScenicSpot(scenicSpot);

            WebSocketMessage wsMessage = new WebSocketMessage(WebSocketMessage.TYPE_AI_RESPONSE, response);
            messagingTemplate.convertAndSend("/topic/visitor/" + visitorId, wsMessage);
            messagingTemplate.convertAndSend("/queue/visitor/" + visitorId, wsMessage);
            statisticsService.incrementTodayInteractions();
            statisticsService.updateHourlyData(LocalDateTime.now().getHour());
            statisticsService.updatePopularQA(userMessage, ragResp.getAnswer());

            ragResp.setVisitorId(visitorId);
            ragResp.setSessionId(sessionId);
            ragResp.setScenicSpot(scenicSpot);
            broadcastRagFlowUpdate(ragResp);
            broadcastDashboardUpdate();
        } catch (Exception e) {
            log.error("处理访客消息失败", e);
            WebSocketMessage errorMessage = new WebSocketMessage(WebSocketMessage.TYPE_ERROR, "处理消息失败，请稍后重试");
            @SuppressWarnings("unchecked") Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Object visitorIdValue = payload != null ? payload.get("visitorId") : null;
            String visitorId = visitorIdValue != null ? visitorIdValue.toString() : null;
            if (visitorId != null && !visitorId.isBlank()) messagingTemplate.convertAndSend("/topic/visitor/" + visitorId, errorMessage);
        }
    }

    public void broadcastNotification(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) { /* unchanged */
        try {
            @SuppressWarnings("unchecked") Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            String title = (String) payload.get("title");
            String content = (String) payload.get("content");
            String notificationType = (String) payload.getOrDefault("notificationType", "INFO");
            String targetScope = (String) payload.getOrDefault("targetScope", "ALL");
            EmergencyNotification notification = new EmergencyNotification();
            notification.setTitle(title);
            notification.setContent(content);
            notification.setNotificationType(notificationType);
            notification.setTargetScope(targetScope);
            notification.setPushTime(LocalDateTime.now());
            notification.setStatus(1);
            notificationMapper.insert(notification);
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("notificationId", notification.getId());
            notificationData.put("title", title);
            notificationData.put("content", content);
            notificationData.put("type", notificationType);
            notificationData.put("timestamp", LocalDateTime.now());
            WebSocketMessage wsMessage = new WebSocketMessage(WebSocketMessage.TYPE_NOTIFICATION, notificationData);
            messagingTemplate.convertAndSend("/topic/all-visitors", wsMessage);
        } catch (Exception e) { log.error("广播通知失败", e); }
    }

    public void broadcastKnowledgeUpdate(WebSocketMessage message, SimpMessagingTemplate messagingTemplate) { /* unchanged */
        try { messagingTemplate.convertAndSend("/topic/knowledge-update", new WebSocketMessage(WebSocketMessage.TYPE_KNOWLEDGE_UPDATE, message.getPayload())); } catch (Exception e) { log.error("广播知识库更新失败", e); }
    }
    public void broadcastDashboardUpdate() { try { messagingTemplate.convertAndSend("/topic/admin/dashboard", new WebSocketMessage(WebSocketMessage.TYPE_DASHBOARD_UPDATE, statisticsService.getRealtimeDashboardData())); } catch (Exception e) { log.error("广播大屏数据更新失败", e); } }
    public int getOnlineVisitorCount() { return onlineVisitors.size(); }
    @Scheduled(fixedRate = 30000) public void heartbeatCheck() { WebSocketMessage heartbeat = new WebSocketMessage(WebSocketMessage.TYPE_HEARTBEAT, LocalDateTime.now()); for (String visitorId : onlineVisitors.keySet()) { try { messagingTemplate.convertAndSend("/topic/visitor/" + visitorId, heartbeat); } catch (Exception e) { log.warn("心跳发送失败: visitorId={}", visitorId); } } }
}
