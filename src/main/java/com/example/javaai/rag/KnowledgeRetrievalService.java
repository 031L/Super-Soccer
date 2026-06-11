package com.example.javaai.rag;

import com.example.javaai.config.properties.VectorStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 为通用 Agent 提供知识库上下文：优先向量语义检索，无结果时回退直读 Markdown。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final KnowledgeVectorStoreService knowledgeVectorStoreService;
    private final VectorStoreProperties properties;

    /**
     * 根据用户问题检索相关知识片段，供通用 Agent 任务 prompt 注入。
     */
    public Optional<String> retrieveContext(String query) {
        if (!properties.isGeneralAgentRagEnabled() || !StringUtils.hasText(query)) {
            return Optional.empty();
        }
        Optional<String> vectorContext = retrieveFromVectorStore(query.trim());
        if (vectorContext.isPresent()) {
            return vectorContext;
        }
        if (properties.isGeneralAgentDirectReadFallback()) {
            return retrieveFromMarkdownFiles();
        }
        return Optional.empty();
    }

    private Optional<String> retrieveFromVectorStore(String query) {
        try {
            List<Document> hits = knowledgeVectorStoreService.search(query);
            if (hits.isEmpty()) {
                log.debug("向量库无命中，query={}", query);
                return Optional.empty();
            }
            log.info("通用 Agent 知识检索命中 {} 条，query={}", hits.size(), query);
            return Optional.of(formatVectorHits(hits));
        } catch (Exception e) {
            log.warn("向量检索失败，将尝试直读知识库 Markdown: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> retrieveFromMarkdownFiles() {
        String fullText = knowledgeVectorStoreService.loadKnowledgeMarkdownFullText();
        if (!StringUtils.hasText(fullText)) {
            return Optional.empty();
        }
        log.info("通用 Agent 使用直读知识库 Markdown，共 {} 字符", fullText.length());
        return Optional.of(fullText);
    }

    private String formatVectorHits(List<Document> hits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            Document doc = hits.get(i);
            sb.append("### 片段 ").append(i + 1);
            Object filename = doc.getMetadata().get("filename");
            if (filename != null) {
                sb.append("（").append(filename).append("）");
            }
            sb.append("\n").append(doc.getText()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
