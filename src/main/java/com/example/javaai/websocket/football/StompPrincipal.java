package com.example.javaai.websocket.football;

import java.security.Principal;

/**
 * STOMP 用户身份（v1：每个 SockJS 连接一个匿名 sessionId）。
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
