package com.example.javaai.agent.football;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.example.javaai.agent.AgentExecutionException;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import com.example.javaai.agent.football.graph.api.FootballGraphResponse;
import com.example.javaai.agent.football.graph.api.GraphStreamEvent;
import com.example.javaai.stream.AnalysisStreamConstants;
import com.example.javaai.stream.AnalysisStreamMessage;
import com.example.javaai.stream.AnalysisStreamMessageMapper;
import com.example.javaai.stream.AnalysisStreamMessageType;
import com.example.javaai.stream.AnalysisStreamOptions;
import com.example.javaai.stream.AnalysisStreamSink;
import com.example.javaai.stream.SseAnalysisStreamSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 足球多智能体编排器：基于 StateGraph 驱动协作。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FootballMultiAgentOrchestrator {

    private final CompiledGraph footballCompiledGraph;
    private final FootballMatchRedisService matchRedisService;
    private final ObjectMapper objectMapper;

    public FootballAgentContext run(String userQuery) {
        return executeGraph(buildContext(userQuery, null), null, null);
    }

    public FootballAgentContext runByMatchId(String matchId, String userQuery) {
        return executeGraph(buildContext(userQuery, matchId), null, null);
    }

    public FootballGraphResponse invokeStructured(String userQuery, String matchId) {
        FootballAgentContext context = executeGraph(buildContext(userQuery, matchId), null, null);
        return FootballGraphResponse.fromContext(context);
    }

    public SseEmitter runStream(String userQuery) {
        return runStructuredStream(userQuery, null, false);
    }

    public SseEmitter runStreamByMatchId(String matchId, String userQuery) {
        return runStructuredStream(userQuery, matchId, false);
    }

    public SseEmitter runStructuredStream(String userQuery, String matchId) {
        return runStructuredStream(userQuery, matchId, true);
    }

    /**
     * 统一流式入口（SSE / STOMP 共用）。
     */
    public void runAnalysisStream(AnalysisStreamSink sink, AnalysisStreamOptions options) {
        if (sink == null || options == null) {
            throw new AgentExecutionException("流式 sink 与 options 不能为 null");
        }
        ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(sink),
                AnalysisStreamConstants.HEARTBEAT_INTERVAL_SEC,
                AnalysisStreamConstants.HEARTBEAT_INTERVAL_SEC,
                TimeUnit.SECONDS);
        try {
            FootballAgentContext context = buildContext(options.message(), options.matchId());
            if (options.structuredOnly()) {
                executeGraphStream(context, sink, options.requestId());
            } else {
                executeGraph(context, sink, options.requestId());
                sink.sendProgress("\n\n========== 分析完成 ==========");
            }
            sink.complete();
        } catch (Exception e) {
            if (!sink.isOpen()) {
                log.warn("StateGraph 流式输出时客户端已断开: {}", e.getMessage());
                return;
            }
            log.error("StateGraph 流式执行失败", e);
            if (options.structuredOnly()) {
                sink.sendEvent(AnalysisStreamMessage.error(sink.requestId(), e.getMessage()));
            } else {
                sink.sendProgress("执行错误：" + e.getMessage());
            }
            sink.completeWithError(e);
        } finally {
            heartbeat.cancel(false);
            heartbeatScheduler.shutdownNow();
        }
    }

    private SseEmitter runStructuredStream(String userQuery, String matchId, boolean structuredOnly) {
        SseEmitter emitter = new SseEmitter(AnalysisStreamConstants.TIMEOUT_MS);
        emitter.onTimeout(() -> log.warn("SSE 异步请求超时（{} ms），前端可能只收到部分 Agent 输出",
                AnalysisStreamConstants.TIMEOUT_MS));
        emitter.onError(ex -> log.warn("SSE 连接异常: {}", ex.getMessage()));
        AnalysisStreamOptions options = structuredOnly
                ? AnalysisStreamOptions.structuredStream(userQuery, matchId)
                : AnalysisStreamOptions.textStream(userQuery, matchId);
        SseAnalysisStreamSink sink = new SseAnalysisStreamSink(emitter, null, structuredOnly, objectMapper);
        CompletableFuture.runAsync(() -> runAnalysisStream(sink, options));
        return emitter;
    }

    private void sendHeartbeat(AnalysisStreamSink sink) {
        if (!sink.isOpen()) {
            return;
        }
        if (sink instanceof SseAnalysisStreamSink sseSink) {
            sseSink.sendKeepalive();
            return;
        }
        sink.sendEvent(AnalysisStreamMessage.of(sink.requestId(), AnalysisStreamMessageType.PING));
    }

    private void executeGraphStream(FootballAgentContext context,
                                    AnalysisStreamSink sink,
                                    String requestId) {
        String sessionId = UUID.randomUUID().toString();
        FootballGraphProgress.bindSession(sessionId, sink);
        try {
            Map<String, Object> input = FootballGraphStateHelper.toInitialState(context, sessionId, requestId);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();
            footballCompiledGraph.stream(input, config)
                    .filter(nodeOutput -> !nodeOutput.isSTART())
                    .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, sink))
                    .blockLast();
        } finally {
            FootballGraphProgress.clearSession(sessionId);
        }
    }

    private void handleNodeOutput(NodeOutput nodeOutput, AnalysisStreamSink sink) {
        Map<String, Object> snapshot = FootballGraphStateHelper.toStateSnapshot(nodeOutput.state());
        if (nodeOutput.isEND()) {
            sink.sendEvent(AnalysisStreamMessage.done(sink.requestId(), snapshot));
            return;
        }
        sink.sendEvent(AnalysisStreamMessageMapper.fromGraphEvent(
                sink.requestId(), GraphStreamEvent.nodeComplete(nodeOutput.node(), snapshot)));
    }

    private FootballAgentContext executeGraph(FootballAgentContext context,
                                              AnalysisStreamSink sink,
                                              String requestId) {
        String sessionId = UUID.randomUUID().toString();
        if (sink != null) {
            FootballGraphProgress.bindSession(sessionId, sink);
        }
        try {
            Map<String, Object> input = FootballGraphStateHelper.toInitialState(context, sessionId, requestId);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();
            Optional<OverAllState> result = footballCompiledGraph.invoke(input, config);
            return FootballGraphStateHelper.toContext(
                    result.orElseThrow(() -> new AgentExecutionException("StateGraph 执行未返回结果")));
        } finally {
            FootballGraphProgress.clearSession(sessionId);
        }
    }

    private FootballAgentContext buildContext(String userQuery, String matchId) {
        if (matchId != null && !matchId.isBlank()) {
            String redisData = matchRedisService.findMatchDataById(matchId).orElse(null);
            String query = userQuery == null || userQuery.isBlank()
                    ? "请整理并分析比赛 ID " + matchId + " 的相关数据"
                    : userQuery;
            return new FootballAgentContext(query, matchId, redisData);
        }
        if (userQuery == null || userQuery.isBlank()) {
            throw new AgentExecutionException("message 与 matchId 不能同时为空");
        }
        return new FootballAgentContext(userQuery);
    }
}
