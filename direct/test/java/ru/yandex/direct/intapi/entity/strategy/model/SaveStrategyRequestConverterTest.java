package ru.yandex.direct.intapi.entity.strategy.model;

import java.math.BigDecimal;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;

import static java.util.Arrays.asList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест проверяет успешность преобразования запроса SaveStrategyRequest в объект {@link DbStrategy}
 */
@RunWith(Parameterized.class)
public class SaveStrategyRequestConverterTest {
    @Parameterized.Parameter()
    public String jsonStrategy;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;

    @Parameterized.Parameters(name = "request format: {0}, strategy format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{"{\"name\":\"\",\"search\":{\"avg_bid\":4.2,\"sum\":330," +
                        "\"name\":\"autobudget_avg_click\"},\"net\":{\"name\":\"default\"},\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.BOTH, StrategyName.AUTOBUDGET_AVG_CLICK,
                                new StrategyData().withVersion(1L).withSum(new BigDecimal("330"))
                                        .withName("autobudget_avg_click")
                                        .withAvgBid(new BigDecimal("4.2")),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"avg_bid\":8.2,\"sum\":320," +
                        "\"name\":\"autobudget_avg_click\"},\"net\":{\"name\":\"stop\"},\"is_net_stop\":1}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.SEARCH, StrategyName.AUTOBUDGET_AVG_CLICK,
                                new StrategyData().withVersion(1L).withSum(new BigDecimal("320"))
                                        .withName("autobudget_avg_click")
                                        .withAvgBid(new BigDecimal("8.2")),
                                null)},
                new Object[]{"{\"search\":{\"name\":\"stop\"},\"net\":{\"name\":\"autobudget_avg_click\"," +
                        "\"avg_bid\":4.5,\"sum\":337},\"name\":\"different_places\",\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.CONTEXT, StrategyName.AUTOBUDGET_AVG_CLICK,
                                new StrategyData().withVersion(1L).withSum(new BigDecimal("337"))
                                        .withName("autobudget_avg_click")
                                        .withAvgBid(new BigDecimal("4.5")),
                                CampOptionsStrategy.DIFFERENT_PLACES)},
                new Object[]{"{\"search\":{\"name\":\"stop\"},\"net\":{\"name\":\"maximum_coverage\"}," +
                        "\"name\":\"different_places\",\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.NO, CampaignsPlatform.CONTEXT, StrategyName.DEFAULT_,
                                new StrategyData().withVersion(1L).withName("default"),
                                CampOptionsStrategy.DIFFERENT_PLACES)},
                new Object[]{"{\"name\":\"\",\"search\":{\"name\":\"default\"},\"net\":{\"name\":\"stop\"}," +
                        "\"is_net_stop\":1}",
                        strategy(CampaignsAutobudget.NO, CampaignsPlatform.SEARCH, StrategyName.DEFAULT_,
                                new StrategyData().withVersion(1L).withName("default"),
                                null)},
                new Object[]{"{\"net\":{\"name\":\"maximum_coverage\"},\"search\":{\"name\":\"default\"}," +
                        "\"name\":\"different_places\",\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.NO, CampaignsPlatform.BOTH, StrategyName.DEFAULT_,
                                new StrategyData().withVersion(1L).withName("default"),
                                CampOptionsStrategy.DIFFERENT_PLACES)},
                new Object[]{"{\"name\":\"\",\"search\":{\"name\":\"default\"},\"net\":{\"name\":\"default\"}," +
                        "\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.NO, CampaignsPlatform.BOTH, StrategyName.DEFAULT_,
                                new StrategyData().withVersion(1L).withName("default"),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"avg_cpa\":43.1,\"bid\":12,\"sum\":443," +
                        "\"goal_id\":\"18520930\",\"name\":\"autobudget_avg_cpa\"},\"net\":{\"name\":\"default\"}," +
                        "\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.BOTH, StrategyName.AUTOBUDGET_AVG_CPA,
                                new StrategyData().withVersion(1L).withName("autobudget_avg_cpa")
                                        .withAvgCpa(new BigDecimal("43.1"))
                                        .withGoalId(18520930L)
                                        .withBid(new BigDecimal("12"))
                                        .withSum(new BigDecimal("443")),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"avg_cpa\":43.1,\"bid\":12,\"sum\":443,\"goal_id\":\"0\"," +
                        "\"name\":\"autobudget_avg_cpa\"},\"net\":{\"name\":\"stop\"},\"is_net_stop\":1}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.SEARCH, StrategyName.AUTOBUDGET_AVG_CPA,
                                new StrategyData().withVersion(1L).withName("autobudget_avg_cpa")
                                        .withAvgCpa(new BigDecimal("43.1"))
                                        .withGoalId(0L)
                                        .withBid(new BigDecimal("12"))
                                        .withSum(new BigDecimal("443")),
                                null)},
                new Object[]{"{\"search\":{\"name\":\"stop\"},\"net\":{\"name\":\"autobudget_avg_cpa\",\"avg_cpa\":43" +
                        ".1,\"bid\":\"\",\"sum\":\"\",\"goal_id\":\"0\"},\"name\":\"different_places\"," +
                        "\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.CONTEXT, StrategyName.AUTOBUDGET_AVG_CPA,
                                new StrategyData().withVersion(1L).withName("autobudget_avg_cpa")
                                        .withAvgCpa(new BigDecimal("43.1"))
                                        .withGoalId(0L),
                                CampOptionsStrategy.DIFFERENT_PLACES)},
                new Object[]{"{\"name\":\"\",\"search\":{\"roi_coef\":21.12,\"profitability\":65.4," +
                        "\"reserve_return\":70,\"bid\":57.7,\"sum\":500.7,\"goal_id\":\"22764440\"," +
                        "\"name\":\"autobudget_roi\"},\"net\":{\"name\":\"default\"},\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.BOTH, StrategyName.AUTOBUDGET_ROI,
                                new StrategyData().withVersion(1L).withName("autobudget_roi").withGoalId(22764440L)
                                        .withBid(new BigDecimal("57.7")).withSum(new BigDecimal("500.7"))
                                        .withRoiCoef(new BigDecimal("21.12"))
                                        .withProfitability(new BigDecimal("65.4"))
                                        .withReserveReturn(70L),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"bid\":47.7,\"sum\":458.7,\"goal_id\":\"\"," +
                        "\"name\":\"autobudget\"},\"net\":{\"name\":\"default\"},\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.BOTH, StrategyName.AUTOBUDGET,
                                new StrategyData().withVersion(1L).withName("autobudget")
                                        .withBid(new BigDecimal("47.7"))
                                        .withSum(new BigDecimal("458.7")),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"bid\":47.7,\"sum\":458.7,\"goal_id\":\"22764695\"," +
                        "\"name\":\"autobudget\"},\"net\":{\"name\":\"default\"},\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.BOTH, StrategyName.AUTOBUDGET,
                                new StrategyData().withVersion(1L).withName("autobudget")
                                        .withBid(new BigDecimal("47.7"))
                                        .withSum(new BigDecimal("458.7"))
                                        .withGoalId(22764695L),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"limit_clicks\":300,\"bid\":47.7,\"avg_bid\":\"\"," +
                        "\"name\":\"autobudget_week_bundle\"},\"net\":{\"name\":\"default\"},\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.BOTH, StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                                new StrategyData().withVersion(1L).withName("autobudget_week_bundle")
                                        .withBid(new BigDecimal("47.7"))
                                        .withLimitClicks(300L),
                                null)},
                new Object[]{"{\"name\":\"\",\"search\":{\"limit_clicks\":300,\"bid\":\"\",\"avg_bid\":47.7," +
                        "\"name\":\"autobudget_week_bundle\"},\"net\":{\"name\":\"stop\"},\"is_net_stop\":1}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.SEARCH, StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                                new StrategyData().withVersion(1L).withName("autobudget_week_bundle")
                                        .withAvgBid(new BigDecimal("47.7"))
                                        .withLimitClicks(300L),
                                null)},
                new Object[]{"{\"search\":{\"name\":\"stop\"},\"net\":{\"name\":\"autobudget_week_bundle\"," +
                        "\"limit_clicks\":300,\"bid\":\"\",\"avg_bid\":\"\"},\"name\":\"different_places\"," +
                        "\"is_net_stop\":0}",
                        strategy(CampaignsAutobudget.YES, CampaignsPlatform.CONTEXT,
                                StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                                new StrategyData().withVersion(1L).withName("autobudget_week_bundle")
                                        .withLimitClicks(300L),
                                CampOptionsStrategy.DIFFERENT_PLACES)}
        );
    }

    @Test
    public void testToDbStrategy() {
        SaveStrategyRequest request = new SaveStrategyRequest();
        request.setJsonStrategy(jsonStrategy);
        Assertions.assertThat(SaveStrategyRequestConverter.toDbStrategy(request))
                .is(matchedBy(beanDiffer(dbStrategy)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    private static DbStrategy strategy(CampaignsAutobudget autobudget, CampaignsPlatform platform,
                                       StrategyName strategyName, StrategyData strategyData,
                                       CampOptionsStrategy strategyOptions) {
        var strategy = new DbStrategy();
        strategy.setAutobudget(autobudget);
        strategy.setPlatform(platform);
        strategy.setStrategy(strategyOptions);
        strategy.setStrategyName(strategyName);
        strategy.setStrategyData(strategyData);
        return strategy;
    }
}
