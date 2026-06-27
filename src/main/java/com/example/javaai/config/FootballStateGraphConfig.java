package com.example.javaai.config;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphNodeNames;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import com.example.javaai.agent.football.graph.FootballIntentType;
import com.example.javaai.agent.football.graph.node.ClassifyIntentNode;
import com.example.javaai.agent.football.graph.node.DataAgentNode;
import com.example.javaai.agent.football.graph.node.GeneralQaAgentNode;
import com.example.javaai.agent.football.graph.node.PrepareContextNode;
import com.example.javaai.agent.football.graph.node.SimulationAgentNode;
import com.example.javaai.agent.football.graph.node.SynthesisAgentNode;
import com.example.javaai.agent.football.graph.node.TacticalAgentNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 足球多智能体 StateGraph 工作流配置。
 *
 */
@Configuration
public class FootballStateGraphConfig {

    @Bean
    public CompiledGraph footballCompiledGraph(
            PrepareContextNode prepareContextNode,
            ClassifyIntentNode classifyIntentNode,
            DataAgentNode dataAgentNode,
            SimulationAgentNode simulationAgentNode,
            TacticalAgentNode tacticalAgentNode,
            SynthesisAgentNode synthesisAgentNode,
            GeneralQaAgentNode generalQaAgentNode) throws GraphStateException {

        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(FootballGraphKeys.USER_QUERY, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.MATCH_ID, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.REDIS_RAW_DATA, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.INTENT, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.DATA_ANALYSIS, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.SIMULATION_ANALYSIS, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.TACTICAL_ANALYSIS, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.FINAL_REPORT, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.STREAM_SESSION_ID, new ReplaceStrategy());
            strategies.put(FootballGraphKeys.REQUEST_ID, new ReplaceStrategy());
            return strategies;
        };

        StateGraph graph = new StateGraph("football-multi-agent", keyStrategyFactory)
                .addNode(FootballGraphNodeNames.PREPARE_CONTEXT, node_async(prepareContextNode))
                .addNode(FootballGraphNodeNames.CLASSIFY_INTENT, node_async(classifyIntentNode))
                .addNode(FootballGraphNodeNames.DATA_AGENT, node_async(dataAgentNode))
                .addNode(FootballGraphNodeNames.SIMULATION_AGENT, node_async(simulationAgentNode))
                .addNode(FootballGraphNodeNames.TACTICAL_AGENT, node_async(tacticalAgentNode))
                .addNode(FootballGraphNodeNames.SYNTHESIS_AGENT, node_async(synthesisAgentNode))
                .addNode(FootballGraphNodeNames.GENERAL_QA_AGENT, node_async(generalQaAgentNode));

        graph.addEdge(START, FootballGraphNodeNames.PREPARE_CONTEXT);
        graph.addEdge(FootballGraphNodeNames.PREPARE_CONTEXT, FootballGraphNodeNames.CLASSIFY_INTENT);

        graph.addConditionalEdges(
                FootballGraphNodeNames.CLASSIFY_INTENT,
                edge_async(state -> FootballIntentType.fromValue(
                        FootballGraphStateHelper.stringValue(state, FootballGraphKeys.INTENT)).name()),
                Map.of(
                        FootballIntentType.MATCH_ANALYSIS.name(), FootballGraphNodeNames.DATA_AGENT,
                        FootballIntentType.GENERAL_QA.name(), FootballGraphNodeNames.GENERAL_QA_AGENT
                ));

        graph.addEdge(FootballGraphNodeNames.DATA_AGENT, FootballGraphNodeNames.TACTICAL_AGENT);
        graph.addEdge(FootballGraphNodeNames.TACTICAL_AGENT, FootballGraphNodeNames.SIMULATION_AGENT);
        graph.addEdge(FootballGraphNodeNames.SIMULATION_AGENT, FootballGraphNodeNames.SYNTHESIS_AGENT);
        graph.addEdge(FootballGraphNodeNames.SYNTHESIS_AGENT, END);
        graph.addEdge(FootballGraphNodeNames.GENERAL_QA_AGENT, END);

        return graph.compile(CompileConfig.builder().build());
    }
}
