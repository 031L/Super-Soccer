package com.example.javaai.agent.football;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisMatchDataProfileTest {

    @Test
    void inspect_shouldDetectMatchDataWithoutOdds() {
        String json = """
                {
                  "matchId": "1364015",
                  "detail": {
                    "fixtureId": "1364015",
                    "header": {"homeTeam": "斯达", "awayTeam": "瓦勒伦加"},
                    "matchData": {
                      "homeStandings": [{"label": "总成绩", "played": 10}],
                      "headToHeadSummary": "双方近6次交战",
                      "headToHeadRecords": [{"score": "1:1"}],
                      "homeRecentRecords": [],
                      "awayRecentRecords": []
                    }
                  }
                }
                """;
        RedisMatchDataProfile profile = RedisMatchDataProfile.inspect(JSONUtil.parseObj(json));

        assertTrue(profile.availableSections().contains("matchHeader"));
        assertTrue(profile.availableSections().contains("standings"));
        assertTrue(profile.availableSections().contains("headToHead"));
        assertFalse(profile.hasAnyOdds());
        assertTrue(profile.missingSections().contains("europeanOdds"));
        assertTrue(profile.missingSections().contains("asianHandicap"));
        assertTrue(profile.missingSections().contains("jczqOfficialOdds"));
        assertTrue(profile.toMetaJson().getStr("note").contains("不含欧赔"));
    }

    @Test
    void inspect_shouldDetectEuropeanOddsWhenPresent() {
        JSONObject merged = JSONUtil.parseObj("""
                {"europeanOdds":{"companies":[{"companyName":"威廉"}]}}
                """);

        RedisMatchDataProfile profile = RedisMatchDataProfile.inspect(merged);

        assertTrue(profile.hasEuropeanOdds());
        assertTrue(profile.hasAnyOdds());
        assertFalse(profile.missingSections().contains("europeanOdds"));
    }

    @Test
    void inspect_shouldTreatEuropeanOddsCompany1AsJczqOfficial() {
        JSONObject merged = JSONUtil.parseObj("""
                {
                  "europeanOdds": {
                    "companies": [
                      {"companyId": "1", "companyName": "竞彩官", "instantOdds": {"win": 2.95}}
                    ]
                  },
                  "asianHandicap": {"companies": [{"companyId": "293"}]}
                }
                """);

        RedisMatchDataProfile profile = RedisMatchDataProfile.inspect(merged);

        assertTrue(profile.hasEuropeanOdds());
        assertTrue(profile.hasJczqOfficialOdds());
        assertFalse(profile.missingSections().contains("jczqOfficialOdds"));
        assertTrue(profile.toMetaJson().getStr("note").contains("companyId=1"));
    }
}
