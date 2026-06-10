package com.example.javaai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 classpath 下 {@code document/} 目录加载多个提示模板（启动时扫描 {@code .md}、{@code .st}）。
 * 占位符与现有文档一致：{@code {{变量名}}}，通过 {@link #render(String, Map)} 替换。
 */
@Component
@Slf4j
public class DocumentPromptService {

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public DocumentPromptService(
            ResourceLoader resourceLoader,
            @Value("${app.ai.prompt.document-patterns:classpath:/document/**/*.md,classpath:/document/**/*.st}") String patternsCsv
    ) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        for (String raw : patternsCsv.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            for (Resource resource : resolver.getResources(pattern)) {
                registerResource(resource);
            }
        }
        log.info("document 提示模板已加载: {}", templates.keySet());
    }

    private void registerResource(Resource resource) throws IOException {
        if (!resource.exists() || !resource.isReadable()) {
            return;
        }
        String filename = resource.getFilename();
        if (filename == null) {
            return;
        }
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }
        String key = filename.substring(0, dot);
        String body = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        templates.put(key, body);
        log.debug("注册模板 key={} <- {}", key, resource);
    }

    /** 已注册的模板名（文件名去掉后缀），例如 {@code 数据Agent提示词} */
    public Set<String> templateNames() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    /** 原始模板正文（未替换占位符） */
    public String getRaw(String templateKey) {
        String body = templates.get(templateKey);
        if (body == null) {
            throw new IllegalArgumentException(
                    "未找到提示模板: " + templateKey + "，已加载: " + templates.keySet());
        }
        return body;
    }

    /**
     * 将 {@code {{占位符}}} 替换为 {@code variables} 中的值；缺失的键保留原文不动。
     */
    public String render(String templateKey, Map<String, String> variables) {
        String body = getRaw(templateKey);
        if (variables == null || variables.isEmpty()) {
            return body;
        }
        String result = body;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            String placeholder = "{{" + e.getKey() + "}}";
            String value = e.getValue() != null ? e.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
