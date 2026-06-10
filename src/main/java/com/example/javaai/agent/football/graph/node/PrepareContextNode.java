package com.example.javaai.agent.football.graph.node;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.FootballMatchRedisService;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 准备上下文：补全 matchId 对应 Redis 数据，规范化用户问题。
 */
@Component
@RequiredArgsConstructor
public class PrepareContextNode implements NodeAction {

    private final FootballMatchRedisService matchRedisService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userQuery = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY);
        String matchId = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.MATCH_ID);
        String redisRawData = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.REDIS_RAW_DATA);

        FootballGraphProgress.emit("【编排器】启动足球 StateGraph 协作，用户问题：" + userQuery);

        if (StrUtil.isBlank(redisRawData) && StrUtil.isNotBlank(matchId)) {
            redisRawData = matchRedisService.findMatchDataById(matchId).orElse("");
            if (StrUtil.isNotBlank(redisRawData)) {
                FootballGraphProgress.emit("【编排器】已从 Redis 加载比赛 ID：" + matchId);
            } else {
                FootballGraphProgress.emit("【编排器】Redis 无比赛 ID " + matchId + " 的数据，将使用搜索工具获取");
            }
        }

        if (StrUtil.isBlank(userQuery) && StrUtil.isNotBlank(matchId)) {
            userQuery = "请整理并分析比赛 ID " + matchId + " 的相关数据";
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FootballGraphKeys.USER_QUERY, userQuery);
        updates.put(FootballGraphKeys.MATCH_ID, matchId);
        updates.put(FootballGraphKeys.REDIS_RAW_DATA, redisRawData);
        return updates;
    }
}
