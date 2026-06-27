package com.example.javaai.stream;

/**
 * 分析流水线流式推送抽象（SSE / STOMP 共用）。
 */
public interface AnalysisStreamSink {

    String requestId();

    boolean isOpen();

    void sendProgress(String content);

    void sendEvent(AnalysisStreamMessage message);

    void complete();

    void completeWithError(Throwable error);
}
