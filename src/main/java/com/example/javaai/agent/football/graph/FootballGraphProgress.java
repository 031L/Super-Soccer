package com.example.javaai.agent.football.graph;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.javaai.agent.football.graph.api.GraphStreamEvent;
import com.example.javaai.stream.AnalysisStreamMessage;
import com.example.javaai.stream.AnalysisStreamMessageMapper;
import com.example.javaai.stream.AnalysisStreamMessageType;
import com.example.javaai.stream.AnalysisStreamSink;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 图执行期间的流式进度回调（SSE / STOMP 共用）。
 * <p>
 * 使用 sessionId 跨异步图节点线程传播 sink，避免 ThreadLocal 在 node_async 线程池中失效。
 */
public final class FootballGraphProgress {

    private record Session(
            AnalysisStreamSink sink,
            Consumer<String> textSink,
            Consumer<GraphStreamEvent> eventSink) {
    }

    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_SESSION = new ThreadLocal<>();

    private static final ThreadLocal<Consumer<String>> LEGACY_TEXT_SINK = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<GraphStreamEvent>> LEGACY_EVENT_SINK = new ThreadLocal<>();

    private FootballGraphProgress() {
    }

    public static void bind(Consumer<String> sink) {
        bind(sink, null);
    }

    public static void bind(Consumer<String> textSink, Consumer<GraphStreamEvent> eventSink) {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId == null) {
            if (textSink != null) {
                LEGACY_TEXT_SINK.set(textSink);
            }
            if (eventSink != null) {
                LEGACY_EVENT_SINK.set(eventSink);
            }
            return;
        }
        SESSIONS.put(sessionId, new Session(null, textSink, eventSink));
    }

    public static void bindSession(String sessionId, AnalysisStreamSink sink) {
        if (StrUtil.isBlank(sessionId) || sink == null) {
            return;
        }
        SESSIONS.put(sessionId, new Session(sink, null, null));
        CURRENT_SESSION.set(sessionId);
    }

    public static void bindSession(String sessionId, Consumer<String> textSink, Consumer<GraphStreamEvent> eventSink) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        SESSIONS.put(sessionId, new Session(null, textSink, eventSink));
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
        AnalysisStreamSink sink = resolveSink();
        if (sink != null) {
            sink.sendProgress(message);
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

    public static void emitGraphEvent(GraphStreamEvent event) {
        if (event == null) {
            return;
        }
        AnalysisStreamSink sink = resolveSink();
        if (sink != null) {
            sink.sendEvent(AnalysisStreamMessageMapper.fromGraphEvent(sink.requestId(), event));
            return;
        }
        Consumer<GraphStreamEvent> eventSink = resolveEventSink();
        if (eventSink != null) {
            eventSink.accept(event);
        }
    }

    public static void emitStageStart(String node, int stage, int totalStages) {
        AnalysisStreamSink sink = resolveSink();
        if (sink != null && sink.isOpen() && sink.requestId() != null) {
            sink.sendEvent(AnalysisStreamMessage
                    .of(sink.requestId(), AnalysisStreamMessageType.STAGE_START)
                    .withNode(node)
                    .withStage(stage, totalStages));
        }
    }

    public static void emitStageOutput(String node, String stageKey, String fullOutput) {
        String forDisplay = FootballSseOutputPolicy.forSse(stageKey, fullOutput);
        AnalysisStreamSink sink = resolveSink();
        if (sink != null && sink.isOpen()) {
            if (sink.requestId() != null) {
                AnalysisStreamMessageMapper.sendChunked(sink, node, forDisplay);
                sink.sendEvent(AnalysisStreamMessage
                        .of(sink.requestId(), AnalysisStreamMessageType.STAGE_COMPLETE)
                        .withNode(node)
                        .withContent(forDisplay)
                        .withMeta(Map.of(
                                "charCount", fullOutput != null ? fullOutput.length() : 0,
                                "truncatedForDisplay", forDisplay != null && fullOutput != null
                                        && forDisplay.length() < fullOutput.length())));
            } else {
                sink.sendProgress(forDisplay);
            }
            return;
        }
        emit(forDisplay);
    }

    private static AnalysisStreamSink resolveSink() {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId != null) {
            Session session = SESSIONS.get(sessionId);
            if (session != null && session.sink() != null) {
                return session.sink();
            }
        }
        return null;
    }

    private static Consumer<String> resolveTextSink() {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId != null) {
            Session session = SESSIONS.get(sessionId);
            if (session != null && session.textSink() != null) {
                return session.textSink();
            }
        }
        return LEGACY_TEXT_SINK.get();
    }

    private static Consumer<GraphStreamEvent> resolveEventSink() {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId != null) {
            Session session = SESSIONS.get(sessionId);
            if (session != null && session.eventSink() != null) {
                return session.eventSink();
            }
        }
        return LEGACY_EVENT_SINK.get();
    }
}
