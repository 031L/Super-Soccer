package com.example.javaai.agent.football.graph;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.javaai.agent.football.graph.api.GraphStreamEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 图执行期间的 SSE / 日志进度回调。
 * <p>
 * 使用 sessionId 跨异步图节点线程传播 sink，避免 ThreadLocal 在 node_async 线程池中失效。
 */
public final class FootballGraphProgress {

    private record Session(Consumer<String> textSink, Consumer<GraphStreamEvent> eventSink) {
    }

    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_SESSION = new ThreadLocal<>();

    private FootballGraphProgress() {
    }

    public static void bind(Consumer<String> sink) {
        bind(sink, null);
    }

    public static void bind(Consumer<String> textSink, Consumer<GraphStreamEvent> eventSink) {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId == null) {
            // 非图内调用（如 GeneralQaOrchestrator 直连）：退化为 ThreadLocal 单 sink
            if (textSink != null) {
                LEGACY_TEXT_SINK.set(textSink);
            }
            if (eventSink != null) {
                LEGACY_EVENT_SINK.set(eventSink);
            }
            return;
        }
        SESSIONS.put(sessionId, new Session(textSink, eventSink));
    }

    /** 非图场景下的 ThreadLocal 回退 */
    private static final ThreadLocal<Consumer<String>> LEGACY_TEXT_SINK = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<GraphStreamEvent>> LEGACY_EVENT_SINK = new ThreadLocal<>();

    public static void bindSession(String sessionId, Consumer<String> textSink, Consumer<GraphStreamEvent> eventSink) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        SESSIONS.put(sessionId, new Session(textSink, eventSink));
        CURRENT_SESSION.set(sessionId);
    }

    public static void enterSession(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            CURRENT_SESSION.set(sessionId);
        }
    }

    public static void leaveSession() {
        CURRENT_SESSION.remove();
    }

    public static void clearSession(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            SESSIONS.remove(sessionId);
        }
        CURRENT_SESSION.remove();
        LEGACY_TEXT_SINK.remove();
        LEGACY_EVENT_SINK.remove();
    }

    public static void clear() {
        CURRENT_SESSION.remove();
        LEGACY_TEXT_SINK.remove();
        LEGACY_EVENT_SINK.remove();
    }

    /**
     * 在图节点线程中绑定 session 并执行逻辑。
     */
    public static <T> T runWithSession(OverAllState state, Supplier<T> action) {
        String sessionId = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.STREAM_SESSION_ID);
        if (StrUtil.isBlank(sessionId)) {
            return action.get();
        }
        enterSession(sessionId);
        try {
            return action.get();
        } finally {
            leaveSession();
        }
    }

    public static void emit(String message) {
        if (message == null) {
            return;
        }
        Consumer<String> textSink = resolveTextSink();
        if (textSink != null) {
            textSink.accept(message);
        }
        Consumer<GraphStreamEvent> eventSink = resolveEventSink();
        if (eventSink != null) {
            eventSink.accept(GraphStreamEvent.progress(message));
        }
    }

    private static Consumer<String> resolveTextSink() {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId != null) {
            Session session = SESSIONS.get(sessionId);
            if (session != null) {
                return session.textSink();
            }
        }
        return LEGACY_TEXT_SINK.get();
    }

    private static Consumer<GraphStreamEvent> resolveEventSink() {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId != null) {
            Session session = SESSIONS.get(sessionId);
            if (session != null) {
                return session.eventSink();
            }
        }
        return LEGACY_EVENT_SINK.get();
    }
}
