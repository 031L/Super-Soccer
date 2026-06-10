package com.example.javaai.agent;

import com.example.javaai.agent.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Agent 串行执行协调器：确保当前 Agent 在上一个 Agent 执行完成后才开始运行。
 */
@Component
@Slf4j
public class AgentExecutionCoordinator {

    private static final long DEFAULT_WAIT_TIMEOUT_MS = 300_000L;

    /**
     * 等待上一个 Agent 进入终态（FINISHED / ERROR），再同步执行当前 Agent。
     *
     * @param previousAgent 上一个 Agent，首个阶段可传 null
     * @param currentAgent  当前待执行的 Agent
     * @param task          当前 Agent 的任务输入
     * @param stageLabel    阶段名称，用于日志与进度输出
     * @param progressSink  可选的进度回调（流式输出时使用）
     * @return Agent.run 的原始返回结果
     */
    public String runAfterPrevious(
            BaseAgent previousAgent,
            BaseAgent currentAgent,
            String task,
            String stageLabel,
            Consumer<String> progressSink) {
        waitForPreviousAgent(previousAgent, currentAgent.getName(), progressSink);
        currentAgent.markWaiting();
        emit(progressSink, "【编排器】" + stageLabel + " 开始执行");
        try {
            String result = currentAgent.run(task);
            currentAgent.awaitCompletion(DEFAULT_WAIT_TIMEOUT_MS);
            if (currentAgent.getState() == AgentState.ERROR) {
                throw new AgentExecutionException(
                        stageLabel + " 执行失败，流水线已终止");
            }
            emit(progressSink, "【编排器】" + stageLabel + " 执行完成");
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            currentAgent.setState(AgentState.ERROR);
            throw new AgentExecutionException(stageLabel + " 等待执行结果被中断", e);
        }
    }

  public String runAfterPrevious(
            BaseAgent previousAgent,
            BaseAgent currentAgent,
            String task,
            String stageLabel) {
        return runAfterPrevious(previousAgent, currentAgent, task, stageLabel, null);
    }

    private void waitForPreviousAgent(
            BaseAgent previousAgent,
            String nextAgentName,
            Consumer<String> progressSink) {
        if (previousAgent == null) {
            return;
        }
        emit(progressSink, "【编排器】" + nextAgentName + " 等待上一个 Agent（"
                + previousAgent.getName() + "）执行完成...");
        log.info("{} 等待 {} 完成", nextAgentName, previousAgent.getName());
        try {
            boolean completed = previousAgent.awaitCompletion(DEFAULT_WAIT_TIMEOUT_MS);
            if (!completed) {
                throw new AgentExecutionException(
                        "等待 " + previousAgent.getName() + " 完成超时，无法启动 " + nextAgentName);
            }
            if (previousAgent.getState() == AgentState.ERROR) {
                throw new AgentExecutionException(
                        previousAgent.getName() + " 执行失败，无法启动 " + nextAgentName);
            }
            if (!previousAgent.isTerminalState()) {
                throw new AgentExecutionException(
                        previousAgent.getName() + " 未进入终态（当前: "
                                + previousAgent.getState() + "），无法启动 " + nextAgentName);
            }
            log.info("{} 已确认 {} 执行完成，状态: {}",
                    nextAgentName, previousAgent.getName(), previousAgent.getState());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentExecutionException(
                    "等待 " + previousAgent.getName() + " 完成时被中断", e);
        }
    }

    private void emit(Consumer<String> sink, String message) {
        if (sink != null && message != null) {
            sink.accept(message);
        }
    }
}
