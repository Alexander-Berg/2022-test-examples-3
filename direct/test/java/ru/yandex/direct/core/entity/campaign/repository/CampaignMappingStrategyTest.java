package ru.yandex.direct.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyPlace;

import static java.util.Arrays.asList;
import static org.assertj.core.util.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Тест проверяет успешность преобразования из строки в объект {@link StrategyData}
 */
@RunWith(Parameterized.class)
public class CampaignMappingStrategyTest {

    private static final LocalDate DATE = LocalDate.of(2016, 10, 13);

    @Parameterized.Parameter(0)
    public String dbJsonStrategy;

    @Parameterized.Parameter(1)
    public StrategyData objectJsonStrategy;

    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{"{\"name\":\"default\"}", new StrategyData().withName("default")},
                new Object[]{"{\"avg_bid\":10.1}", new StrategyData().withAvgBid(new BigDecimal("10.1"))},
                new Object[]{"{\"avg_cpa\":11.1}", new StrategyData().withAvgCpa(new BigDecimal("11.1"))},
                new Object[]{"{\"avg_cpi\":12.6}", new StrategyData().withAvgCpi(new BigDecimal("12.6"))},
                new Object[]{"{\"filter_avg_bid\":13.1}", new StrategyData().withFilterAvgBid(new BigDecimal("13.1"))},
                new Object[]{"{\"filter_avg_cpa\":14.1}", new StrategyData().withFilterAvgCpa(new BigDecimal("14.1"))},
                new Object[]{"{\"bid\":15.1}", new StrategyData().withBid(new BigDecimal("15.1"))},
                new Object[]{"{\"sum\":16.1}", new StrategyData().withSum(new BigDecimal("16.1"))},
                new Object[]{"{\"goal_id\":4100000}", new StrategyData().withGoalId(4100000L)},
                new Object[]{"{\"roi_coef\":17.1}", new StrategyData().withRoiCoef(new BigDecimal("17.1"))},
                new Object[]{"{\"profitability\":18.1}", new StrategyData().withProfitability(new BigDecimal("18.1"))},
                new Object[]{"{\"reserve_return\":80}", new StrategyData().withReserveReturn(80L)},
                new Object[]{"{\"limit_clicks\":200}", new StrategyData().withLimitClicks(200L)},
                new Object[]{"{\"place\":\"highest_place\"}",
                        new StrategyData().withPlace(StrategyPlace.HIGHEST_PLACE)},
                new Object[]{"{\"date\":\"" + DATE.toString() + "\"}", new StrategyData().withDate(DATE)},
                new Object[]{"{\"version\":1}", new StrategyData().withVersion(1L)},
                new Object[]{"{\"bid_second\":123}",
                        new StrategyData().withUnknownFields(newHashMap("bid_second", 123))}, // unknown field
                new Object[]{"{\"name\":\"autobudget_avg_click\",\"sum\":44.2,\"avg_bid\":4.1}",
                        new StrategyData().withAvgBid(new BigDecimal("4.1")).withSum(new BigDecimal("44.2")).withName(
                                "autobudget_avg_click")}
        );
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация стратегии в формат базы должна быть однозначной",
                CampaignMappings.strategyDataToDb(objectJsonStrategy),
                is(dbJsonStrategy));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация стратегии в формат модели должна быть однозначной",
                CampaignMappings.strategyDataFromDb(dbJsonStrategy),
                is(objectJsonStrategy));
    }
}
