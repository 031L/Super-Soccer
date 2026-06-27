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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
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

    static final String KNOWLEDGE_SOURCE = "knowledge";
    static final String KNOWLEDGE_DELETE_FILTER = "source == 'knowledge'";

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
     * 默认先删除 source=knowledge 旧数据，可安全重复调用。
     */
    public int ingestKnowledgeMarkdown() {
        return ingestMarkdownByPatterns(properties.getKnowledgePatterns());
    }

    public int ingestMarkdownByPatterns(String patternsCsv) {
        if (properties.isIngestReplaceExisting()) {
            deleteKnowledgeDocuments();
        }

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
        log.info("知识库 Markdown 入库完成，replaceExisting={}，共 {} 个文档块，patterns={}",
                properties.isIngestReplaceExisting(), allChunks.size(), patternsCsv);
        return allChunks.size();
    }

    /**
     * 删除向量库中 source=knowledge 的知识库批次数据。
     */
    public void deleteKnowledgeDocuments() {
        try {
            vectorStore.delete(KNOWLEDGE_DELETE_FILTER);
            log.info("已清除向量库中 source=knowledge 的旧数据");
        } catch (Exception e) {
            throw new IllegalStateException("清除知识库向量数据失败，已中止入库以避免重复叠加", e);
        }
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

    /**
     * 直读知识库 Markdown 全文（不切块），供文档较少时通用 Agent 直接使用。
     */
    public String loadKnowledgeMarkdownFullText() {
        StringBuilder sb = new StringBuilder();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String raw : properties.getKnowledgePatterns().split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            try {
                for (Resource resource : resolver.getResources(pattern)) {
                    appendMarkdownResource(sb, resource);
                }
            } catch (IOException e) {
                throw new IllegalStateException("读取知识库资源失败: " + pattern, e);
            }
        }
        return sb.toString().trim();
    }

    private void appendMarkdownResource(StringBuilder sb, Resource resource) throws IOException {
        if (!resource.exists() || !resource.isReadable()) {
            return;
        }
        String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.md";
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return;
        }
        sb.append("## ").append(filename).append("\n\n").append(content).append("\n\n");
    }

    private List<Document> loadAndSplitMarkdown(Resource resource) throws IOException {
        if (!resource.exists() || !resource.isReadable()) {
            return List.of();
        }
        String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.md";
        byte[] rawBytes = resource.getInputStream().readAllBytes();
        String contentHash = sha256Prefix(rawBytes, 8);

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .withAdditionalMetadata("source", KNOWLEDGE_SOURCE)
                .build();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> chunks = splitDocuments(reader.get());
        return enrichChunkMetadata(chunks, contentHash);
    }

    private List<Document> enrichChunkMetadata(List<Document> chunks, String contentHash) {
        List<Document> enriched = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document doc = chunks.get(i);
            Map<String, Object> meta = new HashMap<>(doc.getMetadata());
            meta.put("chunkIndex", i);
            meta.put("contentHash", contentHash);
            if (doc.getId() != null) {
                enriched.add(new Document(doc.getId(), doc.getText(), meta));
            } else {
                enriched.add(new Document(doc.getText(), meta));
            }
        }
        return enriched;
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

    private static String sha256Prefix(byte[] data, int hexChars) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash).substring(0, hexChars);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
