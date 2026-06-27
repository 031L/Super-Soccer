package com.example.javaai.websocket.football;

/**
 * 客户端发起分析。
 */
public record AnalyzeCommand(
        String requestId,
        String message,
        String matchId) {
}
