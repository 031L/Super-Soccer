package com.example.javaai.agent.football;

/**
 * 足球多 Agent SSE 流式输出常量（4 阶段 + thinking 总耗时可能超过 5 分钟）。
 */
public final class FootballSseConstants {

    /** SSE / 异步请求总超时：20 分钟 */
    public static final long TIMEOUT_MS = 1_200_000L;

    /** 心跳间隔（秒），LLM 推理期间维持连接 */
    public static final long HEARTBEAT_INTERVAL_SEC = 10L;

    /** 单次 send 最大字符数，避免超大 payload 导致客户端缓冲异常 */
    public static final int CHUNK_SIZE = 4_096;

    private FootballSseConstants() {
    }
}
