package com.example.javaai.agent.football;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Reception 写入 Redis 的竞彩缓存 Key 配置。
 * <p>
 * 完整 Key = prefix + matchId + keySuffix，例如 {@code jczqOuzhi::1366392:false}
 */
@Data
@ConfigurationProperties(prefix = "app.football.redis")
public class FootballRedisProperties {

    /**
     * 与 Reception {@code includeChanges} 对应，常规读取用 {@code :false}。
     */
    private String keySuffix = ":false";

    /**
     * 带 suffix 的 Key 不存在时，是否回退到旧格式（无 suffix，如 {@code jczqDetail::1366392}）。
     */
    private boolean legacyKeyFallback = true;

    private Keys keys = new Keys();

    /**
     * jczqOuzhi 欧盘 companies 过滤：官方竞彩 + 主流公司，减小 Data Agent prompt。
     */
    private CompaniesFilter ouzhiFilter = new CompaniesFilter();

    /**
     * jczqYazhi 亚盘 companies 过滤，规则与欧盘一致（按 companyId 保留）。
     */
    private CompaniesFilter yazhiFilter = new CompaniesFilter();

    @Data
    public static class CompaniesFilter {

        private boolean enabled = true;

        /**
         * 500.com companyId，顺序即输出顺序。默认：1 竞彩官、293 威廉、3 bet365、2 立博、4 Interwetten、5 澳门。
         */
        private List<String> companyIds = List.of("1", "293", "3", "2", "4", "5");
    }

    @Data
    public static class Keys {

        private String detail = "jczqDetail::";
        /** 比赛数据（积分榜、交锋、近期战绩），例如 jczqShuju::1359200 */
        private String shuju = "jczqShuju::";
        private String ouzhi = "jczqOuzhi::";
        /** 某公司欧盘变化过程，例如 jczqOuzhi::changes:1359200:293 */
        private String ouzhiChanges = "jczqOuzhi::changes:";
        private String yazhi = "jczqYazhi::";
        /** 某公司亚盘变化过程，例如 jczqYazhi::changes:1359200:293 */
        private String yazhiChanges = "jczqYazhi::changes:";
        /** 必发交易量，例如 jczqTouzhu::1359200 */
        private String touzhu = "jczqTouzhu::";
    }
}
