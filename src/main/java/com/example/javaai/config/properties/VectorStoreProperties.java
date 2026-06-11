package com.example.javaai.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库业务配置，与 application.yml 中 app.ai.vectorstore 对应。
 */
@Data
@ConfigurationProperties(prefix = "app.ai.vectorstore")
public class VectorStoreProperties {

    /** 知识库 Markdown 扫描路径，逗号分隔 */
    private String knowledgePatterns = "classpath:/document/knowledge/**/*.md";

    /** 文本切块大小（token 数） */
    private int chunkSize = 512;

    /** 启动时自动将知识库 Markdown 入库 */
    private boolean ingestOnStartup = false;

    /** 默认检索返回条数 */
    private int searchTopK = 5;

    /** 默认相似度阈值，0 表示不过滤 */
    private double searchSimilarityThreshold = 0.0;
}
