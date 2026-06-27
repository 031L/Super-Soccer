package com.example.javaai.websocket.football;

/**
 * 客户端心跳。
 */
public record PingCommand(
        String requestId,
        Long timestamp) {
}
