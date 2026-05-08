package com.jingqu.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionRouterTest {

    private final QuestionRouter router = new QuestionRouter();

    @Test
    void routesSpotQuestionToStructuredDataset() {
        assertEquals(QuestionRouter.RouteTarget.SPOT_STRUCTURED, router.route("五明桥在哪里"));
    }

    @Test
    void routesHistoryQuestionToHistoryDataset() {
        assertEquals(QuestionRouter.RouteTarget.HISTORY_ROUTE, router.route("灵山胜境的历史文化是什么"));
    }

    @Test
    void routesRoutePlanningToHistoryDataset() {
        assertEquals(QuestionRouter.RouteTarget.HISTORY_ROUTE, router.route("第一次来应该怎么逛，推荐顺序是什么"));
    }

    @Test
    void routesFacilityQuestionToFallback() {
        assertEquals(QuestionRouter.RouteTarget.FALLBACK_ONLY, router.route("厕所在哪里"));
    }
}
