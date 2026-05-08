package com.jingqu.websocket;

import com.jingqu.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 处理游客连接
     */
    @MessageMapping("/visitor/connect")
    public void handleVisitorConnect(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("游客连接: {}", message);

        String visitorId = null;
        String scenicSpot = null;
        Object payload = message.getPayload();
        if (payload instanceof java.util.Map<?, ?> map) {
            Object visitorIdValue = map.get("visitorId");
            Object scenicSpotValue = map.get("scenicSpot");
            visitorId = visitorIdValue != null ? visitorIdValue.toString() : null;
            scenicSpot = scenicSpotValue != null ? scenicSpotValue.toString() : null;
        } else if (payload != null) {
            visitorId = payload.toString();
        }

        if (visitorId != null && !visitorId.isBlank()) {
            String sessionId = headerAccessor.getSessionId();
            webSocketService.registerVisitor(visitorId, sessionId);

            // 发送连接成功消息
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("visitorId", visitorId);
            responseData.put("scenicSpot", scenicSpot);
            responseData.put("message", "连接成功");
            WebSocketMessage response = new WebSocketMessage(
                WebSocketMessage.TYPE_VISITOR_CONNECT,
                responseData
            );
            messagingTemplate.convertAndSend(
                "/topic/visitor/" + visitorId,
                response
            );
        }
    }

    /**
     * 处理游客消息
     */
    @MessageMapping("/visitor/message")
    public void handleVisitorMessage(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到游客消息: {}", message);
        
        webSocketService.processVisitorMessage(message, messagingTemplate);
    }

    /**
     * 处理管理员发送通知
     */
    @MessageMapping("/admin/notification")
    public void handleAdminNotification(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("管理员发送通知: {}", message);
        
        webSocketService.broadcastNotification(message, messagingTemplate);
    }

    /**
     * 处理管理员更新知识库
     */
    @MessageMapping("/admin/knowledge")
    public void handleKnowledgeUpdate(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        log.info("管理员更新知识库: {}", message);
        
        webSocketService.broadcastKnowledgeUpdate(message, messagingTemplate);
    }

    /**
     * WebSocket 连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("新的 WebSocket 连接建立");
    }

    /**
     * WebSocket 断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("WebSocket 连接断开");
        
        if (event.getSessionId() != null) {
            webSocketService.removeVisitorBySession(event.getSessionId());
        }
    }
}
