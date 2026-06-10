package com.example.javaai.agent.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤百家亚盘 companies：仅保留配置的主流公司，并压缩单家公司字段。
 */
public final class AsianHandicapCompanyFilter {

    private AsianHandicapCompanyFilter() {
    }

    public static JSONObject filter(JSONObject asianHandicap, List<String> allowedCompanyIds) {
        if (asianHandicap == null || allowedCompanyIds == null || allowedCompanyIds.isEmpty()) {
            return asianHandicap;
        }
        JSONArray companies = asianHandicap.getJSONArray("companies");
        if (companies == null || companies.isEmpty()) {
            return asianHandicap;
        }

        List<JSONObject> kept = new ArrayList<>();
        for (String companyId : allowedCompanyIds) {
            JSONObject company = findCompany(companies, companyId);
            if (company != null) {
                kept.add(slimCompany(company));
            }
        }

        JSONObject result = copyHeaderFields(asianHandicap);
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

    private static JSONObject copyHeaderFields(JSONObject asianHandicap) {
        JSONObject result = new JSONObject();
        if (StrUtil.isNotBlank(asianHandicap.getStr("sourceUrl"))) {
            result.set("sourceUrl", asianHandicap.getStr("sourceUrl"));
        }
        JSONObject header = asianHandicap.getJSONObject("header");
        if (header != null && !header.isEmpty()) {
            result.set("header", header);
        }
        return result;
    }

    private static JSONObject slimCompany(JSONObject company) {
        JSONObject slim = new JSONObject();
        slim.set("companyId", company.getStr("companyId"));
        slim.set("companyName", company.getStr("companyName"));
        slim.set("lastUpdateTime", company.getStr("lastUpdateTime"));
        copyIfPresent(company, slim, "instantHomeWater");
        copyIfPresent(company, slim, "instantHandicap");
        copyIfPresent(company, slim, "instantAwayWater");
        copyIfPresent(company, slim, "instantChangeTime");
        copyIfPresent(company, slim, "initialHomeWater");
        copyIfPresent(company, slim, "initialHandicap");
        copyIfPresent(company, slim, "initialAwayWater");
        copyIfPresent(company, slim, "initialChangeTime");
        return slim;
    }

    private static void copyIfPresent(JSONObject source, JSONObject target, String key) {
        Object value = source.get(key);
        if (value != null && !(value instanceof cn.hutool.json.JSONNull)) {
            target.set(key, value);
        }
    }
}
