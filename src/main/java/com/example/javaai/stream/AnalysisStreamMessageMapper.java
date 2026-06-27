package com.example.javaai.stream;

import com.example.javaai.agent.football.graph.api.GraphStreamEvent;
import com.example.javaai.agent.football.graph.api.GraphStreamEventType;

import java.util.Map;

/**
 * GraphStreamEvent 与 AnalysisStreamMessage 互转。
 */
public final class AnalysisStreamMessageMapper {

    private AnalysisStreamMessageMapper() {
    }

    public static AnalysisStreamMessage fromGraphEvent(String requestId, GraphStreamEvent event) {
        if (event == null) {
            return null;
        }
        AnalysisStreamMessageType type = switch (event.type()) {
            case PROGRESS -> AnalysisStreamMessageType.PROGRESS;
            case NODE_COMPLETE -> AnalysisStreamMessageType.NODE_COMPLETE;
            case DONE -> AnalysisStreamMessageType.DONE;
            case ERROR -> AnalysisStreamMessageType.ERROR;
        };
        return new AnalysisStreamMessage(
                requestId,
                type,
                System.currentTimeMillis(),
                event.node(),
                null,
                null,
                event.content(),
                event.state(),
                event.type() == GraphStreamEventType.ERROR ? event.content() : null,
                null);
    }

    public static void sendChunked(AnalysisStreamSink sink, String node, String fullText) {
        if (sink == null || fullText == null || !sink.isOpen()) {
            return;
        }
        String requestId = sink.requestId();
        int chunkSize = AnalysisStreamConstants.CHUNK_SIZE;
        if (fullText.length() <= chunkSize) {
            sink.sendEvent(new AnalysisStreamMessage(
                    requestId, AnalysisStreamMessageType.CHUNK, System.currentTimeMillis(),
                    node, null, null, fullText, null, null, null));
            return;
        }
        int total = (fullText.length() + chunkSize - 1) / chunkSize;
        for (int i = 0, offset = 0; offset < fullText.length(); i++, offset += chunkSize) {
            if (!sink.isOpen()) {
                return;
            }
            int end = Math.min(offset + chunkSize, fullText.length());
            sink.sendEvent(new AnalysisStreamMessage(
                    requestId,
                    AnalysisStreamMessageType.CHUNK,
                    System.currentTimeMillis(),
                    node,
                    null,
                    null,
                    fullText.substring(offset, end),
                    null,
                    null,
                    Map.of("chunkIndex", i, "chunkTotal", total)));
        }
    }
}
