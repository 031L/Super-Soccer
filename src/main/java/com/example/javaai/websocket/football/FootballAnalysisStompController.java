package com.example.javaai.websocket.football;

import cn.hutool.core.util.StrUtil;
import com.example.javaai.agent.AgentExecutionException;
import com.example.javaai.agent.football.FootballMultiAgentOrchestrator;
import com.example.javaai.stream.AnalysisStreamConstants;
import com.example.javaai.stream.AnalysisStreamMessage;
import com.example.javaai.stream.AnalysisStreamMessageType;
import com.example.javaai.stream.AnalysisStreamOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

/**
 * 足球分析 STOMP 消息入口。
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class FootballAnalysisStompController {

    private final FootballMultiAgentOrchestrator orchestrator;
    private final AnalysisTaskRegistry taskRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/football/analyze")
    public void analyze(AnalyzeCommand command, Principal principal) {
        try {
            requirePrincipal(principal);
            validateAnalyzeCommand(command);
        } catch (Exception e) {
            if (command != null && StrUtil.isNotBlank(command.requestId()) && principal != null) {
                sendError(command.requestId(), principal.getName(), e.getMessage());
            }
            return;
        }
        String principalName = principal.getName();
        if (taskRegistry.activeCountForPrincipal(principalName)
                >= AnalysisStreamConstants.MAX_ACTIVE_TASKS_PER_PRINCIPAL) {
            sendError(command.requestId(), principalName, "已有分析任务进行中，请稍后再试");
            return;
        }
        taskRegistry.register(command.requestId(), principalName);
        StompAnalysisStreamSink sink = new StompAnalysisStreamSink(
                messagingTemplate, principalName, command.requestId());
        sink.sendEvent(AnalysisStreamMessage.of(command.requestId(), AnalysisStreamMessageType.CONNECTED));
        log.info("STOMP 分析开始 requestId={}, principal={}, matchId={}",
                command.requestId(), principalName, command.matchId());
        CompletableFuture.runAsync(() -> {
            try {
                orchestrator.runAnalysisStream(sink, AnalysisStreamOptions.stompAnalyze(
                        command.requestId(), command.message(), command.matchId()));
            } finally {
                taskRegistry.remove(command.requestId());
            }
        });
    }

    @MessageMapping("/football/cancel")
    public void cancel(CancelCommand command, Principal principal) {
        requirePrincipal(principal);
        if (command == null || StrUtil.isBlank(command.requestId())) {
            return;
        }
        boolean cancelled = taskRegistry.cancel(command.requestId(), principal.getName());
        if (!cancelled) {
            sendError(command.requestId(), principal.getName(), "任务不存在或无权取消");
            return;
        }
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/football/analysis/" + command.requestId(),
                AnalysisStreamMessage.of(command.requestId(), AnalysisStreamMessageType.CANCELLED)
                        .withContent(command.reason()));
        log.info("STOMP 分析已取消 requestId={}", command.requestId());
    }

    @MessageMapping("/football/ping")
    public void ping(PingCommand command, Principal principal) {
        requirePrincipal(principal);
        if (command == null || StrUtil.isBlank(command.requestId())) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/football/analysis/" + command.requestId(),
                AnalysisStreamMessage.of(command.requestId(), AnalysisStreamMessageType.PONG));
    }

    private void validateAnalyzeCommand(AnalyzeCommand command) {
        if (command == null || StrUtil.isBlank(command.requestId())) {
            throw new AgentExecutionException("requestId 不能为空");
        }
        if (StrUtil.isBlank(command.message()) && StrUtil.isBlank(command.matchId())) {
            throw new AgentExecutionException("message 与 matchId 不能同时为空");
        }
    }

    private void requirePrincipal(Principal principal) {
        if (principal == null || StrUtil.isBlank(principal.getName())) {
            throw new AgentExecutionException("STOMP 未建立用户身份，请重新连接");
        }
    }

    private void sendError(String requestId, String principalName, String message) {
        messagingTemplate.convertAndSendToUser(
                principalName,
                "/queue/football/analysis/" + requestId,
                AnalysisStreamMessage.error(requestId, message));
    }
}
