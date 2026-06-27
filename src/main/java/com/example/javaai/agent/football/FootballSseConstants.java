package com.example.javaai.agent.football;

import com.example.javaai.stream.AnalysisStreamConstants;

/**
 * @deprecated 使用 {@link com.example.javaai.stream.AnalysisStreamConstants}
 */
@Deprecated
public final class FootballSseConstants {

    public static final long TIMEOUT_MS = AnalysisStreamConstants.TIMEOUT_MS;
    public static final long HEARTBEAT_INTERVAL_SEC = AnalysisStreamConstants.HEARTBEAT_INTERVAL_SEC;
    public static final int CHUNK_SIZE = AnalysisStreamConstants.CHUNK_SIZE;

    private FootballSseConstants() {
    }
}
