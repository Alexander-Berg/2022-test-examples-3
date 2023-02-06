package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignOptsStrategyName;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.grid.model.campaign.GdiDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetPeriod;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignFlatStrategy;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgClick;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpa;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpaPerCamp;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpaPerFilter;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpcPerCamp;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpcPerFilter;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpi;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpm;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyPeriodFixBid;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyRoi;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyType;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyWeekBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyWeekBundle;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyOptimizeClicks;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyOptimizeConversions;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyOptimizeInstalls;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyType;
import ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorFacade;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorHelper.STRATEGIES_EXTRACTORS_BY_TYPES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class GridCampaignAggregationFieldsServiceFlatStrategyTest {
    private static final BigDecimal TEN = BigDecimal.valueOf(10.0);
    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forClasses(BigDecimal.class)
            .useDiffer(new BigDecimalDiffer());

    @Parameterized.Parameter
    public GdiCampaign campaign;
    @Parameterized.Parameter(1)
    public GdCampaignFlatStrategy expectedFlatStrategy;
    @Parameterized.Parameter(2)
    public GdCampaignFlatStrategy expectedStrategy;
    @Parameterized.Parameter(3)
    public Class<? extends Throwable> throwable;
    private static GridCampaignStrategyService service;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {//0
                        defaultCampaign(1),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//1
                        defaultCampaign(1)
                                .withDayBudget(TEN)
                                .withDayBudgetShowMode(GdiDayBudgetShowMode.STRETCHED),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN.setScale(1, RoundingMode.HALF_UP))
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.STRETCHED))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN.setScale(1, RoundingMode.HALF_UP))
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.STRETCHED))
                                .withIsAutoBudget(false),
                        null
                },
                {//2
                        defaultCampaign(1)
                                .withDayBudget(TEN)
                                .withPlatform(CampaignsPlatform.CONTEXT)
                                .withDayBudgetShowMode(GdiDayBudgetShowMode.DEFAULT_),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN.setScale(1, RoundingMode.HALF_UP))
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN.setScale(1, RoundingMode.HALF_UP))
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//3
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET)
                                .withStrategyData("{\"bid\": 50, \"goal_id\": 30, \"sum\": 10, \"version\": 1}"),
                        new GdCampaignStrategyWeekBudget()
                                .withStrategyType(GdStrategyType.WEEK_BUDGET)
                                .withType(GdCampaignStrategyType.WEEK_BUDGET)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withBid(BigDecimal.valueOf(50_000L, 3))
                                .withGoalId(30L)
                                .withIsAutoBudget(true),
                        new GdStrategyOptimizeConversions()
                                .withStrategyType(GdStrategyType.OPTIMIZE_CONVERSIONS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withBid(BigDecimal.valueOf(50_000L, 3))
                                .withGoalId(30L)
                                .withIsAutoBudget(true),
                        null
                },
                {//4
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                                .withPlatform(CampaignsPlatform.SEARCH)
                                .withStrategyData(
                                "{\"avg_bid\": 50, \"bid\": 40, \"limit_clicks\": 10, \"version\": 1}"),
                        new GdCampaignStrategyWeekBundle()
                                .withStrategyType(GdStrategyType.WEEK_BUNDLE)
                                .withType(GdCampaignStrategyType.WEEK_BUNDLE)
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withAvgBid(BigDecimal.valueOf(50_000L, 3))
                                .withBid(BigDecimal.valueOf(40_000L, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withClicksLimit(10L)
                                .withIsAutoBudget(true),
                        new GdStrategyOptimizeClicks()
                                .withStrategyType(GdStrategyType.OPTIMIZE_CLICKS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withAvgBid(BigDecimal.valueOf(50_000L, 3))
                                .withBid(BigDecimal.valueOf(40_000L, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withClicksLimit(10L)
                                .withIsAutoBudget(true),
                        null
                },
                {//5
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.DEFAULT_)
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//6
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.DEFAULT_)
                                .withPlatform(CampaignsPlatform.CONTEXT),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//7
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.DEFAULT_)
                                .withOptsStrategyName(GdiCampaignOptsStrategyName.DIFFERENT_PLACES)
                                .withPlatform(CampaignsPlatform.SEARCH),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withSeparateBidding(false)
                                .withCanSetNetworkBids(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//8
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.DEFAULT_)
                                .withOptsStrategyName(GdiCampaignOptsStrategyName.DIFFERENT_PLACES)
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.DEFAULT)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(true)
                                .withCanSetNetworkBids(true)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(true)
                                .withCanSetNetworkBids(true)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//9
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.NO_PREMIUM)
                                .withPlatform(CampaignsPlatform.BOTH)
                                .withDayBudget(BigDecimal.TEN),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.NO_PREMIUM)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyManual()
                                .withStrategyType(GdStrategyType.NO_PREMIUM)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withSeparateBidding(false)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//10
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_ROI)
                                .withStrategyData(
                                        "{\"bid\": null, \"sum\": null, \"name\": \"autobudget_roi\", \"goal_id\": " +
                                                "\"2586688\", \"version\": 1, \"roi_coef\": 0, \"profitability\": " +
                                                "null, \"reserve_return\": 100}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyRoi()
                                .withStrategyType(GdStrategyType.ROI)
                                .withType(GdCampaignStrategyType.ROI)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withRoiCoef(BigDecimal.valueOf(0, 3))
                                .withReserveReturn(100L)
                                .withGoalId(2586688L)
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyRoi()
                                .withStrategyType(GdStrategyType.ROI)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withRoiCoef(BigDecimal.valueOf(0, 3))
                                .withReserveReturn(100L)
                                .withGoalId(2586688L)
                                .withIsAutoBudget(true),
                        null
                },
                {//11
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_ROI)
                                .withStrategyData(
                                        "{\"bid\": 0.28, \"sum\": 200, \"name\": \"autobudget_roi\", \"goal_id\": " +
                                                "\"13150999\", \"version\": 1, \"roi_coef\": -0.52, " +
                                                "\"profitability\": 50, \"reserve_return\": 100}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyRoi()
                                .withStrategyType(GdStrategyType.ROI)
                                .withType(GdCampaignStrategyType.ROI)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBid(BigDecimal.valueOf(280, 3))
                                .withRoiCoef(BigDecimal.valueOf(-520, 3))
                                .withProfitability(BigDecimal.valueOf(50000, 3))
                                .withReserveReturn(100L)
                                .withGoalId(13150999L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(200000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyRoi()
                                .withStrategyType(GdStrategyType.ROI)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBid(BigDecimal.valueOf(280, 3))
                                .withRoiCoef(BigDecimal.valueOf(-520, 3))
                                .withProfitability(BigDecimal.valueOf(50000, 3))
                                .withReserveReturn(100L)
                                .withGoalId(13150999L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(200000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//12
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                                .withStrategyData(
                                        "{\"sum\": null, \"name\": \"autobudget_avg_click\", \"avg_bid\": 0.09, " +
                                                "\"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgClick()
                                .withStrategyType(GdStrategyType.AVG_CLICK)
                                .withType(GdCampaignStrategyType.AVG_CLICK)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgBid(BigDecimal.valueOf(90, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdStrategyOptimizeClicks()
                                .withStrategyType(GdStrategyType.OPTIMIZE_CLICKS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgBid(BigDecimal.valueOf(90, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//13
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPA)
                                .withStrategyData(
                                        "{\"bid\": null, \"sum\": 3000, \"name\": \"autobudget_avg_cpa\", " +
                                                "\"avg_cpa\": 300, \"goal_id\": \"24319780\", \"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpa()
                                .withStrategyType(GdStrategyType.AVG_CPA)
                                .withType(GdCampaignStrategyType.AVG_CPA)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpa(BigDecimal.valueOf(300000, 3))
                                .withGoalId(24319780L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(3000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdStrategyOptimizeConversions()
                                .withStrategyType(GdStrategyType.OPTIMIZE_CONVERSIONS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpa(BigDecimal.valueOf(300000, 3))
                                .withGoalId(24319780L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(3000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//14
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                                .withStrategyData(
                                        "{\"bid\": null, \"sum\": 15000, \"name\": \"autobudget_avg_cpa_per_camp\", " +
                                                "\"avg_cpa\": 7000, \"goal_id\": \"6931863\", \"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpaPerCamp()
                                .withStrategyType(GdStrategyType.AVG_CPA_PER_CAMP)
                                .withType(GdCampaignStrategyType.AVG_CPA_PER_CAMP)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpa(BigDecimal.valueOf(7000000, 3))
                                .withGoalId(6931863L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(15000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdStrategyOptimizeConversions()
                                .withStrategyType(GdStrategyType.OPTIMIZE_CONVERSIONS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpa(BigDecimal.valueOf(7000000, 3))
                                .withGoalId(6931863L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(15000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//15
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
                                .withStrategyData(
                                        "{\"bid\": null, \"sum\": 16000, \"name\": \"autobudget_avg_cpa_per_filter\"," +
                                                " \"goal_id\": \"1136443\", \"version\": 1, " +
                                                "\"filter_avg_cpa\": 700}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpaPerFilter()
                                .withStrategyType(GdStrategyType.AVG_CPA_PER_FILTER)
                                .withType(GdCampaignStrategyType.AVG_CPA_PER_FILTER)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withFilterAvgCpa(BigDecimal.valueOf(700000, 3))
                                .withGoalId(1136443L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(16000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpaPerFilter()
                                .withStrategyType(GdStrategyType.AVG_CPA_PER_FILTER)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withFilterAvgCpa(BigDecimal.valueOf(700000, 3))
                                .withGoalId(1136443L)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(16000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//16
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                                .withStrategyData(
                                        "{\"bid\": null, \"sum\": 838.983, \"name\": " +
                                                "\"autobudget_avg_cpc_per_camp\", \"avg_bid\": 5, \"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpcPerCamp()
                                .withStrategyType(GdStrategyType.AVG_CPC_PER_CAMP)
                                .withType(GdCampaignStrategyType.AVG_CPC_PER_CAMP)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgBid(BigDecimal.valueOf(5000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(838983, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpcPerCamp()
                                .withStrategyType(GdStrategyType.AVG_CPC_PER_CAMP)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgBid(BigDecimal.valueOf(5000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(838983, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//17
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
                                .withStrategyData(
                                        "{\"bid\": 6, \"sum\": null, \"name\": \"autobudget_avg_cpc_per_filter\", " +
                                                "\"version\": 1, \"filter_avg_bid\": 4}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpcPerFilter()
                                .withStrategyType(GdStrategyType.AVG_CPC_PER_FILTER)
                                .withType(GdCampaignStrategyType.AVG_CPC_PER_FILTER)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBid(BigDecimal.valueOf(6000, 3))
                                .withFilterAvgBid(BigDecimal.valueOf(4000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpcPerFilter()
                                .withStrategyType(GdStrategyType.AVG_CPC_PER_FILTER)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBid(BigDecimal.valueOf(6000, 3))
                                .withFilterAvgBid(BigDecimal.valueOf(4000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.ZERO)
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//18
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CPI)
                                .withStrategyData(
                                        "{\"bid\": null, \"sum\": 200, \"name\": \"autobudget_avg_cpi\", \"avg_cpi\":" +
                                                " 1.5, \"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpi()
                                .withStrategyType(GdStrategyType.AVG_CPI)
                                .withType(GdCampaignStrategyType.AVG_CPI)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpi(BigDecimal.valueOf(1500, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(200000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdStrategyOptimizeInstalls()
                                .withStrategyType(GdStrategyType.OPTIMIZE_INSTALLS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpi(BigDecimal.valueOf(1500, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(200000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//19
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.CPM_DEFAULT)
                                .withStrategyData("{\"name\": \"cpm_default\", \"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH)
                                .withDayBudget(BigDecimal.TEN),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_DEFAULT)
                                .withType(GdCampaignStrategyType.CPM_DEFAULT)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_DEFAULT)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.TEN)
                                        .withPeriod(GdCampaignBudgetPeriod.DAY)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(false),
                        null
                },
                {//20
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_MAX_REACH)
                                .withStrategyData(
                                        "{\"sum\": 4566, \"name\": \"autobudget_max_reach\", \"avg_cpm\": 45, " +
                                                "\"version\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_REACH)
                                .withType(GdCampaignStrategyType.CPM_MAX_REACH)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpm(BigDecimal.valueOf(45000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(4566000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_REACH)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpm(BigDecimal.valueOf(45000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(4566000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//21
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                        "{\"name\": \"autobudget_max_reach_custom_period\", \"start\": " +
                                                "\"2018-02-06\", \"budget\": 60000, \"finish\": \"2018-03-07\", " +
                                                "\"avg_cpm\": 555, \"version\": 1, \"auto_prolongation\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_REACH_CUSTOM_PERIOD)
                                .withType(GdCampaignStrategyType.CPM_MAX_REACH_CUSTOM_PERIOD)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpm(BigDecimal.valueOf(555000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(60000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                                        .withStart(LocalDate.parse("2018-02-06"))
                                        .withFinish(LocalDate.parse("2018-03-07"))
                                        .withAutoProlongation(true))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_REACH_CUSTOM_PERIOD)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpm(BigDecimal.valueOf(555000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(60000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                                        .withStart(LocalDate.parse("2018-02-06"))
                                        .withFinish(LocalDate.parse("2018-03-07"))
                                        .withAutoProlongation(true))
                                .withIsAutoBudget(true),
                        null
                },
                {//22
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_MAX_IMPRESSIONS)
                                .withStrategyData(
                                        "{\"sum\": 500, \"name\": \"autobudget_max_impressions\", \"avg_cpm\": 300, " +
                                                "\"version\": 1}")
                                .withPlatform(CampaignsPlatform.CONTEXT),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_IMPRESSIONS)
                                .withType(GdCampaignStrategyType.CPM_MAX_IMPRESSIONS)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withAvgCpm(BigDecimal.valueOf(300000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(500000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_IMPRESSIONS)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withAvgCpm(BigDecimal.valueOf(300000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(500000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                                .withIsAutoBudget(true),
                        null
                },
                {//23
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withStrategyData(
                                        "{\"name\": \"autobudget_max_impressions_custom_period\", \"start\": " +
                                                "\"2018-02-06\", \"budget\": 10000, \"finish\": \"2018-03-07\", " +
                                                "\"avg_cpm\": 5, \"version\": 1, \"auto_prolongation\": 1}")
                                .withPlatform(CampaignsPlatform.BOTH),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withType(GdCampaignStrategyType.CPM_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpm(BigDecimal.valueOf(5000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(10000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                                        .withStart(LocalDate.parse("2018-02-06"))
                                        .withFinish(LocalDate.parse("2018-03-07"))
                                        .withAutoProlongation(true))
                                .withIsAutoBudget(true),
                        new GdCampaignStrategyAvgCpm()
                                .withStrategyType(GdStrategyType.CPM_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withAvgCpm(BigDecimal.valueOf(5000, 3))
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(10000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                                        .withStart(LocalDate.parse("2018-02-06"))
                                        .withFinish(LocalDate.parse("2018-03-07"))
                                        .withAutoProlongation(true))
                                .withIsAutoBudget(true),
                        null
                },
                {//24
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.PERIOD_FIX_BID)
                                .withStrategyData(
                                        "{\"name\": \"period_fix_bid\", \"start\": " +
                                                "\"2018-02-06\", \"budget\": 10000, \"finish\": \"2018-03-07\", " +
                                                "\"version\": 1, \"auto_prolongation\": 1}")
                                .withPlatform(CampaignsPlatform.CONTEXT),
                        new GdCampaignStrategyPeriodFixBid()
                                .withStrategyType(GdStrategyType.PERIOD_FIX_BID)
                                .withType(GdCampaignStrategyType.PERIOD_FIX_BID)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(10000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                                        .withStart(LocalDate.parse("2018-02-06"))
                                        .withFinish(LocalDate.parse("2018-03-07"))
                                        .withAutoProlongation(true))
                                .withIsAutoBudget(false),
                        new GdCampaignStrategyPeriodFixBid()
                                .withStrategyType(GdStrategyType.PERIOD_FIX_BID)
                                .withType(GdCampaignStrategyType.NO_PREMIUM)
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withBudget(new GdCampaignBudget()
                                        .withSum(BigDecimal.valueOf(10000000, 3))
                                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                                        .withStart(LocalDate.parse("2018-02-06"))
                                        .withFinish(LocalDate.parse("2018-03-07"))
                                        .withAutoProlongation(true))
                                .withIsAutoBudget(false),
                        null
                },
                {//25
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.MIN_PRICE),
                        null,
                        null,
                        IllegalArgumentException.class
                },
                {//26
                        defaultCampaign(1)
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_MEDIA),
                        null,
                        null,
                        IllegalArgumentException.class
                },
        });
    }

    @BeforeClass
    public static void beforeClass() {
        GdStrategyExtractorFacade gdStrategyExtractorFacade =
                new GdStrategyExtractorFacade(STRATEGIES_EXTRACTORS_BY_TYPES);
        service = new GridCampaignStrategyService(gdStrategyExtractorFacade);
    }

    @Test
    public void testStrategyGeneration() {
        if (throwable == null) {
            GdCampaignFlatStrategy flatStrategy = service.extractFlatStrategy(campaign);
            GdCampaignFlatStrategy strategy = service.extractStrategy(campaign);
            SoftAssertions softAssertions = new SoftAssertions();
            softAssertions.assertThat(strategy)
                    .is(matchedBy(beanDiffer(expectedStrategy).useCompareStrategy(COMPARE_STRATEGY)));
            softAssertions.assertThat(flatStrategy)
                    .is(matchedBy(beanDiffer(expectedFlatStrategy).useCompareStrategy(COMPARE_STRATEGY)));
            softAssertions.assertAll();
        } else {
            assertThatThrownBy(() -> service.extractFlatStrategy(campaign)).isInstanceOf(throwable);
        }
    }
}
