package com.jingqu.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {
    private String type;
    private String target;
    private Object payload;
    private LocalDateTime timestamp;

    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(String type, String target, Object payload) {
        this.type = type;
        this.target = target;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    // 消息类型常量
    public static final String TYPE_VISITOR_CONNECT = "VISITOR_CONNECT";
    public static final String TYPE_VISITOR_MESSAGE = "VISITOR_MESSAGE";
    public static final String TYPE_AI_RESPONSE = "AI_RESPONSE";
    public static final String TYPE_NOTIFICATION = "NOTIFICATION";
    public static final String TYPE_KNOWLEDGE_UPDATE = "KNOWLEDGE_UPDATE";
    public static final String TYPE_DASHBOARD_UPDATE = "DASHBOARD_UPDATE";
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";
    public static final String TYPE_ERROR = "ERROR";
}
