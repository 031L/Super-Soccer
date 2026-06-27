package com.example.javaai.websocket.football;

import com.example.javaai.stream.AnalysisStreamMessage;
import com.example.javaai.stream.AnalysisStreamMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompAnalysisStreamSinkTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsToUserQueueDestination() {
        StompAnalysisStreamSink sink = new StompAnalysisStreamSink(
                messagingTemplate, "principal-1", "req-99");
        sink.sendEvent(AnalysisStreamMessage.of("req-99", AnalysisStreamMessageType.CONNECTED));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("principal-1"),
                eq("/queue/football/analysis/req-99"),
                payloadCaptor.capture());
        AnalysisStreamMessage sent = (AnalysisStreamMessage) payloadCaptor.getValue();
        assertEquals(AnalysisStreamMessageType.CONNECTED, sent.type());
        assertEquals("req-99", sent.requestId());
    }
}
