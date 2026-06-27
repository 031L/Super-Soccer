package com.example.javaai.websocket.football;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * SockJS 握手时为连接分配 principalName（session 级 UUID）。
 */
@Component
@Slf4j
public class StompHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_NAME_ATTR = "stompPrincipalName";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String principalName = UUID.randomUUID().toString();
        attributes.put(PRINCIPAL_NAME_ATTR, principalName);
        if (request instanceof ServletServerHttpRequest servletRequest) {
            log.debug("STOMP 握手 principal={}, remote={}",
                    principalName, servletRequest.getServletRequest().getRemoteAddr());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
