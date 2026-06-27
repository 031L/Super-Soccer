package com.example.javaai.stream;

/**
 * 分析流式输出常量（SSE / STOMP 共用）。
 */
public final class AnalysisStreamConstants {

    /** 流式任务总超时：20 分钟 */
    public static final long TIMEOUT_MS = 1_200_000L;

    /** 心跳间隔（秒） */
    public static final long HEARTBEAT_INTERVAL_SEC = 10L;

    /** 单次推送最大字符数 */
    public static final int CHUNK_SIZE = 4_096;

    /** 每 Principal 默认最大并发分析任务数 */
    public static final int MAX_ACTIVE_TASKS_PER_PRINCIPAL = 1;

    private AnalysisStreamConstants() {
    }
}
