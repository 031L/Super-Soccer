package com.example.javaai.controller;

import com.example.javaai.rag.KnowledgeVectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量库调试接口：手动入库、批量导入知识库 Markdown、语义检索。
 */
@RestController
@RequestMapping("/ai/vector")
@RequiredArgsConstructor
public class VectorStoreController {

    private final KnowledgeVectorStoreService knowledgeVectorStoreService;

    /**
     * 写入单条文本到 PGVector（自动调用 EmbeddingModel 向量化）。
     * POST /api/ai/vector/add
     * body: {"content":"越位是指...", "metadata":{"type":"rule"}}
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody AddDocumentRequest request) {
        int count = knowledgeVectorStoreService.addText(request.content(), request.metadata());
        return Map.of("chunksAdded", count, "message", "写入成功");
    }

    /**
     * 将 classpath 下知识库 Markdown 切块入库。
     * POST /api/ai/vector/ingest
     */
    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        int count = knowledgeVectorStoreService.ingestKnowledgeMarkdown();
        return Map.of("chunksAdded", count, "message", count > 0 ? "导入成功" : "未找到知识库文件");
    }

    /**
     * 语义检索。
     * GET /api/ai/vector/search?query=什么是越位&topK=5
     */
    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0") double similarityThreshold) {
        return knowledgeVectorStoreService.search(query, topK, similarityThreshold).stream()
                .map(this::toResult)
                .toList();
    }

    private Map<String, Object> toResult(Document document) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", document.getText());
        result.put("metadata", document.getMetadata());
        if (document.getScore() != null) {
            result.put("score", document.getScore());
        }
        return result;
    }

    public record AddDocumentRequest(String content, Map<String, Object> metadata) {
    }
}
