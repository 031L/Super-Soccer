package com.example.javaai.agent.football;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.example.javaai.agent.AgentExecutionException;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 足球多智能体编排器：基于 StateGraph 驱动协作。
 *
 * <pre>
 * START → prepare_context → classify_intent
 *   ├─ MATCH_ANALYSIS → 数据 → 推演 → 战术 → 综合 → END
 *   └─ GENERAL_QA     → 通用 Agent → END
 * </pre>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FootballMultiAgentOrchestrator {

    private final CompiledGraph footballCompiledGraph;
    private final FootballMatchRedisService matchRedisService;

    public FootballAgentContext run(String userQuery) {
        return executeGraph(new FootballAgentContext(userQuery), null);
    }

    public FootballAgentContext runByMatchId(String matchId, String userQuery) {
        String redisData = matchRedisService.findMatchDataById(matchId).orElse(null);
        String query = userQuery == null || userQuery.isBlank()
                ? "请整理并分析比赛 ID " + matchId + " 的相关数据"
                : userQuery;
        FootballAgentContext context = new FootballAgentContext(query, matchId, redisData);
        return executeGraph(context, null);
    }

    public SseEmitter runStream(String userQuery) {
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> runStreamInternal(new FootballAgentContext(userQuery), emitter));
        return emitter;
    }

    public SseEmitter runStreamByMatchId(String matchId, String userQuery) {
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> {
            try {
                String redisData = matchRedisService.findMatchDataById(matchId).orElse(null);
                String query = userQuery == null || userQuery.isBlank()
                        ? "请整理并分析比赛 ID " + matchId + " 的相关数据"
                        : userQuery;
                runStreamInternal(new FootballAgentContext(query, matchId, redisData), emitter);
            } catch (Exception e) {
                log.error("足球 StateGraph 流式执行失败", e);
                completeEmitterWithError(emitter, new AtomicBoolean(false), e);
            }
        });
        return emitter;
    }

    private void runStreamInternal(FootballAgentContext context, SseEmitter emitter) {
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);
        try {
            executeGraph(context, event -> safeSend(emitter, clientDisconnected, event));
            safeSend(emitter, clientDisconnected, "\n\n========== 分析完成 ==========");
            safeComplete(emitter, clientDisconnected);
        } catch (Exception e) {
            if (clientDisconnected.get()) {
                log.warn("足球 StateGraph 流式输出时客户端已断开，后台流水线异常: {}", e.getMessage());
                return;
            }
            log.error("足球 StateGraph 流式执行失败", e);
            completeEmitterWithError(emitter, clientDisconnected, e);
        }
    }

    private FootballAgentContext executeGraph(FootballAgentContext context, Consumer<String> progressSink) {
        FootballGraphProgress.bind(progressSink);
        try {
            Map<String, Object> input = FootballGraphStateHelper.toInitialState(context);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();
            Optional<OverAllState> result = footballCompiledGraph.invoke(input, config);
            return FootballGraphStateHelper.toContext(
                    result.orElseThrow(() -> new AgentExecutionException("StateGraph 执行未返回结果")));
        } finally {
            FootballGraphProgress.clear();
        }
    }

    private void safeSend(SseEmitter emitter, AtomicBoolean clientDisconnected, String event) {
        if (clientDisconnected.get() || event == null) {
            return;
        }
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            clientDisconnected.set(true);
            log.warn("SSE 连接已关闭，停止向前端推送: {}", e.getMessage());
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
        safeSend(emitter, clientDisconnected, "执行错误：" + e.getMessage());
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
