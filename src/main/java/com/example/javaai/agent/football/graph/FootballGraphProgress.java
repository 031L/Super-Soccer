package com.example.javaai.agent.football.graph;

import com.example.javaai.agent.football.graph.api.GraphStreamEvent;

import java.util.function.Consumer;

/**
 * 图执行期间的 SSE / 日志进度回调（线程局部变量，避免写入图状态）。
 */
public final class FootballGraphProgress {

    private static final ThreadLocal<Consumer<String>> TEXT_SINK = new ThreadLocal<>();
    private static final ThreadLocal<Consumer<GraphStreamEvent>> EVENT_SINK = new ThreadLocal<>();

    private FootballGraphProgress() {
    }

    public static void bind(Consumer<String> sink) {
        bind(sink, null);
    }

    public static void bind(Consumer<String> textSink, Consumer<GraphStreamEvent> eventSink) {
        TEXT_SINK.set(textSink);
        EVENT_SINK.set(eventSink);
    }

    public static void emit(String message) {
        if (message == null) {
            return;
        }
        Consumer<String> textSink = TEXT_SINK.get();
        if (textSink != null) {
            textSink.accept(message);
        }
        Consumer<GraphStreamEvent> eventSink = EVENT_SINK.get();
        if (eventSink != null) {
            eventSink.accept(GraphStreamEvent.progress(message));
        }
    }

    public static void clear() {
        TEXT_SINK.remove();
        EVENT_SINK.remove();
    }
}
