package com.example.javaai.agent.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述合并清洗后的 Redis 比赛数据实际包含哪些板块，避免 Agent 误以为已有赔率。
 */
public record RedisMatchDataProfile(
        List<String> availableSections,
        boolean hasEuropeanOdds,
        boolean hasAsianHandicap,
        boolean hasJczqOfficialOdds,
        boolean hasBettingVolume,
        boolean hasEuropeanOddsChanges,
        boolean hasAsianHandicapChanges,
        List<String> missingSections) {

    public boolean hasAnyOdds() {
        return hasEuropeanOdds || hasAsianHandicap || hasJczqOfficialOdds;
    }

    public JSONObject toMetaJson() {
        JSONObject json = new JSONObject();
        json.set("availableSections", new JSONArray(availableSections));
        json.set("hasEuropeanOdds", hasEuropeanOdds);
        json.set("hasAsianHandicap", hasAsianHandicap);
        json.set("hasJczqOfficialOdds", hasJczqOfficialOdds);
        json.set("hasBettingVolume", hasBettingVolume);
        json.set("hasEuropeanOddsChanges", hasEuropeanOddsChanges);
        json.set("hasAsianHandicapChanges", hasAsianHandicapChanges);
        json.set("missingSections", new JSONArray(missingSections));
        json.set("note", buildNote());
        return json;
    }

    private String buildNote() {
        StringBuilder note = new StringBuilder();
        if (hasEuropeanOdds && hasJczqOfficialOdds) {
            note.append("Redis 已含欧赔/亚盘；竞彩官方欧赔见 europeanOdds.companies 中 companyId=1（竞彩官）。");
        } else if (hasAnyOdds()) {
            note.append("Redis 含欧赔/亚盘；若 missingSections 含 jczqOfficialOdds 表示欧盘无 companyId=1，可搜索补充。");
        } else {
            note.append("当前 Redis 数据不含欧赔、亚盘，仅含比赛基础/战绩/交锋等。");
            note.append(" 报告中的「赔率变化」章节必须通过搜索/爬取补充，并标注来源。");
        }
        if (hasBettingVolume) {
            note.append(" 必发交易量见 bettingVolume（来自 jczqTouzhu），勿再搜索补充。");
        } else if (!missingSections.isEmpty() && missingSections.contains("bettingVolume")) {
            note.append(" 必发交易量缺失，可通过搜索补充。");
        }
        if (hasEuropeanOddsChanges) {
            note.append(" 欧盘变化见 europeanOdds.changesByCompany 或 companies[].changes（来自 jczqOuzhi::changes）。");
        }
        if (hasAsianHandicapChanges) {
            note.append(" 亚盘变化见 asianHandicap.changesByCompany 或 companies[].changes（来自 jczqYazhi::changes）。");
        }
        note.append(" 独立 Key 的 matchData/europeanOdds/asianHandicap/bettingVolume 优先于 detail 内同名块。");
        note.append(" 仅伤停、近期战绩、天气等 _meta.missingSections 未列出的空字段需搜索补充。");
        return note.toString();
    }

    static RedisMatchDataProfile inspect(JSONObject merged) {
        List<String> available = new ArrayList<>();
        boolean european = hasEuropeanOdds(merged);
        boolean asian = hasAsianHandicap(merged);
        boolean jczq = hasJczqOfficialOdds(merged);
        boolean bettingVolume = hasBettingVolume(merged);
        boolean europeanChanges = hasOddsChanges(merged.getJSONObject("europeanOdds"));
        boolean asianChanges = hasOddsChanges(merged.getJSONObject("asianHandicap"));

        if (hasMatchHeader(merged)) {
            available.add("matchHeader");
        }
        if (hasStandings(merged)) {
            available.add("standings");
        }
        if (hasHeadToHead(merged)) {
            available.add("headToHead");
        }
        if (hasRecentForm(merged)) {
            available.add("recentForm");
        }
        if (european) {
            available.add("europeanOdds");
        }
        if (asian) {
            available.add("asianHandicap");
        }
        if (jczq) {
            available.add("jczqOfficialOdds");
        }
        if (bettingVolume) {
            available.add("bettingVolume");
        }
        if (europeanChanges) {
            available.add("europeanOddsChanges");
        }
        if (asianChanges) {
            available.add("asianHandicapChanges");
        }

        List<String> missing = new ArrayList<>();
        if (!european) {
            missing.add("europeanOdds");
        }
        if (!asian) {
            missing.add("asianHandicap");
        }
        if (!jczq) {
            missing.add("jczqOfficialOdds");
        }
        if (!bettingVolume) {
            missing.add("bettingVolume");
        }
        if (european && !europeanChanges) {
            missing.add("europeanOddsChanges");
        }
        if (asian && !asianChanges) {
            missing.add("asianHandicapChanges");
        }

        return new RedisMatchDataProfile(
                available, european, asian, jczq, bettingVolume, europeanChanges, asianChanges, missing);
    }

    private static boolean hasOddsChanges(JSONObject oddsSection) {
        if (oddsSection == null) {
            return false;
        }
        if (hasNonEmptySection(oddsSection.getJSONObject("changesByCompany"))) {
            return true;
        }
        JSONArray companies = oddsSection.getJSONArray("companies");
        if (companies == null) {
            return false;
        }
        for (int i = 0; i < companies.size(); i++) {
            JSONObject company = companies.getJSONObject(i);
            if (company != null && hasNonEmptySection(company.getJSONObject("changes"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBettingVolume(JSONObject merged) {
        return hasNonEmptySection(merged.getJSONObject("bettingVolume"))
                || hasNonEmptySection(detailPart(merged, "bettingVolume"));
    }

    private static boolean hasNonEmptySection(JSONObject section) {
        if (section == null || section.isEmpty()) {
            return false;
        }
        for (String key : section.keySet()) {
            Object value = section.get(key);
            if (value == null || value instanceof cn.hutool.json.JSONNull) {
                continue;
            }
            if (value instanceof JSONArray array) {
                if (!array.isEmpty()) {
                    return true;
                }
            } else if (value instanceof JSONObject object) {
                if (!object.isEmpty()) {
                    return true;
                }
            } else if (StrUtil.isNotBlank(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEuropeanOdds(JSONObject merged) {
        return hasOddsCompanies(merged.getJSONObject("europeanOdds"))
                || hasOddsCompanies(detailPart(merged, "europeanOdds"));
    }

    private static boolean hasAsianHandicap(JSONObject merged) {
        return hasHandicapCompanies(merged.getJSONObject("asianHandicap"))
                || hasHandicapCompanies(detailPart(merged, "asianHandicap"));
    }

    /**
     * 竞彩官方赔率：优先认欧盘 companies 中 companyId=1（500.com「竞彩官」），其次 detail 内独立 SP 字段。
     */
    private static boolean hasJczqOfficialOdds(JSONObject merged) {
        if (hasEuropeanOddsCompany(merged, "1")) {
            return true;
        }
        JSONObject detail = merged.getJSONObject("detail");
        if (detail == null) {
            return false;
        }
        for (String key : List.of("jczqOdds", "officialOdds", "lotteryOdds", "spfOdds")) {
            Object node = detail.get(key);
            if (node != null && !(node instanceof cn.hutool.json.JSONNull)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEuropeanOddsCompany(JSONObject merged, String companyId) {
        if (hasCompanyId(merged.getJSONObject("europeanOdds"), companyId)) {
            return true;
        }
        return hasCompanyId(detailPart(merged, "europeanOdds"), companyId);
    }

    private static boolean hasCompanyId(JSONObject oddsSection, String companyId) {
        if (oddsSection == null) {
            return false;
        }
        Object companies = oddsSection.get("companies");
        if (!(companies instanceof JSONArray array)) {
            return false;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject company = array.getJSONObject(i);
            if (company != null && companyId.equals(company.getStr("companyId"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMatchHeader(JSONObject merged) {
        JSONObject header = firstNonNull(
                merged.getJSONObject("header"),
                detailPart(merged, "header"),
                merged.getJSONObject("matchData") != null ? merged.getJSONObject("matchData").getJSONObject("header") : null,
                nestedHeader(merged.getJSONObject("matchData")),
                nestedHeader(detailPart(merged, "matchData")));
        return header != null && StrUtil.isNotBlank(header.getStr("homeTeam"));
    }

    private static boolean hasStandings(JSONObject merged) {
        return hasNonEmptyArray(detailPart(merged, "matchData"), "homeStandings")
                || hasNonEmptyArray(detailPart(merged, "matchData"), "awayStandings")
                || hasNonEmptyArray(merged.getJSONObject("matchData"), "homeStandings");
    }

    private static boolean hasHeadToHead(JSONObject merged) {
        JSONObject matchData = firstNonNull(detailPart(merged, "matchData"), merged.getJSONObject("matchData"));
        if (matchData == null) {
            return false;
        }
        return StrUtil.isNotBlank(matchData.getStr("headToHeadSummary"))
                || hasNonEmptyArray(matchData, "headToHeadRecords");
    }

    private static boolean hasRecentForm(JSONObject merged) {
        JSONObject matchData = firstNonNull(detailPart(merged, "matchData"), merged.getJSONObject("matchData"));
        if (matchData == null) {
            return false;
        }
        return hasNonEmptyArray(matchData, "homeRecentRecords")
                || hasNonEmptyArray(matchData, "awayRecentRecords");
    }

    private static boolean hasOddsCompanies(JSONObject odds) {
        return hasNonEmptyCompanies(odds);
    }

    private static boolean hasHandicapCompanies(JSONObject handicap) {
        return hasNonEmptyCompanies(handicap);
    }

    private static boolean hasNonEmptyCompanies(JSONObject section) {
        if (section == null) {
            return false;
        }
        Object companies = section.get("companies");
        if (companies instanceof JSONArray array) {
            return !array.isEmpty();
        }
        return false;
    }

    private static JSONObject detailPart(JSONObject merged, String key) {
        JSONObject detail = merged.getJSONObject("detail");
        return detail == null ? null : detail.getJSONObject(key);
    }

    private static JSONObject nestedHeader(JSONObject matchData) {
        return matchData == null ? null : matchData.getJSONObject("header");
    }

    private static boolean hasNonEmptyArray(JSONObject parent, String key) {
        if (parent == null) {
            return false;
        }
        JSONArray array = parent.getJSONArray(key);
        return array != null && !array.isEmpty();
    }

    @SafeVarargs
    private static JSONObject firstNonNull(JSONObject... candidates) {
        for (JSONObject candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
