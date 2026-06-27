package com.example.javaai.agent.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 过滤百家欧盘 companies：仅保留官方竞彩 + 配置的主流公司，并压缩单家公司字段。
 */
public final class EuropeanOddsCompanyFilter {

    private EuropeanOddsCompanyFilter() {
    }

    /**
     * @param allowedCompanyIds 按输出顺序排列，如 1(竞彩官)、293(威廉)、3(bet365)、2(立博)、4(Interwetten)、5(澳门)
     */
    public static JSONObject filter(JSONObject europeanOdds, List<String> allowedCompanyIds) {
        if (europeanOdds == null || allowedCompanyIds == null || allowedCompanyIds.isEmpty()) {
            return europeanOdds;
        }
        JSONArray companies = europeanOdds.getJSONArray("companies");
        if (companies == null || companies.isEmpty()) {
            return europeanOdds;
        }

        List<JSONObject> kept = new ArrayList<>();
        Set<String> allowed = new LinkedHashSet<>(allowedCompanyIds);
        for (String companyId : allowedCompanyIds) {
            JSONObject company = findCompany(companies, companyId);
            if (company != null) {
                kept.add(slimCompany(company));
            }
        }

        JSONObject result = copyHeaderFields(europeanOdds);
        result.set("companies", new JSONArray(kept));
        JSONObject filterMeta = new JSONObject();
        filterMeta.set("keptCompanyIds", new JSONArray(allowedCompanyIds));
        filterMeta.set("keptCount", kept.size());
        filterMeta.set("originalCount", companies.size());
        result.set("_companyFilter", filterMeta);
        return result;
    }

    private static JSONObject findCompany(JSONArray companies, String companyId) {
        for (int i = 0; i < companies.size(); i++) {
            JSONObject company = companies.getJSONObject(i);
            if (company != null && companyId.equals(company.getStr("companyId"))) {
                return company;
            }
        }
        return null;
    }

    private static JSONObject copyHeaderFields(JSONObject europeanOdds) {
        JSONObject result = new JSONObject();
        if (StrUtil.isNotBlank(europeanOdds.getStr("sourceUrl"))) {
            result.set("sourceUrl", europeanOdds.getStr("sourceUrl"));
        }
        JSONObject header = europeanOdds.getJSONObject("header");
        if (header != null && !header.isEmpty()) {
            result.set("header", header);
        }
        return result;
    }

    /**
     * Data Agent 分析所需的核心赔率字段。
     */
    private static JSONObject slimCompany(JSONObject company) {
        JSONObject slim = new JSONObject();
        slim.set("companyId", company.getStr("companyId"));
        slim.set("companyName", company.getStr("companyName"));
        slim.set("lastUpdateTime", company.getStr("lastUpdateTime"));
        copyIfPresent(company, slim, "instantOdds");
        copyIfPresent(company, slim, "initialOdds");
        copyIfPresent(company, slim, "instantProbability");
        copyIfPresent(company, slim, "initialProbability");
        copyIfPresent(company, slim, "instantReturnRate");
        copyIfPresent(company, slim, "initialReturnRate");
        copyIfPresent(company, slim, "changes");
        return slim;
    }

    private static void copyIfPresent(JSONObject source, JSONObject target, String key) {
        Object value = source.get(key);
        if (value != null && !(value instanceof cn.hutool.json.JSONNull)) {
            target.set(key, value);
        }
    }
}
