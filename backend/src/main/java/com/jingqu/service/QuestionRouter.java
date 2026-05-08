package com.jingqu.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class QuestionRouter {

    public enum RouteTarget {
        SPOT_STRUCTURED,
        HISTORY_ROUTE,
        BOTH,
        FALLBACK_ONLY
    }

    private static final List<String> ROUTE_KEYWORDS = Arrays.asList("怎么逛", "路线", "半天", "一天", "亲子", "推荐顺序", "深度游", "全景游");
    private static final List<String> HISTORY_KEYWORDS = Arrays.asList("历史", "文化", "佛教", "渊源", "典故", "祈福", "贴士", "游览");
    private static final List<String> SPOT_KEYWORDS = Arrays.asList("在哪", "哪里", "介绍", "特色", "亮点", "看什么", "位置");
    private static final List<String> FALLBACK_KEYWORDS = Arrays.asList("厕所", "洗手间", "卫生间", "停车", "客服电话", "热线", "天气", "门票", "开放时间");
    private static final List<String> SCENIC_SPOTS = Arrays.asList(
        "大照壁", "五明桥", "佛足坛", "五智门", "菩提大道", "九龙灌浴", "降魔浮雕",
        "阿育王柱", "百子戏弥勒", "祥符禅寺", "灵山大佛", "灵山梵宫", "五印坛城",
        "佛手广场", "曼飞龙塔", "无尽意斋", "三圣殿", "胜境广场", "杏坛广场", "佛前广场"
    );

    public RouteTarget route(String question) {
        String normalized = normalize(question);
        if (normalized.isEmpty()) {
            return RouteTarget.FALLBACK_ONLY;
        }

        boolean forceHistory = containsAny(normalized, ROUTE_KEYWORDS) || containsAny(normalized, HISTORY_KEYWORDS);
        boolean forceSpot = containsAny(normalized, SPOT_KEYWORDS) && containsAny(normalized, SCENIC_SPOTS);
        boolean fallbackOnly = containsAny(normalized, FALLBACK_KEYWORDS) && !forceHistory && !forceSpot;

        if (fallbackOnly) {
            return RouteTarget.FALLBACK_ONLY;
        }
        if (forceHistory && forceSpot) {
            return RouteTarget.BOTH;
        }
        if (forceHistory) {
            return RouteTarget.HISTORY_ROUTE;
        }
        if (forceSpot) {
            return RouteTarget.SPOT_STRUCTURED;
        }

        boolean mentionsSpot = containsAny(normalized, SCENIC_SPOTS);
        boolean mentionsHistory = containsAny(normalized, HISTORY_KEYWORDS);
        if (mentionsSpot && mentionsHistory) {
            return RouteTarget.BOTH;
        }
        if (mentionsSpot) {
            return RouteTarget.SPOT_STRUCTURED;
        }
        if (mentionsHistory) {
            return RouteTarget.HISTORY_ROUTE;
        }
        return RouteTarget.BOTH;
    }

    public List<String> datasetKeys(RouteTarget routeTarget) {
        Set<String> result = new LinkedHashSet<>();
        switch (routeTarget) {
            case SPOT_STRUCTURED:
                result.add("spotStructured");
                break;
            case HISTORY_ROUTE:
                result.add("historyRoute");
                break;
            case BOTH:
                result.add("spotStructured");
                result.add("historyRoute");
                break;
            default:
                break;
        }
        return new ArrayList<>(result);
    }

    public String normalize(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
