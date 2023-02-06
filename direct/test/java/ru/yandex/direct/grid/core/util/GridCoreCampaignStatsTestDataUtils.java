package ru.yandex.direct.grid.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.model.GdiGoalStats;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class GridCoreCampaignStatsTestDataUtils {
    public static final BigDecimal DEFAULT_CONVERSION_RATE = BigDecimal.valueOf(0.15);
    public static final Long DEFAULT_COST = 10000L;

    public static final Long DEFAULT_GOAL_ACTION_COUNT = 123L;
    public static final BigDecimal DEFAULT_GOAL_COST_PER_ACTION = BigDecimal.valueOf(225L);

    public static GdiEntityStats getEntityStats(Long cost) {
        return GridStatNew.addZeros(new GdiEntityStats()
                .withShows(BigDecimal.valueOf(1000))
                .withClicks(BigDecimal.valueOf(100))
                .withCost(BigDecimal.valueOf(cost))
                .withRevenue(BigDecimal.ZERO)
                .withGoals(BigDecimal.valueOf(15))
                .withConversionRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .withFirstPageShows(BigDecimal.valueOf(60))
                .withFirstPageClicks(BigDecimal.valueOf(30))
                .withFirstPageSumPosShows(null)
                .withFirstPageSumPosClicks(null)
                .withSessions(BigDecimal.valueOf(99))
                .withSessionsLimited(BigDecimal.valueOf(92))
                .withSessionDepth(BigDecimal.valueOf(600))
                .withBounces(BigDecimal.valueOf(40)));
    }

    public static GdiEntityStats getDefaultEntityStats() {
        return getEntityStats(DEFAULT_COST);
    }

    public static GdiGoalStats getGoalStat(Long goalId, BigDecimal costPerAction, Long goals) {
        return new GdiGoalStats()
                .withConversionRate(DEFAULT_CONVERSION_RATE)
                .withCostPerAction(costPerAction)
                .withGoalId(goalId)
                .withGoals(goals);
    }

    /**
     * Создает статистику по заданному набору целей
     * Заполняет параметры costPerAction и goals для каждой цели (если они заданы) или использует значение по-умолчанию
     *
     * @param goals                    - набор целей
     * @param goalsActionCountByGoalId - параметр goals (кол-во достижений) для каждой цели. Если не указан
     *                                 используется @{code DEFAULT_GOAL_ACTION_COUNT}
     * @param costPerActionByGoalId    - параметры costPerAction (стоимость конверсии) для каждой цели. Если не
     *                                 указан(ы) используется @{code DEFAULT_GOAL_COST_PER_ACTION}
     */
    public static List<GdiGoalStats> getGdiGoalStats(Set<Goal> goals, Map<Long, BigDecimal> costPerActionByGoalId,
                                                     Map<Long, Long> goalsActionCountByGoalId) {
        return mapList(goals, goal -> getGoalStat(
                goal.getId(),
                costPerActionByGoalId.getOrDefault(goal.getId(), DEFAULT_GOAL_COST_PER_ACTION),
                goalsActionCountByGoalId.getOrDefault(goal.getId(), DEFAULT_GOAL_ACTION_COUNT)));
    }

    public static GdiCampaignStats getDefaultCampaignStatsWithEmptyGoalStat() {
        return getCampaignStats(DEFAULT_COST, emptyList());
    }

    public static GdiCampaignStats getDefaultCampaignStats(List<GdiGoalStats> goalStats) {
        return getCampaignStats(DEFAULT_COST, goalStats);
    }

    public static GdiCampaignStats getCampaignStats(Long cost, List<GdiGoalStats> goalStats) {
        return new GdiCampaignStats()
                .withStat(getEntityStats(cost))
                .withGoalStats(goalStats);
    }

}
