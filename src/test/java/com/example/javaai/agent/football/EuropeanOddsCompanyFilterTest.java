package com.example.javaai.agent.football;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EuropeanOddsCompanyFilterTest {

    @Test
    void filter_shouldKeepOfficialAndFiveMajorsInOrder() {
        JSONArray companies = new JSONArray();
        companies.add(company("1", "竞*官*"));
        companies.add(company("999", "其他"));
        companies.add(company("293", "威廉"));
        companies.add(company("3", "bet365"));
        companies.add(company("2", "立*"));
        companies.add(company("4", "IW"));
        companies.add(company("5", "澳门"));

        JSONObject europeanOdds = JSONUtil.createObj()
                .set("sourceUrl", "https://odds.500.com/fenxi/ouzhi-1364015.shtml")
                .set("companies", companies);

        JSONObject filtered = EuropeanOddsCompanyFilter.filter(
                europeanOdds, List.of("1", "293", "3", "2", "4", "5"));

        JSONArray kept = filtered.getJSONArray("companies");
        assertEquals(6, kept.size());
        assertEquals("1", kept.getJSONObject(0).getStr("companyId"));
        assertEquals("293", kept.getJSONObject(1).getStr("companyId"));
        assertEquals("5", kept.getJSONObject(5).getStr("companyId"));
        assertEquals(7, filtered.getJSONObject("_companyFilter").getInt("originalCount"));
        assertFalse(kept.getJSONObject(0).containsKey("instantKelly"));
    }

    private static JSONObject company(String id, String name) {
        return JSONUtil.createObj()
                .set("companyId", id)
                .set("companyName", name)
                .set("instantOdds", JSONUtil.createObj().set("win", 2.9))
                .set("instantKelly", JSONUtil.createObj().set("win", 0.9));
    }
}
