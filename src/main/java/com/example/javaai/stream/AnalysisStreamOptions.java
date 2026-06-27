package com.example.javaai.stream;

/**
 * 分析流式执行参数。
 */
public record AnalysisStreamOptions(
        String requestId,
        String message,
        String matchId,
        boolean structuredOnly) {

    public static AnalysisStreamOptions textStream(String message, String matchId) {
        return new AnalysisStreamOptions(null, message, matchId, false);
    }

    public static AnalysisStreamOptions structuredStream(String message, String matchId) {
        return new AnalysisStreamOptions(null, message, matchId, true);
    }

    public static AnalysisStreamOptions stompAnalyze(String requestId, String message, String matchId) {
        return new AnalysisStreamOptions(requestId, message, matchId, true);
    }
}
