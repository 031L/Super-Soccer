package com.example.javaai.agent.football.graph;

import java.util.function.Consumer;

/**
 * 图执行期间的 SSE / 日志进度回调（线程局部变量，避免写入图状态）。
 */
public final class FootballGraphProgress {

    private static final ThreadLocal<Consumer<String>> SINK = new ThreadLocal<>();

    private FootballGraphProgress() {
    }

    public static void bind(Consumer<String> sink) {
        SINK.set(sink);
    }

    public static void emit(String message) {
        Consumer<String> sink = SINK.get();
        if (sink != null && message != null) {
            sink.accept(message);
        }
    }

    public static void clear() {
        SINK.remove();
    }
}
