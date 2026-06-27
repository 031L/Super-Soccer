package com.example.javaai.stream;

import com.example.javaai.agent.football.graph.api.GraphStreamEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 实现的 {@link AnalysisStreamSink}。
 */
@Slf4j
public class SseAnalysisStreamSink implements AnalysisStreamSink {

    private final SseEmitter emitter;
    private final String requestId;
    private final boolean structuredOnly;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SseAnalysisStreamSink(SseEmitter emitter,
                                 String requestId,
                                 boolean structuredOnly,
                                 ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.requestId = requestId;
        this.structuredOnly = structuredOnly;
        this.objectMapper = objectMapper;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public void sendProgress(String content) {
        if (!isOpen() || content == null) {
            return;
        }
        if (structuredOnly) {
            sendEvent(AnalysisStreamMessage.progress(requestId, content));
            return;
        }
        safeSendText(content);
    }

    @Override
    public void sendEvent(AnalysisStreamMessage message) {
        if (!isOpen() || message == null) {
            return;
        }
        if (structuredOnly) {
            safeSendStructured(message);
            return;
        }
        if (message.content() != null) {
            safeSendText(message.content());
        }
    }

    @Override
    public void complete() {
        if (closed.compareAndSet(false, true)) {
            try {
                emitter.complete();
            } catch (IllegalStateException e) {
                log.warn("SSE 已完成或已关闭，跳过 complete: {}", e.getMessage());
            }
        }
    }

    @Override
    public void completeWithError(Throwable error) {
        if (closed.get()) {
            return;
        }
        try {
            emitter.completeWithError(error);
        } catch (IllegalStateException e) {
            log.warn("SSE 已完成或已关闭，跳过 completeWithError: {}", e.getMessage());
        } finally {
            closed.set(true);
        }
    }

    public void sendKeepalive() {
        if (!isOpen()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException | IllegalStateException e) {
            closed.set(true);
            log.debug("SSE keepalive 失败: {}", e.getMessage());
        }
    }

    private void safeSendText(String event) {
        try {
            int chunkSize = AnalysisStreamConstants.CHUNK_SIZE;
            if (event.length() <= chunkSize) {
                emitter.send(event);
                return;
            }
            for (int offset = 0; offset < event.length(); offset += chunkSize) {
                if (closed.get()) {
                    return;
                }
                int end = Math.min(offset + chunkSize, event.length());
                emitter.send(event.substring(offset, end));
            }
        } catch (IOException | IllegalStateException e) {
            closed.set(true);
            log.warn("SSE 连接已关闭，停止向前端推送: {}", e.getMessage());
        }
    }

    private void safeSendStructured(AnalysisStreamMessage message) {
        try {
            GraphStreamEvent legacy = toLegacyEvent(message);
            emitter.send(SseEmitter.event()
                    .name(legacy.type().name())
                    .data(objectMapper.writeValueAsString(legacy)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化流式事件失败", e);
        } catch (IOException | IllegalStateException e) {
            closed.set(true);
            log.warn("SSE 连接已关闭，停止向前端推送: {}", e.getMessage());
        }
    }

    private GraphStreamEvent toLegacyEvent(AnalysisStreamMessage message) {
        return switch (message.type()) {
            case PROGRESS -> GraphStreamEvent.progress(message.content());
            case NODE_COMPLETE -> GraphStreamEvent.nodeComplete(message.node(), message.state());
            case DONE -> GraphStreamEvent.done(message.state());
            case ERROR -> GraphStreamEvent.error(message.error() != null ? message.error() : message.content());
            default -> GraphStreamEvent.progress(message.content());
        };
    }
}
