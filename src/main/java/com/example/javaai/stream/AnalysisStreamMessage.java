package com.example.javaai.stream;

import java.util.Map;

/**
 * 统一流式 JSON Envelope（STOMP 主推，SSE 结构化模式可复用）。
 */
public record AnalysisStreamMessage(
        String requestId,
        AnalysisStreamMessageType type,
        long timestamp,
        String node,
        Integer stage,
        Integer totalStages,
        String content,
        Map<String, Object> state,
        String error,
        Map<String, Object> meta) {

    public static AnalysisStreamMessage of(String requestId, AnalysisStreamMessageType type) {
        return new AnalysisStreamMessage(
                requestId, type, System.currentTimeMillis(),
                null, null, null, null, null, null, null);
    }

    public static AnalysisStreamMessage progress(String requestId, String content) {
        return new AnalysisStreamMessage(
                requestId, AnalysisStreamMessageType.PROGRESS, System.currentTimeMillis(),
                null, null, null, content, null, null, null);
    }

    public static AnalysisStreamMessage error(String requestId, String message) {
        return new AnalysisStreamMessage(
                requestId, AnalysisStreamMessageType.ERROR, System.currentTimeMillis(),
                null, null, null, null, null, message, null);
    }

    public static AnalysisStreamMessage done(String requestId, Map<String, Object> state) {
        return new AnalysisStreamMessage(
                requestId, AnalysisStreamMessageType.DONE, System.currentTimeMillis(),
                null, null, null, null, state, null, null);
    }

    public AnalysisStreamMessage withNode(String nodeName) {
        return new AnalysisStreamMessage(
                requestId, type, timestamp, nodeName, stage, totalStages, content, state, error, meta);
    }

    public AnalysisStreamMessage withContent(String text) {
        return new AnalysisStreamMessage(
                requestId, type, timestamp, node, stage, totalStages, text, state, error, meta);
    }

    public AnalysisStreamMessage withMeta(Map<String, Object> metaMap) {
        return new AnalysisStreamMessage(
                requestId, type, timestamp, node, stage, totalStages, content, state, error, metaMap);
    }

    public AnalysisStreamMessage withStage(int stageNumber, int total) {
        return new AnalysisStreamMessage(
                requestId, type, timestamp, node, stageNumber, total, content, state, error, meta);
    }
}
