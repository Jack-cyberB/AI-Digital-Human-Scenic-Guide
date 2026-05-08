package com.jingqu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 配置消息代理前缀
        // /topic 用于广播消息（一对多）
        // /queue 用于点对点消息（一对一）
        config.enableSimpleBroker("/topic", "/queue");
        
        // 配置应用目标前缀，客户端发送消息时使用
        config.setApplicationDestinationPrefixes("/app");
        
        // 配置用户目标前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点，客户端通过该端点连接 WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 同时注册不带 SockJS 的 WebSocket 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
