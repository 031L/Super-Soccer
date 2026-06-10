package com.example.javaai.agent.football;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisJacksonJsonCleanerTest {

    @Test
    void cleanJson_shouldUnwrapBigDecimalAndArrayList() {
        String raw = """
                {"@class":"com.example.Foo","win":["java.math.BigDecimal",2.95],
                "companies":["java.util.ArrayList",[{"@class":"com.example.Bar","id":"1"}]],
                "changes":null}
                """;

        String cleaned = RedisJacksonJsonCleaner.cleanJson(raw);
        JSONObject obj = JSONUtil.parseObj(cleaned);

        assertFalse(obj.containsKey("@class"));
        assertFalse(obj.containsKey("changes"));
        assertEquals(2.95, obj.getDouble("win"));
        JSONArray companies = obj.getJSONArray("companies");
        assertEquals(1, companies.size());
        assertEquals("1", companies.getJSONObject(0).getStr("id"));
        assertFalse(companies.getJSONObject(0).containsKey("@class"));
    }

    @Test
    void decodeUtf8HexEscapes_shouldRestoreChinese() {
        String encoded = "\\xe6\\x96\\xaf\\xe8\\xbe\\xbe";
        assertEquals("斯达", RedisJacksonJsonCleaner.decodeUtf8HexEscapes(encoded));
    }

    @Test
    void preprocessHexEscapesInRawJson_shouldAllowJsonParse() {
        String raw = "{\"homeTeam\":\"\\xe6\\x96\\xaf\\xe8\\xbe\\xbe\"}";
        String preprocessed = RedisJacksonJsonCleaner.preprocessHexEscapesInRawJson(raw);
        assertTrue(JSONUtil.isTypeJSON(preprocessed));
        assertEquals("斯达", JSONUtil.parseObj(preprocessed).getStr("homeTeam"));
    }

    @Test
    void cleanJson_shouldDecodeHexInNestedStrings() {
        String raw = "{\"header\":{\"homeTeam\":\"\\xe6\\x96\\xaf\\xe8\\xbe\\xbe\",\"awayTeam\":\"\\xe7\\x93\\xa6\\xe5\\x8b\\x92\\xe4\\xbc\\xa6\\xe5\\x8a\\xa0\"}}";

        JSONObject obj = JSONUtil.parseObj(RedisJacksonJsonCleaner.cleanJson(raw));
        assertEquals("斯达", obj.getJSONObject("header").getStr("homeTeam"));
        assertEquals("瓦勒伦加", obj.getJSONObject("header").getStr("awayTeam"));
    }

    @Test
    void cleanJson_shouldHandleRealisticSpringRedisFragment() {
        String raw = "{\"@class\":\"com.example.reception.model.analysis.JczqMatchDetail\",\"fixtureId\":\"1364015\","
                + "\"header\":{\"@class\":\"com.example.reception.model.analysis.MatchHeaderInfo\",\"fixtureId\":\"1364015\","
                + "\"homeTeam\":\"\\xe6\\x96\\xaf\\xe8\\xbe\\xbe\",\"league\":\"26\\xe6\\x8c\\xaa\\xe8\\xb6\\x85\\xe7\\xac\\xac10\\xe8\\xbd\\xae\"},"
                + "\"europeanOdds\":{\"companies\":[\"java.util.ArrayList\",[{"
                + "\"@class\":\"com.example.reception.model.analysis.EuropeanOddsCompany\",\"companyName\":\"\\xe7\\xab\\x9e*\\xe5\\xae\\x98*\","
                + "\"instantOdds\":{\"win\":[\"java.math.BigDecimal\",2.95],\"draw\":[\"java.math.BigDecimal\",3.55],\"lose\":[\"java.math.BigDecimal\",1.97]},"
                + "\"changes\":null}]]}}";

        String cleaned = RedisJacksonJsonCleaner.cleanJson(raw);
        JSONObject obj = JSONUtil.parseObj(cleaned);

        assertEquals("1364015", obj.getStr("fixtureId"));
        assertEquals("斯达", obj.getJSONObject("header").getStr("homeTeam"));
        assertEquals("26挪超第10轮", obj.getJSONObject("header").getStr("league"));
        JSONObject company = obj.getJSONObject("europeanOdds").getJSONArray("companies").getJSONObject(0);
        assertEquals("竞*官*", company.getStr("companyName"));
        assertEquals(2.95, company.getJSONObject("instantOdds").getDouble("win"));
        assertTrue(cleaned.contains("斯达"));
        assertFalse(cleaned.contains("@class"));
        assertFalse(cleaned.contains("java.math.BigDecimal"));
    }
}
