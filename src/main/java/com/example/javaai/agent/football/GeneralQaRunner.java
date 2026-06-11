package com.example.javaai.agent.football;

import com.example.javaai.agent.football.graph.FootballAgentNodeExecutor;
import com.example.javaai.agent.football.graph.FootballAgentTaskBuilder;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.rag.KnowledgeRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 通用问答 Agent 执行器：供 StateGraph 节点与独立 API 共用。
 */
@Component
@RequiredArgsConstructor
public class GeneralQaRunner {

    private final FootballAgentFactory agentFactory;
    private final FootballAgentTaskBuilder taskBuilder;
    private final FootballAgentNodeExecutor nodeExecutor;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public String run(String userQuery, Consumer<String> progressSink) {
        // 图内调用时 progressSink 为 null，沿用编排器已绑定的 ThreadLocal，避免 bind(emit) 自递归
        if (progressSink != null) {
            FootballGraphProgress.bind(progressSink);
        }
        try {
            Consumer<String> sink = progressSink != null ? progressSink : FootballGraphProgress::emit;
            return runInternal(userQuery, sink);
        } finally {
            if (progressSink != null) {
                FootballGraphProgress.clear();
            }
        }
    }

    private String runInternal(String userQuery, Consumer<String> progressSink) {
        emit(progressSink, "\n\n---------- 通用问答 Agent ----------\n");
        Optional<String> knowledgeContext = knowledgeRetrievalService.retrieveContext(userQuery);
        if (knowledgeContext.isPresent()) {
            emit(progressSink, "【编排器】已注入知识库参考内容\n");
        } else {
            emit(progressSink, "【编排器】未命中知识库，将依赖搜索工具补充\n");
        }
        var generalAgent = agentFactory.createGeneralAgent();
        String output = nodeExecutor.runStage(
                null,
                generalAgent,
                taskBuilder.buildGeneralQaTask(userQuery, knowledgeContext),
                "通用 Agent");
        emit(progressSink, output);
        return output;
    }

    private void emit(Consumer<String> sink, String message) {
        if (sink != null && message != null) {
            sink.accept(message);
        }
    }
}
