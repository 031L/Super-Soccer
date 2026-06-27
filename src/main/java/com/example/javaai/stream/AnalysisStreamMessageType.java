package com.example.javaai.stream;

/**
 * STOMP / 结构化流式消息类型。
 */
public enum AnalysisStreamMessageType {

    CONNECTED,
    PROGRESS,
    STAGE_START,
    CHUNK,
    STAGE_COMPLETE,
    NODE_COMPLETE,
    PING,
    PONG,
    CANCELLED,
    DONE,
    ERROR
}
