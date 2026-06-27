package com.example.javaai.agent.football.graph;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.javaai.agent.football.FootballAgentContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StateGraph 状态与业务上下文之间的转换辅助。
 */
public final class FootballGraphStateHelper {

    private FootballGraphStateHelper() {
    }

    public static Map<String, Object> toInitialState(FootballAgentContext context) {
        Map<String, Object> state = new HashMap<>();
        state.put(FootballGraphKeys.USER_QUERY, context.getUserQuery());
        state.put(FootballGraphKeys.MATCH_ID, nullToEmpty(context.getMatchId()));
        state.put(FootballGraphKeys.REDIS_RAW_DATA, nullToEmpty(context.getRedisRawData()));
        return state;
    }

    public static FootballAgentContext toContext(OverAllState state) {
        FootballAgentContext context = new FootballAgentContext(
                stringValue(state, FootballGraphKeys.USER_QUERY),
                blankToNull(stringValue(state, FootballGraphKeys.MATCH_ID)),
                blankToNull(stringValue(state, FootballGraphKeys.REDIS_RAW_DATA)));
        context.setDataAnalysis(stringValue(state, FootballGraphKeys.DATA_ANALYSIS));
        context.setSimulationAnalysis(stringValue(state, FootballGraphKeys.SIMULATION_ANALYSIS));
        context.setTacticalAnalysis(stringValue(state, FootballGraphKeys.TACTICAL_ANALYSIS));
        context.setFinalReport(stringValue(state, FootballGraphKeys.FINAL_REPORT));
        context.setIntent(blankToNull(stringValue(state, FootballGraphKeys.INTENT)));
        return context;
    }

    /**
     * 提取前端可见的状态快照（不含 Redis 原始 JSON，避免体积过大）。
     */
    public static Map<String, Object> toStateSnapshot(OverAllState state) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("userQuery", stringValue(state, FootballGraphKeys.USER_QUERY));
        snapshot.put("matchId", blankToNull(stringValue(state, FootballGraphKeys.MATCH_ID)));
        snapshot.put("hasRedisData", hasRedisMatchData(state));
        snapshot.put("intent", blankToNull(stringValue(state, FootballGraphKeys.INTENT)));
        putIfNotBlank(snapshot, "dataAnalysis", state, FootballGraphKeys.DATA_ANALYSIS);
        putIfNotBlank(snapshot, "simulationAnalysis", state, FootballGraphKeys.SIMULATION_ANALYSIS);
        putIfNotBlank(snapshot, "tacticalAnalysis", state, FootballGraphKeys.TACTICAL_ANALYSIS);
        putIfNotBlank(snapshot, "finalReport", state, FootballGraphKeys.FINAL_REPORT);
        return snapshot;
    }

    private static void putIfNotBlank(Map<String, Object> target, String key, OverAllState state, String stateKey) {
        String value = stringValue(state, stateKey);
        if (StrUtil.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    public static String stringValue(OverAllState state, String key) {
        return state.value(key, "").toString();
    }

    public static boolean hasRedisMatchData(OverAllState state) {
        return StrUtil.isNotBlank(stringValue(state, FootballGraphKeys.REDIS_RAW_DATA));
    }

    public static boolean hasMatchId(OverAllState state) {
        return StrUtil.isNotBlank(stringValue(state, FootballGraphKeys.MATCH_ID));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }
}
