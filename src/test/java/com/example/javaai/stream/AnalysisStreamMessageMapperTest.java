package com.example.javaai.stream;

import com.example.javaai.agent.football.graph.api.GraphStreamEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisStreamMessageMapperTest {

    @Test
    void mapsProgressGraphEvent() {
        GraphStreamEvent event = GraphStreamEvent.progress("hello");
        AnalysisStreamMessage message = AnalysisStreamMessageMapper.fromGraphEvent("req-1", event);

        assertEquals("req-1", message.requestId());
        assertEquals(AnalysisStreamMessageType.PROGRESS, message.type());
        assertEquals("hello", message.content());
    }

    @Test
    void mapsDoneGraphEvent() {
        GraphStreamEvent event = GraphStreamEvent.done(Map.of("finalReport", "report"));
        AnalysisStreamMessage message = AnalysisStreamMessageMapper.fromGraphEvent("req-2", event);

        assertEquals(AnalysisStreamMessageType.DONE, message.type());
        assertEquals("report", message.state().get("finalReport"));
    }

    @Test
    void sendChunkedSplitsLongText() {
        CollectingSink sink = new CollectingSink("req-3");
        String longText = "a".repeat(9000);
        AnalysisStreamMessageMapper.sendChunked(sink, "data_agent", longText);

        assertTrue(sink.chunkCount >= 3);
    }

    private static final class CollectingSink implements AnalysisStreamSink {
        private final String requestId;
        private int chunkCount;

        private CollectingSink(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public String requestId() {
            return requestId;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void sendProgress(String content) {
        }

        @Override
        public void sendEvent(AnalysisStreamMessage message) {
            if (message.type() == AnalysisStreamMessageType.CHUNK) {
                chunkCount++;
            }
        }

        @Override
        public void complete() {
        }

        @Override
        public void completeWithError(Throwable error) {
        }
    }
}
