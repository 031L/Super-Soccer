package com.example.javaai.agent.football.graph.api;

import java.util.Map;

/**
 * StateGraph 流式 SSE 事件体（JSON）。
 */
public record GraphStreamEvent(
        GraphStreamEventType type,
        String node,
        String content,
        Map<String, Object> state) {

    public static GraphStreamEvent progress(String content) {
        return new GraphStreamEvent(GraphStreamEventType.PROGRESS, null, content, null);
    }

    public static GraphStreamEvent nodeComplete(String node, Map<String, Object> state) {
        return new GraphStreamEvent(GraphStreamEventType.NODE_COMPLETE, node, null, state);
    }

    public static GraphStreamEvent done(Map<String, Object> state) {
        return new GraphStreamEvent(GraphStreamEventType.DONE, null, null, state);
    }

    public static GraphStreamEvent error(String message) {
        return new GraphStreamEvent(GraphStreamEventType.ERROR, null, message, null);
    }
}
