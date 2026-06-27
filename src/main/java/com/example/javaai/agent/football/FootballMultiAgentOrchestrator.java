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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 足球多智能体编排器：基于 StateGraph 驱动协作。
 *
 * <pre>
 * START → prepare_context → classify_intent
 *   ├─ MATCH_ANALYSIS → 数据 → 战术 → 推演 → 综合 → END
 *   └─ GENERAL_QA     → 通用 Agent → END
 * </pre>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FootballMultiAgentOrchestrator {

    private final CompiledGraph footballCompiledGraph;
    private final FootballMatchRedisService matchRedisService;
    private final ObjectMapper objectMapper;

    public FootballAgentContext run(String userQuery) {
        return executeGraph(buildContext(userQuery, null), null);
    }

    public FootballAgentContext runByMatchId(String matchId, String userQuery) {
        return executeGraph(buildContext(userQuery, matchId), null);
    }

    public FootballGraphResponse invokeStructured(String userQuery, String matchId) {
        FootballAgentContext context = executeGraph(buildContext(userQuery, matchId), null);
        return FootballGraphResponse.fromContext(context);
    }

    public SseEmitter runStream(String userQuery) {
        return runStructuredStream(userQuery, null, false);
    }

    public SseEmitter runStreamByMatchId(String matchId, String userQuery) {
        return runStructuredStream(userQuery, matchId, false);
    }

    /**
     * 使用 CompiledGraph.stream() 推送结构化 SSE 事件，充分暴露 StateGraph 节点与状态。
     */
    public SseEmitter runStructuredStream(String userQuery, String matchId) {
        return runStructuredStream(userQuery, matchId, true);
    }

    private SseEmitter runStructuredStream(String userQuery, String matchId, boolean structuredOnly) {
        SseEmitter emitter = new SseEmitter(FootballSseConstants.TIMEOUT_MS);
        emitter.onTimeout(() -> log.warn("SSE 异步请求超时（{} ms），前端可能只收到部分 Agent 输出",
                FootballSseConstants.TIMEOUT_MS));
        emitter.onError(ex -> log.warn("SSE 连接异常: {}", ex.getMessage()));
        CompletableFuture.runAsync(() -> {
            AtomicBoolean clientDisconnected = new AtomicBoolean(false);
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                    () -> safeSendKeepalive(emitter, clientDisconnected),
                    FootballSseConstants.HEARTBEAT_INTERVAL_SEC,
                    FootballSseConstants.HEARTBEAT_INTERVAL_SEC,
                    TimeUnit.SECONDS);
            try {
                if (structuredOnly) {
                    executeGraphStream(buildContext(userQuery, matchId), event -> safeSendEvent(emitter, clientDisconnected, event));
                } else {
                    executeGraph(buildContext(userQuery, matchId), text -> safeSendText(emitter, clientDisconnected, text));
                    safeSendText(emitter, clientDisconnected, "\n\n========== 分析完成 ==========");
                }
                safeComplete(emitter, clientDisconnected);
            } catch (Exception e) {
                if (clientDisconnected.get()) {
                    log.warn("StateGraph 流式输出时客户端已断开: {}", e.getMessage());
                    return;
                }
                log.error("StateGraph 流式执行失败", e);
                if (structuredOnly) {
                    safeSendEvent(emitter, clientDisconnected, GraphStreamEvent.error(e.getMessage()));
                } else {
                    safeSendText(emitter, clientDisconnected, "执行错误：" + e.getMessage());
                }
                completeEmitterWithError(emitter, clientDisconnected, e);
            } finally {
                heartbeat.cancel(false);
                heartbeatScheduler.shutdownNow();
            }
        });
        return emitter;
    }

    private void executeGraphStream(FootballAgentContext context, Consumer<GraphStreamEvent> eventSink) {
        String sessionId = UUID.randomUUID().toString();
        FootballGraphProgress.bindSession(sessionId, null, eventSink);
        try {
            Map<String, Object> input = FootballGraphStateHelper.toInitialState(context, sessionId);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();
            footballCompiledGraph.stream(input, config)
                    .filter(nodeOutput -> !nodeOutput.isSTART())
                    .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, eventSink))
                    .blockLast();
        } finally {
            FootballGraphProgress.clearSession(sessionId);
        }
    }

    private void handleNodeOutput(NodeOutput nodeOutput, Consumer<GraphStreamEvent> eventSink) {
        Map<String, Object> snapshot = FootballGraphStateHelper.toStateSnapshot(nodeOutput.state());
        if (nodeOutput.isEND()) {
            eventSink.accept(GraphStreamEvent.done(snapshot));
            return;
        }
        eventSink.accept(GraphStreamEvent.nodeComplete(nodeOutput.node(), snapshot));
    }

    private FootballAgentContext executeGraph(FootballAgentContext context, Consumer<String> progressSink) {
        String sessionId = UUID.randomUUID().toString();
        FootballGraphProgress.bindSession(sessionId, progressSink, null);
        try {
            Map<String, Object> input = FootballGraphStateHelper.toInitialState(context, sessionId);
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

    private void safeSendKeepalive(SseEmitter emitter, AtomicBoolean clientDisconnected) {
        if (clientDisconnected.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException | IllegalStateException e) {
            clientDisconnected.set(true);
            log.debug("SSE keepalive 失败，连接可能已关闭: {}", e.getMessage());
        }
    }

    private void safeSendText(SseEmitter emitter, AtomicBoolean clientDisconnected, String event) {
        if (clientDisconnected.get() || event == null) {
            return;
        }
        try {
            int chunkSize = FootballSseConstants.CHUNK_SIZE;
            if (event.length() <= chunkSize) {
                emitter.send(event);
                return;
            }
            for (int offset = 0; offset < event.length(); offset += chunkSize) {
                if (clientDisconnected.get()) {
                    return;
                }
                int end = Math.min(offset + chunkSize, event.length());
                emitter.send(event.substring(offset, end));
            }
        } catch (IOException | IllegalStateException e) {
            clientDisconnected.set(true);
            log.warn("SSE 连接已关闭，停止向前端推送: {}", e.getMessage());
        }
    }

    private void safeSendEvent(SseEmitter emitter, AtomicBoolean clientDisconnected, GraphStreamEvent event) {
        if (clientDisconnected.get() || event == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(event.type().name())
                    .data(toJson(event)));
        } catch (IOException | IllegalStateException e) {
            clientDisconnected.set(true);
            log.warn("SSE 连接已关闭，停止向前端推送: {}", e.getMessage());
        }
    }

    private String toJson(GraphStreamEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 GraphStreamEvent 失败", e);
        }
    }

    private void safeComplete(SseEmitter emitter, AtomicBoolean clientDisconnected) {
        if (clientDisconnected.get()) {
            return;
        }
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            clientDisconnected.set(true);
            log.warn("SSE 已完成或已关闭，跳过 complete: {}", e.getMessage());
        }
    }

    private void completeEmitterWithError(SseEmitter emitter, AtomicBoolean clientDisconnected, Exception e) {
        if (!clientDisconnected.get()) {
            try {
                emitter.completeWithError(e);
            } catch (IllegalStateException ex) {
                clientDisconnected.set(true);
                log.warn("SSE 已完成或已关闭，跳过 completeWithError: {}", ex.getMessage());
            }
        }
    }
}
