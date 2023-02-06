package ru.yandex.direct.grid.processing.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import org.junit.Test;

import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.model.GdEntityStats;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

// TODO как-то выбрать фреймворк матчеров -- либо junit-ассерты, либо assertj-ассерты
public class StatHelperTest {

    public static GdEntityStats zeroStats() {
        return new GdEntityStats()
                .withCost(BigDecimal.ZERO)
                .withCostWithTax(BigDecimal.ZERO)
                .withRevenue(BigDecimal.ZERO)
                .withShows(0L)
                .withClicks(0L)
                .withGoals(0L)
                .withCtr(BigDecimal.ZERO)
                .withAvgClickCost(BigDecimal.ZERO)
                .withAvgClickPosition(BigDecimal.ZERO)
                .withAvgShowPosition(BigDecimal.ZERO)
                .withAvgDepth(BigDecimal.ZERO)
                .withBounceRate(BigDecimal.ZERO)
                .withConversionRate(BigDecimal.ZERO)
                .withCpmPrice(BigDecimal.ZERO);
    }

    private static final GdiEntityStats ZERO_STATS_INTERNAL = new GdiEntityStats()
            .withCost(BigDecimal.ZERO)
            .withRevenue(BigDecimal.ZERO)
            .withShows(BigDecimal.valueOf(0L))
            .withClicks(BigDecimal.valueOf(0L))
            .withGoals(BigDecimal.valueOf(0L))
            .withCtr(BigDecimal.ZERO)
            .withAvgClickCost(BigDecimal.ZERO)
            .withAvgClickPosition(BigDecimal.ZERO)
            .withAvgShowPosition(BigDecimal.ZERO)
            .withAvgDepth(BigDecimal.ZERO)
            .withBounceRate(BigDecimal.ZERO)
            .withConversionRate(BigDecimal.ZERO)
            .withCpmPrice(BigDecimal.ZERO);

    private static final GdiEntityStats TEST_STAT_1_INTERNAL = new GdiEntityStats()
            .withCost(BigDecimal.TEN)
            .withCostWithTax(BigDecimal.TEN)
            .withRevenue(BigDecimal.ONE)
            .withShows(BigDecimal.valueOf(100L))
            .withClicks(BigDecimal.valueOf(10L))
            .withGoals(BigDecimal.valueOf(0L))
            .withCtr(BigDecimal.TEN)
            .withAvgClickCost(BigDecimal.ONE)
            .withAvgClickPosition(BigDecimal.TEN)
            .withAvgShowPosition(BigDecimal.ONE)
            .withAvgDepth(BigDecimal.TEN)
            .withBounceRate(BigDecimal.ONE)
            .withConversionRate(BigDecimal.TEN)
            .withAvgGoalCost(BigDecimal.ONE)
            .withProfitability(BigDecimal.TEN);

    private static final GdEntityStats TEST_STAT_1 = new GdEntityStats()
            .withCost(BigDecimal.TEN)
            .withCostWithTax(BigDecimal.TEN)
            .withRevenue(BigDecimal.ONE)
            .withShows(100L)
            .withClicks(10L)
            .withGoals(0L)
            .withCtr(BigDecimal.TEN)
            .withAvgClickCost(BigDecimal.ONE)
            .withAvgClickPosition(BigDecimal.TEN)
            .withAvgShowPosition(BigDecimal.ONE)
            .withAvgDepth(BigDecimal.TEN)
            .withBounceRate(BigDecimal.ONE)
            .withConversionRate(BigDecimal.TEN)
            .withAvgGoalCost(BigDecimal.ONE)
            .withProfitability(BigDecimal.TEN);

    private static final GdEntityStats TEST_STAT_2 = new GdEntityStats()
            .withCost(BigDecimal.ONE)
            .withCostWithTax(BigDecimal.TEN)
            .withRevenue(BigDecimal.TEN)
            .withShows(100L)
            .withClicks(1L)
            .withGoals(0L)
            .withCtr(BigDecimal.ONE)
            .withAvgClickCost(BigDecimal.TEN)
            .withAvgClickPosition(BigDecimal.ONE)
            .withAvgShowPosition(BigDecimal.TEN)
            .withAvgDepth(BigDecimal.ONE)
            .withBounceRate(BigDecimal.TEN)
            .withConversionRate(BigDecimal.ONE)
            .withAvgGoalCost(BigDecimal.TEN)
            .withProfitability(BigDecimal.ONE);

    private static final GdEntityStats EXPECTED_TOTAL_STATS = new GdEntityStats()
            .withCost(BigDecimal.valueOf(11))
            .withCostWithTax(BigDecimal.valueOf(20))
            .withRevenue(BigDecimal.valueOf(11))
            .withShows(200L)
            .withClicks(11L)
            .withGoals(0L)
            .withCpmPrice(BigDecimal.ZERO)
            .withCtr(toBigDecimal(5.5))
            .withAvgClickCost(toBigDecimal(1.0))
            .withAvgClickPosition(BigDecimal.ZERO)
            .withAvgShowPosition(BigDecimal.ZERO)
            .withAvgDepth(BigDecimal.ZERO)
            .withBounceRate(toBigDecimal(1.82))
            .withConversionRate(toBigDecimal(0.0))
            .withProfitability(toBigDecimal(0.0))
            .withCrr(toBigDecimal(100.0));

    private static BigDecimal toBigDecimal(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    public void testCalcTotalStats() {
        assertThat(
                StatHelper.calcTotalStats(Arrays.asList(TEST_STAT_1, TEST_STAT_2)), beanDiffer(EXPECTED_TOTAL_STATS));
    }

    @Test
    public void testInternalStatsToOuter() {
        assertThat(StatHelper.internalStatsToOuter(TEST_STAT_1_INTERNAL, GdCampaignType.TEXT),
                beanDiffer(TEST_STAT_1));
    }

    @Test
    public void testInternalStatsToOuterWithInternalCampaign() {
        assertThat(StatHelper.internalStatsToOuter(ZERO_STATS_INTERNAL, GdCampaignType.INTERNAL_FREE),
                beanDiffer(zeroStats()
                        .withCost(null)
                        .withCostWithTax(null)
                        .withAvgClickCost(null)));
    }
}
