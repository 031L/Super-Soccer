package com.example.javaai.websocket.football;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * 足球分析 STOMP + SockJS 配置。
 * <p>
 * 对外 URL：{@code http://host:8123/api/ws/football}（含 context-path /api）
 */
@Configuration
@EnableWebSocketMessageBroker
public class FootballStompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandshakeInterceptor handshakeInterceptor;
    private final StompChannelInterceptor channelInterceptor;

    public FootballStompWebSocketConfig(StompHandshakeInterceptor handshakeInterceptor,
                                        StompChannelInterceptor channelInterceptor) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.channelInterceptor = channelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/football")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendTimeLimit(120_000);
        registration.setSendBufferSizeLimit(512 * 1024);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }
}
