package com.example.javaai.agent.football;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通用问答独立编排器：跳过意图分类，直接执行通用 Agent。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GeneralQaOrchestrator {

    private final GeneralQaRunner generalQaRunner;

    public String run(String userQuery) {
        log.info("通用问答开始: {}", userQuery);
        return generalQaRunner.run(userQuery, null);
    }

    public SseEmitter runStream(String userQuery) {
        log.info("通用问答流式开始: {}", userQuery);
        SseEmitter emitter = new SseEmitter(FootballSseConstants.TIMEOUT_MS);
        emitter.onTimeout(() -> log.warn("通用问答 SSE 超时（{} ms）", FootballSseConstants.TIMEOUT_MS));
        CompletableFuture.runAsync(() -> runStreamInternal(userQuery, emitter));
        return emitter;
    }

    private void runStreamInternal(String userQuery, SseEmitter emitter) {
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);
        try {
            generalQaRunner.run(userQuery, event -> safeSend(emitter, clientDisconnected, event));
            safeSend(emitter, clientDisconnected, "\n\n========== 回答完成 ==========");
            safeComplete(emitter, clientDisconnected);
        } catch (Exception e) {
            if (clientDisconnected.get()) {
                log.warn("通用问答流式输出时客户端已断开: {}", e.getMessage());
                return;
            }
            log.error("通用问答流式执行失败", e);
            completeEmitterWithError(emitter, clientDisconnected, e);
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
