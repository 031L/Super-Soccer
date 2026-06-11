package com.example.javaai.agent.football.graph.api;

import java.util.List;
import java.util.Map;

/**
 * StateGraph 工作流元信息，供前端展示流程图与节点说明。
 */
public record GraphWorkflowInfo(
        String graphName,
        List<String> nodes,
        Map<String, List<String>> routes,
        List<String> matchAnalysisPipeline) {

    public static GraphWorkflowInfo football() {
        return new GraphWorkflowInfo(
                "football-multi-agent",
                List.of(
                        "prepare_context",
                        "classify_intent",
                        "data_agent",
                        "simulation_agent",
                        "tactical_agent",
                        "synthesis_agent",
                        "general_qa_agent"),
                Map.of(
                        "MATCH_ANALYSIS", List.of("data_agent", "simulation_agent", "tactical_agent", "synthesis_agent"),
                        "GENERAL_QA", List.of("general_qa_agent")),
                List.of("prepare_context", "classify_intent", "data_agent", "simulation_agent", "tactical_agent", "synthesis_agent"));
    }
}
