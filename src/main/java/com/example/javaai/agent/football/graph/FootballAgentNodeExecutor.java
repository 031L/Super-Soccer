package com.example.javaai.agent.football.graph;

import com.example.javaai.agent.AgentExecutionCoordinator;
import com.example.javaai.agent.BaseAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 在 StateGraph 节点中执行 Agent，复用串行协调器。
 */
@Component
@RequiredArgsConstructor
public class FootballAgentNodeExecutor {

    private final AgentExecutionCoordinator executionCoordinator;
    private final FootballAgentOutputExtractor outputExtractor;

    public String runStage(BaseAgent previousAgent,
                           BaseAgent currentAgent,
                           String task,
                           String stageLabel) {
        FootballGraphProgress.emit("【编排器】" + stageLabel + " 开始执行");
        String runResult = executionCoordinator.runAfterPrevious(
                previousAgent, currentAgent, task, stageLabel, FootballGraphProgress::emit);
        String output = outputExtractor.extract(currentAgent, runResult);
        FootballGraphProgress.emit("【编排器】" + stageLabel + " 执行完成");
        return output;
    }
}
