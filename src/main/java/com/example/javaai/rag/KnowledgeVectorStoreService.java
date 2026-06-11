package com.example.javaai.rag;

import com.example.javaai.config.properties.VectorStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库向量存储：将文本/Markdown 切块后写入 PGVector，检索时由 EmbeddingModel 自动向量化查询。
 * <p>
 * 写入流程：Document → EmbeddingModel 生成向量 → PGVector 持久化
 * 检索流程：query → EmbeddingModel 向量化 → PGVector 相似度搜索
 */
@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(VectorStoreProperties.class)
public class KnowledgeVectorStoreService implements ApplicationRunner {

    private final VectorStore vectorStore;
    private final VectorStoreProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isIngestOnStartup()) {
            return;
        }
        int count = ingestKnowledgeMarkdown();
        log.info("启动时知识库入库完成，共写入 {} 个文档块", count);
    }

    /**
     * 写入单条文本。VectorStore.add 内部会调用 EmbeddingModel 生成向量。
     */
    public int addText(String content, Map<String, Object> metadata) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content 不能为空");
        }
        Map<String, Object> meta = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        meta.putIfAbsent("source", "manual");
        List<Document> chunks = splitDocuments(List.of(new Document(content.trim(), meta)));
        vectorStore.add(chunks);
        log.info("已向向量库写入 {} 个文档块，source={}", chunks.size(), meta.get("source"));
        return chunks.size();
    }

    /**
     * 扫描 classpath 下知识库 Markdown，切块后批量入库。
     */
    public int ingestKnowledgeMarkdown() {
        return ingestMarkdownByPatterns(properties.getKnowledgePatterns());
    }

    public int ingestMarkdownByPatterns(String patternsCsv) {
        List<Document> allChunks = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String raw : patternsCsv.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            try {
                for (Resource resource : resolver.getResources(pattern)) {
                    allChunks.addAll(loadAndSplitMarkdown(resource));
                }
            } catch (IOException e) {
                throw new IllegalStateException("扫描知识库资源失败: " + pattern, e);
            }
        }
        if (allChunks.isEmpty()) {
            log.warn("未找到可入库的知识库 Markdown，patterns={}", patternsCsv);
            return 0;
        }
        vectorStore.add(allChunks);
        log.info("知识库 Markdown 入库完成，共 {} 个文档块，patterns={}", allChunks.size(), patternsCsv);
        return allChunks.size();
    }

    /**
     * 语义检索：query 会由 EmbeddingModel 自动向量化，再在 PGVector 中做相似度匹配。
     */
    public List<Document> search(String query) {
        return search(query, properties.getSearchTopK(), properties.getSearchSimilarityThreshold());
    }

    public List<Document> search(String query, int topK, double similarityThreshold) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("query 不能为空");
        }
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query.trim())
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());
    }

    private List<Document> loadAndSplitMarkdown(Resource resource) throws IOException {
        if (!resource.exists() || !resource.isReadable()) {
            return List.of();
        }
        String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.md";
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .withAdditionalMetadata("source", "knowledge")
                .build();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        return splitDocuments(reader.get());
    }

    private List<Document> splitDocuments(List<Document> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(properties.getChunkSize())
                .withKeepSeparator(true)
                .build();
        return splitter.apply(documents);
    }
}
