package com.example.javaai.agent.football;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AsianHandicapCompanyFilterTest {

    @Test
    void filter_shouldKeepMajorCompaniesInOrder() {
        JSONArray companies = new JSONArray();
        companies.add(company("293", "威廉", 0.78));
        companies.add(company("999", "其他", 0.50));
        companies.add(company("5", "澳门", 0.85));
        companies.add(company("3", "bet365", 0.90));

        JSONObject asianHandicap = JSONUtil.createObj()
                .set("sourceUrl", "https://odds.500.com/fenxi/yazhi-1364015.shtml")
                .set("companies", companies);

        JSONObject filtered = AsianHandicapCompanyFilter.filter(
                asianHandicap, List.of("293", "3", "5"));

        JSONArray kept = filtered.getJSONArray("companies");
        assertEquals(3, kept.size());
        assertEquals("293", kept.getJSONObject(0).getStr("companyId"));
        assertEquals("受平手/半球", kept.getJSONObject(0).getStr("instantHandicap"));
        assertEquals(3, filtered.getJSONObject("_companyFilter").getInt("keptCount"));
        assertFalse(kept.getJSONObject(0).containsKey("changes"));
    }

    private static JSONObject company(String id, String name, double homeWater) {
        return JSONUtil.createObj()
                .set("companyId", id)
                .set("companyName", name)
                .set("instantHomeWater", homeWater)
                .set("instantHandicap", "受平手/半球")
                .set("instantAwayWater", 0.83)
                .set("changes", null);
    }
}
