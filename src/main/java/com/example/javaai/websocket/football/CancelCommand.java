package com.example.javaai.websocket.football;

/**
 * 客户端取消分析。
 */
public record CancelCommand(
        String requestId,
        String reason) {
}
