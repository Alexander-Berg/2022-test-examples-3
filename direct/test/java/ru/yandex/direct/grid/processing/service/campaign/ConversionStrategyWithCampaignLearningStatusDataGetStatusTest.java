package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.model.GdiGoalStats;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdCampaignAggregatedStatusInfo;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignFlatStrategy;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpa;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyCrr;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyType;
import ru.yandex.direct.grid.processing.model.campaign.GdConversionStrategyLearningStatus;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID;


@RunWith(Parameterized.class)
public class ConversionStrategyWithCampaignLearningStatusDataGetStatusTest {

    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public ConversionStrategyWithCampaignLearningData data;

    @Parameterized.Parameter(2)
    public LocalDate now;

    @Parameterized.Parameter(3)
    public GdConversionStrategyLearningStatus expectedStatus;

    private static final LocalDate NOW = now();

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                //learning
                {"new campaign with 10 conversions", data(11L, 0L, NOW.minusDays(1),
                        GdSelfStatusEnum.RUN_OK,false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"new campaign with 10 conversions", data(9L, 0L, NOW.minusDays(1),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"5 day campaign with 10 conversions", data(11L, 0L, NOW.minusDays(5),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"7 day campaign with 0 conversions", data(0L, 0L, NOW.minusDays(7),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"8 day with only one conversion in last day", data(1L, 1L, NOW.minusDays(8),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"crr strategy 6 days, no revenue, 100 conversions", data(100L, 0L, NOW.minusDays(6),
                        GdSelfStatusEnum.RUN_OK, false, true, false), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"crr strategy 6 days, has revenue, 5 conversions", data(5L, 0L, NOW.minusDays(6),
                        GdSelfStatusEnum.RUN_OK, false, true, true), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"crr strategy 2 days, has revenue, 100 conversions", data(100L, 0L, NOW.minusDays(2),
                        GdSelfStatusEnum.RUN_OK, false, true, true), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"crr strategy 2 days, has revenue, 100 conversions, optimization on meaningful goals", data(100L, 0L, NOW.minusDays(2),
                        GdSelfStatusEnum.RUN_OK, true, true, true), NOW, GdConversionStrategyLearningStatus.LEARNING},
                {"crr strategy 2 days, no revenue, 100 conversions, optimization on meaningful goals", data(100L, 0L, NOW.minusDays(2),
                        GdSelfStatusEnum.RUN_OK, true, true, false), NOW, GdConversionStrategyLearningStatus.LEARNING},

                //learned
                {"7 day campaign with 10 conversions", data(10L, 0L, NOW.minusDays(7),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNED},
                {"8 day campaign with 10 conversions", data(10L, 0L, NOW.minusDays(8),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.LEARNED},
                {"6 day campaign with 10 conversions", data(10L, 0L, NOW.minusDays(6),
                        GdSelfStatusEnum.RUN_OK, false, false, true), NOW, GdConversionStrategyLearningStatus.LEARNED},
                {"revenue model strategy 6 day campaign with 10 conversions", data(10L, 0L, NOW.minusDays(6),
                        GdSelfStatusEnum.RUN_OK, false, true, true), NOW, GdConversionStrategyLearningStatus.LEARNED},
                {"revenue model strategy 6 day campaign with 10 conversions and optimization on meaningful goals",
                        data(10L, 0L, NOW.minusDays(6), GdSelfStatusEnum.RUN_OK, true, true, true),
                        NOW, GdConversionStrategyLearningStatus.LEARNED
                },
                {"revenue model strategy 10 day campaign with 10 conversions and optimization on meaningful goals",
                        data(10L, 0L, NOW.minusDays(10), GdSelfStatusEnum.RUN_OK, true, true, true),
                        NOW, GdConversionStrategyLearningStatus.LEARNED
                },

                //not learned
                {"8 day campaign with less than 10 conversions", data(9L, 0L, NOW.minusDays(8),
                        GdSelfStatusEnum.RUN_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.NOT_LEARNED},
                {"stopped campaign with 10 conversions", data(10L, 0L, NOW.minusDays(8),
                        GdSelfStatusEnum.STOP_OK, false, false, false), NOW, GdConversionStrategyLearningStatus.NOT_LEARNED},
                {"archived campaign with 10 conversions", data(10L, 0L, NOW.minusDays(8),
                        GdSelfStatusEnum.ARCHIVED, false, false, false), NOW, GdConversionStrategyLearningStatus.NOT_LEARNED},
                {"100 conversions 8 days and no revenue", data(100L, 0L, NOW.minusDays(8),
                        GdSelfStatusEnum.RUN_OK, false, true, false), NOW, GdConversionStrategyLearningStatus.NOT_LEARNED},
                {"100 conversions, 8 days, no revenue, has last day conversions", data(100L, 10L, NOW.minusDays(8),
                        GdSelfStatusEnum.RUN_OK, false, true, false), NOW, GdConversionStrategyLearningStatus.NOT_LEARNED},
                {"9 conversions 8 days and has revenue", data(9L, 0L, NOW.minusDays(8),
                        GdSelfStatusEnum.RUN_OK, false, true, true), NOW, GdConversionStrategyLearningStatus.NOT_LEARNED},
                {"revenue model strategy 10 day campaign with 5 conversions and optimization on meaningful goals",
                        data(5L, 0L, NOW.minusDays(10), GdSelfStatusEnum.RUN_OK, true, true, true),
                        NOW, GdConversionStrategyLearningStatus.NOT_LEARNED
                },
        };
    }

    @Test
    public void testGetStatus() {
        GdConversionStrategyLearningStatus status = data.getStatus(now);

        assertThat(status).isEqualTo(expectedStatus);
    }

    private static ConversionStrategyWithCampaignLearningData data(Long conversionCount, Long lastDayConversionCount,
                                                                   LocalDate restartDate, GdSelfStatusEnum status,
                                                                   boolean isMeaningfulGoalsOptimizationGoalId,
                                                                   boolean isRevenueModelStrategy,
                                                                   boolean hasRevenue) {
        var campaign = new GdTextCampaign()
                .withFlatStrategy(strategy(isRevenueModelStrategy))
                .withStartDate(restartDate)
                .withAggregatedStatusInfo(aggregatedStatusInfo(status));

        return new ConversionStrategyWithCampaignLearningData(
                campaign,
                restartDate,
                isMeaningfulGoalsOptimizationGoalId ? MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID : null,
                stats(conversionCount, hasRevenue),
                stats(lastDayConversionCount, hasRevenue)
        );
    }

    private static GdCampaignFlatStrategy strategy(boolean isRevenueModelStrategy) {
        if (isRevenueModelStrategy) {
            return new GdCampaignStrategyCrr().withStrategyType(GdStrategyType.CRR);
        } else {
            return new GdCampaignStrategyAvgCpa().withStrategyType(GdStrategyType.AVG_CPA);
        }
    }

    private static GdCampaignAggregatedStatusInfo aggregatedStatusInfo(GdSelfStatusEnum status) {
        return new GdCampaignAggregatedStatusInfo().withStatus(status);
    }

    private static GdiCampaignStats stats(Long conversionCount, boolean hasRevenue) {
        var stats = new GdiCampaignStats()
                .withGoalStats(List.of(goalStats(conversionCount, hasRevenue)));
        var entityStats = new GdiEntityStats();
        return stats.withStat(entityStats);
    }

    private static GdiGoalStats goalStats(Long conversionCount, boolean hasRevenue) {
        var stat = new GdiGoalStats()
                .withGoals(conversionCount);
        if (hasRevenue) {
            stat.withRevenue(RandomNumberUtils.nextPositiveLong());
        }
        return stat;
    }
}
