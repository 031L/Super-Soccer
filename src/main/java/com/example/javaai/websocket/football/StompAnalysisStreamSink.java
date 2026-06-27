package com.example.javaai.websocket.football;

import com.example.javaai.stream.AnalysisStreamMessage;
import com.example.javaai.stream.AnalysisStreamSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * STOMP 实现的 {@link AnalysisStreamSink}。
 */
@Slf4j
public class StompAnalysisStreamSink implements AnalysisStreamSink {

    private final SimpMessagingTemplate messagingTemplate;
    private final String principalName;
    private final String requestId;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public StompAnalysisStreamSink(SimpMessagingTemplate messagingTemplate,
                                   String principalName,
                                   String requestId) {
        this.messagingTemplate = messagingTemplate;
        this.principalName = principalName;
        this.requestId = requestId;
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
        sendEvent(AnalysisStreamMessage.progress(requestId, content));
    }

    @Override
    public void sendEvent(AnalysisStreamMessage message) {
        if (!isOpen() || message == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(
                    principalName,
                    destination(),
                    message);
        } catch (Exception e) {
            closed.set(true);
            log.warn("STOMP 推送失败 requestId={}: {}", requestId, e.getMessage());
        }
    }

    @Override
    public void complete() {
        closed.set(true);
    }

    @Override
    public void completeWithError(Throwable error) {
        if (isOpen()) {
            sendEvent(AnalysisStreamMessage.error(requestId,
                    error != null ? error.getMessage() : "unknown error"));
        }
        closed.set(true);
    }

    private String destination() {
        return "/queue/football/analysis/" + requestId;
    }
}
