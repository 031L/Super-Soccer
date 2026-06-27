package com.example.javaai.agent.football.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.javaai.agent.AgentExecutionCoordinator;
import com.example.javaai.agent.AgentExecutionException;
import com.example.javaai.agent.BaseAgent;
import com.example.javaai.websocket.football.AnalysisTaskRegistry;
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
    private final AnalysisTaskRegistry taskRegistry;

    public String runStage(BaseAgent previousAgent,
                           BaseAgent currentAgent,
                           String task,
                           String stageLabel) {
        return runStage(previousAgent, currentAgent, task, stageLabel, null);
    }

    public String runStage(BaseAgent previousAgent,
                           BaseAgent currentAgent,
                           String task,
                           String stageLabel,
                           OverAllState state) {
        ensureNotCancelled(state);
        FootballGraphProgress.emit("【编排器】" + stageLabel + " 开始执行");
        String runResult = executionCoordinator.runAfterPrevious(
                previousAgent, currentAgent, task, stageLabel, FootballGraphProgress::emit);
        ensureNotCancelled(state);
        String output = outputExtractor.extract(currentAgent, runResult);
        FootballGraphProgress.emit("【编排器】" + stageLabel + " 执行完成");
        return output;
    }

    private void ensureNotCancelled(OverAllState state) {
        if (state == null) {
            return;
        }
        String requestId = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.REQUEST_ID);
        if (taskRegistry.isCancelled(requestId)) {
            throw new AgentExecutionException("分析任务已取消");
        }
    }
}
