package com.example.javaai.agent.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 按比赛 ID 从 Redis 合并读取 Detail + Ouzhi + Yazhi，供足球 Agent 使用。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FootballMatchRedisService {

    private final StringRedisTemplate redisTemplate;
    private final FootballRedisProperties redisProperties;

    /**
     * 合并读取 Detail、Ouzhi、Yazhi；任一块存在即返回，三块皆无则 empty。
     */
    public Optional<String> findMatchDataById(String matchId) {
        if (StrUtil.isBlank(matchId)) {
            throw new IllegalArgumentException("比赛 ID 不能为空");
        }
        String fixtureId = matchId.trim();

        Optional<String> detail = readSection(redisProperties.getKeys().getDetail(), fixtureId);
        Optional<String> ouzhi = readSection(redisProperties.getKeys().getOuzhi(), fixtureId);
        Optional<String> yazhi = readSection(redisProperties.getKeys().getYazhi(), fixtureId);

        if (detail.isEmpty() && ouzhi.isEmpty() && yazhi.isEmpty()) {
            log.warn("Redis 中未找到比赛数据，matchId={}, detailKey={}, ouzhiKey={}, yazhiKey={}",
                    fixtureId,
                    buildRedisKey(redisProperties.getKeys().getDetail(), fixtureId),
                    buildRedisKey(redisProperties.getKeys().getOuzhi(), fixtureId),
                    buildRedisKey(redisProperties.getKeys().getYazhi(), fixtureId));
            return Optional.empty();
        }

        JSONObject merged = JSONUtil.createObj();
        merged.set("matchId", fixtureId);

        List<String> loadedKeys = new ArrayList<>(3);
        detail.ifPresent(data -> {
            JSONObject detailObj = JSONUtil.parseObj(data);
            filterOddsCompaniesInDetail(detailObj);
            merged.set("detail", detailObj);
            loadedKeys.add(buildRedisKey(redisProperties.getKeys().getDetail(), fixtureId));
        });
        ouzhi.ifPresent(data -> {
            merged.set("europeanOdds", JSONUtil.parseObj(data));
            loadedKeys.add(buildRedisKey(redisProperties.getKeys().getOuzhi(), fixtureId));
        });
        yazhi.ifPresent(data -> {
            merged.set("asianHandicap", JSONUtil.parseObj(data));
            loadedKeys.add(buildRedisKey(redisProperties.getKeys().getYazhi(), fixtureId));
        });

        RedisMatchDataProfile profile = RedisMatchDataProfile.inspect(merged);
        JSONObject meta = JSONUtil.createObj();
        meta.set("loadedKeys", new JSONArray(loadedKeys));
        meta.set("mergeStrategy", "detail+ouzhi+yazhi");
        meta.putAll(profile.toMetaJson());
        merged.set("_meta", meta);

        String payload = JSONUtil.toJsonPrettyStr(merged);
        log.info("已从 Redis 合并读取比赛数据，matchId={}, keys={}, length={}",
                fixtureId, loadedKeys, payload.length());
        return Optional.of(payload);
    }

    /**
     * 根据比赛 ID 从 Redis 获取合并数据。
     */
    public String getMatchDataById(String matchId) {
        return findMatchDataById(matchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Redis 中未找到比赛数据，matchId=" + matchId.trim()));
    }

    String buildRedisKey(String prefix, String matchId) {
        return prefix + matchId + redisProperties.getKeySuffix();
    }

    private Optional<String> readSection(String prefix, String matchId) {
        String primaryKey = buildRedisKey(prefix, matchId);
        String data = readStringValue(primaryKey);
        if (StrUtil.isBlank(data) && redisProperties.isLegacyKeyFallback()) {
            String legacyKey = prefix + matchId;
            if (!legacyKey.equals(primaryKey)) {
                data = readStringValue(legacyKey);
                if (StrUtil.isNotBlank(data)) {
                    log.info("Redis 使用旧格式 Key 命中，key={}", legacyKey);
                }
            }
        }
        return StrUtil.isBlank(data) ? Optional.empty() : Optional.of(normalizeRedisData(data, prefix));
    }

    private String readStringValue(String key) {
        String data = redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(data)) {
            return null;
        }
        return data;
    }

    /**
     * 清洗并格式化 Redis 返回值（去除 Jackson 类型包装、还原中文、省略 null），便于 Data Agent 解析。
     */
    private String normalizeRedisData(String raw, String keyPrefix) {
        try {
            String cleaned = RedisJacksonJsonCleaner.cleanJson(raw);
            if (keyPrefix.equals(redisProperties.getKeys().getOuzhi())) {
                cleaned = filterEuropeanOddsJson(cleaned);
            } else if (keyPrefix.equals(redisProperties.getKeys().getYazhi())) {
                cleaned = filterAsianHandicapJson(cleaned);
            }
            return cleaned;
        } catch (Exception e) {
            log.warn("Redis 数据清洗失败，使用原始字符串: {}", e.getMessage());
            return raw.trim();
        }
    }

    private void filterOddsCompaniesInDetail(JSONObject root) {
        if (root == null) {
            return;
        }
        JSONObject europeanOdds = root.getJSONObject("europeanOdds");
        if (europeanOdds != null) {
            root.set("europeanOdds", applyOuzhiCompanyFilter(europeanOdds));
        }
        JSONObject asianHandicap = root.getJSONObject("asianHandicap");
        if (asianHandicap != null) {
            root.set("asianHandicap", applyYazhiCompanyFilter(asianHandicap));
        }
    }

    private String filterEuropeanOddsJson(String cleanedJson) {
        if (!redisProperties.getOuzhiFilter().isEnabled()) {
            return cleanedJson;
        }
        return JSONUtil.toJsonPrettyStr(applyOuzhiCompanyFilter(JSONUtil.parseObj(cleanedJson)));
    }

    private String filterAsianHandicapJson(String cleanedJson) {
        if (!redisProperties.getYazhiFilter().isEnabled()) {
            return cleanedJson;
        }
        return JSONUtil.toJsonPrettyStr(applyYazhiCompanyFilter(JSONUtil.parseObj(cleanedJson)));
    }

    private JSONObject applyOuzhiCompanyFilter(JSONObject europeanOdds) {
        if (!redisProperties.getOuzhiFilter().isEnabled()) {
            return europeanOdds;
        }
        JSONArray before = europeanOdds.getJSONArray("companies");
        int originalCount = before == null ? 0 : before.size();
        JSONObject filtered = EuropeanOddsCompanyFilter.filter(
                europeanOdds, redisProperties.getOuzhiFilter().getCompanyIds());
        logCompanyFilterResult("欧盘", filtered, originalCount, redisProperties.getOuzhiFilter().getCompanyIds());
        return filtered;
    }

    private JSONObject applyYazhiCompanyFilter(JSONObject asianHandicap) {
        if (!redisProperties.getYazhiFilter().isEnabled()) {
            return asianHandicap;
        }
        JSONArray before = asianHandicap.getJSONArray("companies");
        int originalCount = before == null ? 0 : before.size();
        JSONObject filtered = AsianHandicapCompanyFilter.filter(
                asianHandicap, redisProperties.getYazhiFilter().getCompanyIds());
        logCompanyFilterResult("亚盘", filtered, originalCount, redisProperties.getYazhiFilter().getCompanyIds());
        return filtered;
    }

    private void logCompanyFilterResult(String label, JSONObject filtered, int originalCount, List<String> ids) {
        JSONObject filterMeta = filtered.getJSONObject("_companyFilter");
        int keptCount = filterMeta == null ? originalCount : filterMeta.getInt("keptCount");
        log.info("{} companies 已过滤，保留 {}/{} 家，ids={}", label, keptCount, originalCount, ids);
    }
}
