package com.example.javaai.websocket.football;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分析任务注册表：支持 cancel 与并发控制。
 */
@Component
public class AnalysisTaskRegistry {

    public record TaskContext(
            String requestId,
            String principalName,
            AtomicBoolean cancelled,
            Instant createdAt) {
    }

    private final ConcurrentHashMap<String, TaskContext> tasks = new ConcurrentHashMap<>();

    public void register(String requestId, String principalName) {
        tasks.put(requestId, new TaskContext(
                requestId, principalName, new AtomicBoolean(false), Instant.now()));
    }

    public void remove(String requestId) {
        if (requestId != null) {
            tasks.remove(requestId);
        }
    }

    public boolean cancel(String requestId, String principalName) {
        TaskContext ctx = tasks.get(requestId);
        if (ctx == null || !ctx.principalName().equals(principalName)) {
            return false;
        }
        ctx.cancelled().set(true);
        return true;
    }

    public boolean isCancelled(String requestId) {
        if (requestId == null) {
            return false;
        }
        TaskContext ctx = tasks.get(requestId);
        return ctx != null && ctx.cancelled().get();
    }

    public boolean belongsToPrincipal(String requestId, String principalName) {
        TaskContext ctx = tasks.get(requestId);
        return ctx != null && ctx.principalName().equals(principalName);
    }

    public int activeCountForPrincipal(String principalName) {
        AtomicInteger count = new AtomicInteger();
        tasks.values().forEach(ctx -> {
            if (ctx.principalName().equals(principalName) && !ctx.cancelled().get()) {
                count.incrementAndGet();
            }
        });
        return count.get();
    }
}
