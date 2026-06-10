package com.example.javaai.agent.football.graph;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.javaai.agent.football.FootballAgentContext;

import java.util.HashMap;
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
        return context;
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
