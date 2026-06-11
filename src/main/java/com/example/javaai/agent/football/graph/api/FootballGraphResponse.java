package com.example.javaai.agent.football.graph.api;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.javaai.agent.football.FootballAgentContext;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;

/**
 * StateGraph 同步调用完整响应，包含各节点产出。
 */
public record FootballGraphResponse(
        String userQuery,
        String matchId,
        boolean hasRedisData,
        String intent,
        String dataAnalysis,
        String simulationAnalysis,
        String tacticalAnalysis,
        String finalReport) {

    public static FootballGraphResponse fromContext(FootballAgentContext context) {
        return new FootballGraphResponse(
                context.getUserQuery(),
                context.getMatchId(),
                context.hasRedisMatchData(),
                context.getIntent(),
                context.getDataAnalysis(),
                context.getSimulationAnalysis(),
                context.getTacticalAnalysis(),
                context.getFinalReport());
    }

    public static FootballGraphResponse fromState(OverAllState state) {
        return fromContext(FootballGraphStateHelper.toContext(state));
    }
}
