package com.example.javaai.agent.football.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.FootballAgentFactory;
import com.example.javaai.agent.football.graph.FootballAgentNodeExecutor;
import com.example.javaai.agent.football.graph.FootballAgentTaskBuilder;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphNodeNames;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SimulationAgentNode implements NodeAction {

    private final FootballAgentFactory agentFactory;
    private final FootballAgentTaskBuilder taskBuilder;
    private final FootballAgentNodeExecutor nodeExecutor;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        return FootballGraphProgress.runWithSession(state, () -> {
            FootballGraphProgress.emitStageStart(FootballGraphNodeNames.SIMULATION_AGENT, 3, 4);
            FootballGraphProgress.emit("\n\n---------- 阶段 3/4：推演 Agent ----------\n");
            var simulationAgent = agentFactory.createSimulationAgent();
            String output = nodeExecutor.runStage(
                    null, simulationAgent, taskBuilder.buildSimulationTask(state), "推演 Agent", state);
            FootballGraphProgress.emitStageOutput(FootballGraphNodeNames.SIMULATION_AGENT, "simulation", output);
            return Map.of(FootballGraphKeys.SIMULATION_ANALYSIS, output);
        });
    }
}
