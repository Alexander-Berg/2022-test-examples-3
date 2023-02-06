package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BY_ALL_GOALS_GOAL_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.CRR_MAX;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.CRR_MIN;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MAX;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MIN;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ROI_COEF_MIN;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.avgBidAndBidTogetherAreProhibited;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.incorrectReserveReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.payForConversionDoesNotAllowAllGoals;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.unableToUseCurrentMeaningfulGoalsForOptimization;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.CAMPAIGN_COUNTERS_AVAILABLE_GOALS;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.CAMPAIGN_COUNTER_GOAL_1;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.TURBOLANDING_INTERNAL_COUNTER_GOAL_1;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autoBudgetWeekBundle;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetCrrStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaPayForConversionStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.feature.FeatureName.ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES;
import static ru.yandex.direct.feature.FeatureName.AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.FIX_CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на корректность методов валидации параметров
 */
@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorStartegyParametersTest {

    private static final int TEST_SHARD = 2;
    private static final BigDecimal AVG_CPA = new BigDecimal("1000");
    private static final Long TURBOLANDING_ID = 12345L;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ClientService clientService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private Currency currency;
    private CampaignWithCustomStrategy campaign;

    private Supplier<List<BannerWithSystemFields>> getCampaignBannersSupplier = Collections::emptyList;
    private Supplier<List<AdGroupSimple>> campaignAdGroupsSupplier = Collections::emptyList;
    private Function<List<BannerWithSystemFields>, List<SitelinkSet>> getBannersSiteLinkSetsFunction =
            banners -> Collections.emptyList();

    private Set<String> availableFeatures = ImmutableSet.of(CRR_STRATEGY_ALLOWED.getName(),
            AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED.getName());

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo);
        currency = clientService.getWorkCurrency(clientInfo.getClientId());
        campaign = (CampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(TEST_SHARD,
                singletonList(campaignInfo.getCampaignId())).get(0);
    }

    @Test
    public void sum_NotNull() {
        DbStrategy dbStrategy = autobudgetStrategy(null, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.SUM), notNull())));
    }

    @Test
    public void sum_LessThanMin() {
        BigDecimal min = currency.getMinAutobudget();
        DbStrategy dbStrategy = averageClickStrategy(new BigDecimal("100"), min.subtract(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.SUM),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void sum_GreaterThanMax() {
        BigDecimal max = currency.getMaxAutobudget();
        DbStrategy dbStrategy = averageClickStrategy(new BigDecimal("100"), max.add(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.SUM),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void sum_payForConversionStrategyWithExtendedMode_NotLessThanMin() {
        BigDecimal minSum = AVG_CPA.multiply(BigDecimal.valueOf(PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO));
        DbStrategy dbStrategy = averageCpaPayForConversionStrategy(AVG_CPA,
                CAMPAIGN_COUNTER_GOAL_1, minSum.subtract(BigDecimal.ONE), null);

        var vr = validate(dbStrategy,
                ImmutableSet.of(), getCampaignBannersSupplier,
                getBannersSiteLinkSetsFunction);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void bid_LessThanMin() {
        BigDecimal min = campaignType == CampaignType.PERFORMANCE
                ? currency.getMinCpcCpaPerformance()
                : currency.getMinPrice();
        DbStrategy dbStrategy = averageCpaStrategy(new BigDecimal("2000"), CAMPAIGN_COUNTER_GOAL_1, new BigDecimal(
                "10000"), min.subtract(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.BID),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void bid_NotWeekBundleStrategy_GreaterThanMax() {
        BigDecimal max = currency.getMaxAutobudgetBid();
        DbStrategy dbStrategy = averageCpaStrategy(new BigDecimal("2000"), CAMPAIGN_COUNTER_GOAL_1, new BigDecimal(
                "10000"), max.add(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.BID),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void bid_WeekBundleStrategy_GreaterThanMax() {
        BigDecimal max = currency.getMaxPrice();
        DbStrategy dbStrategy = autoBudgetWeekBundle(1000L, max.add(BigDecimal.ONE), new BigDecimal("100"));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.BID),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void bid_WeekBudgetLessThan() {
        BigDecimal sum = new BigDecimal("1000");
        DbStrategy dbStrategy = averageCpaStrategy(new BigDecimal("700"), CAMPAIGN_COUNTER_GOAL_1, sum,
                sum.add(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.BID), weekBudgetLessThan())));
    }

    @Test
    public void avgBid_NotNull() {
        DbStrategy dbStrategy = averageClickStrategy(null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_BID), notNull())));
    }

    @Test
    public void avgBid_LessThanMin() {
        BigDecimal min = campaignType == CampaignType.PERFORMANCE
                ? currency.getMinCpcCpaPerformance()
                : currency.getMinAutobudgetAvgPrice();
        DbStrategy dbStrategy = autoBudgetWeekBundle(1000L, null, min.subtract(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_BID),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void avgBid_GreaterThanMax() {
        BigDecimal max = currency.getMaxAutobudgetBid();
        DbStrategy dbStrategy = autoBudgetWeekBundle(1000L, null, max.add(BigDecimal.ONE));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_BID),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void avgBid_WeekBudgetLessThan() {
        BigDecimal sum = new BigDecimal("1000");
        DbStrategy dbStrategy = averageClickStrategy(sum.add(BigDecimal.ONE), sum);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_BID),
                weekBudgetLessThan())));
    }

    @Test
    public void avgBid_AvgBidAndBidTogether() {
        DbStrategy dbStrategy = autoBudgetWeekBundle(1000L, new BigDecimal("100"), new BigDecimal("100"));
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_BID),
                avgBidAndBidTogetherAreProhibited())));
    }

    @Test
    public void avgCpa_NotNull() {
        DbStrategy dbStrategy = averageCpaStrategy(null, CAMPAIGN_COUNTER_GOAL_1, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_CPA), notNull())));
    }

    @Test
    public void avgCpa_LessThanMin() {
        BigDecimal min = campaignType == CampaignType.PERFORMANCE
                ? currency.getMinCpcCpaPerformance()
                : currency.getMinAutobudgetAvgCpa();
        DbStrategy dbStrategy = averageCpaStrategy(min.subtract(BigDecimal.ONE), CAMPAIGN_COUNTER_GOAL_1, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_CPA),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void avgCpa_GreaterThanMax() {
        BigDecimal max = currency.getAutobudgetAvgCpaWarning();
        DbStrategy dbStrategy = averageCpaStrategy(max.add(BigDecimal.ONE), CAMPAIGN_COUNTER_GOAL_1, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_CPA),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void avgCpa_WeekBudgetLessThan() {
        BigDecimal sum = new BigDecimal("1000");
        DbStrategy dbStrategy = averageCpaStrategy(sum.add(BigDecimal.ONE), CAMPAIGN_COUNTER_GOAL_1, sum, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_CPA),
                weekBudgetLessThan())));
    }

    @Test
    public void avgCpa_payForConversionStrategyWithExtendedMode_GreaterThanMax() {
        BigDecimal avgCpaPayForConversionUpperBound = currency.getAutobudgetPayForConversionAvgCpaWarning();
        DbStrategy dbStrategy = averageCpaPayForConversionStrategy(avgCpaPayForConversionUpperBound.add(BigDecimal.ONE),
                TURBOLANDING_INTERNAL_COUNTER_GOAL_1, null, null);

        var vr = validate(dbStrategy,
                ImmutableSet.of(), getCampaignBannersSupplier,
                getBannersSiteLinkSetsFunction);

        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.AVG_CPA),
                lessThanOrEqualTo(avgCpaPayForConversionUpperBound))));
    }

    @Test
    public void payForConversionExtendedMode_enabledOnCampainWithoutBanners() {
        DbStrategy dbStrategy = averageCpaPayForConversionStrategy(AVG_CPA,
                TURBOLANDING_INTERNAL_COUNTER_GOAL_1,
                AVG_CPA.multiply(BigDecimal.valueOf(PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO)), null);

        //передаем валидатору пустой список баннеров
        var vr = validate(dbStrategy,
                ImmutableSet.of(),
                getCampaignBannersSupplier, getBannersSiteLinkSetsFunction);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void payForConversion_isNullForAutobudgetCrr_ifFeatureDisabled() {
        DbStrategy dbStrategy = autobudgetCrrStrategy(null, 100L, CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void crrStrategy_AllowAllMeaningfulGoalsForPayForConversionStrategiesFlag_WithoutPayForConversion_AllowAllMeaningfulGoals() {
        DbStrategy dbStrategy = autobudgetCrrStrategy(null, 100L, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);
        var vr = validate(dbStrategy,
                ImmutableSet.of(ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES.getName(),
                        CRR_STRATEGY_ALLOWED.getName()),
                getCampaignBannersSupplier, getBannersSiteLinkSetsFunction);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID),
                unableToUseCurrentMeaningfulGoalsForOptimization())));
    }

    @Test
    public void crrStrategy_AllowAllMeaningfulGoalsForPayForConversionStrategies_PayForConversion_AllowAllMeaningfulGoals() {
        DbStrategy dbStrategy = autobudgetCrrStrategy(null, 100L, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);
        dbStrategy.getStrategyData().withPayForConversion(true);
        var vr = validate(dbStrategy,
                ImmutableSet.of(ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES.getName(),
                        CRR_STRATEGY_ALLOWED.getName(),
                        FIX_CRR_STRATEGY_ALLOWED.getName()),
                getCampaignBannersSupplier, getBannersSiteLinkSetsFunction);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID),
                unableToUseCurrentMeaningfulGoalsForOptimization())));
    }

    @Test
    public void autobudgetStrategy_WithoutPayForConversion_AllowAllMeaningfulGoalsForPayForConversion_MeaningfulGoalsGoalId() {
        DbStrategy dbStrategy = autobudgetStrategy(new BigDecimal("1000"), null, MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);
        var vr = validate(dbStrategy,
                ImmutableSet.of(ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES.getName(),
                        AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED.getName()),
                getCampaignBannersSupplier, getBannersSiteLinkSetsFunction);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID),
                unableToUseCurrentMeaningfulGoalsForOptimization())));
    }

    @Test
    public void goalId_NotNull() {
        DbStrategy dbStrategy = averageCpaStrategy(new BigDecimal("1000"), null, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID), notNull())));
    }

    @Test
    public void goalId_ValidId() {
        DbStrategy dbStrategy = averageCpaStrategy(new BigDecimal("1000"), -1L, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID), validId())));
    }

    @Test
    public void goalId_Exists() {
        DbStrategy dbStrategy = averageCpaStrategy(new BigDecimal("1000"), 100500L, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID), objectNotFound())));
    }

    @Test
    public void goalId_payForConversionStrategyExtendedMode_notTurbolandingGoal() {
        DbStrategy dbStrategy = averageCpaPayForConversionStrategy(AVG_CPA,
                CAMPAIGN_COUNTER_GOAL_1, null, null);

        var vr = validate(dbStrategy,
                ImmutableSet.of(), getCampaignBannersSupplier,
                getBannersSiteLinkSetsFunction);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void goalId_payForConversionStrategyExtendedMode_allGoals() {
        DbStrategy dbStrategy = averageCpaPayForConversionStrategy(AVG_CPA,
                BY_ALL_GOALS_GOAL_ID, null, null);

        var vr = validate(dbStrategy,
                ImmutableSet.of(), getCampaignBannersSupplier,
                getBannersSiteLinkSetsFunction);

        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID),
                payForConversionDoesNotAllowAllGoals())));
    }

    @Test
    public void goalId_payForConversion_meaningfulGoalsOptimizationGoalId() {
        DbStrategy dbStrategy = averageCpaPayForConversionStrategy(AVG_CPA,
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, null, null);

        var vr = validate(dbStrategy,
                ImmutableSet.of(ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES.getName()),
                getCampaignBannersSupplier, getBannersSiteLinkSetsFunction);

        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.GOAL_ID),
                payForConversionDoesNotAllowAllGoals())));
    }

    @Test
    public void roiCoef_NotNull() {
        DbStrategy dbStrategy = autobudgetRoiStrategy(null, null, null, 30L, null, CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.ROI_COEF), notNull())));
    }

    @Test
    public void roiCoef_LessThanMin() {
        BigDecimal min = ROI_COEF_MIN;
        DbStrategy dbStrategy = autobudgetRoiStrategy(null, null, min.subtract(BigDecimal.ONE), 30L, null,
                CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.ROI_COEF),
                greaterThan(min))));
    }

    @Test
    public void reserveReturn_NotNull() {
        DbStrategy dbStrategy = autobudgetRoiStrategy(null, null, new BigDecimal("5"), null, null,
                CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.RESERVE_RETURN), notNull())));
    }

    @Test
    public void reserveReturn_Correct() {
        DbStrategy dbStrategy = autobudgetRoiStrategy(null, null, new BigDecimal("5"), 22L, null,
                CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.RESERVE_RETURN),
                incorrectReserveReturn())));
    }

    @Test
    public void profitability_LessThanMin() {
        BigDecimal min = PROFITABILITY_MIN;
        DbStrategy dbStrategy = autobudgetRoiStrategy(null, null, new BigDecimal("5"), 20L,
                min.subtract(BigDecimal.ONE),
                CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.PROFITABILITY),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void profitability_GreaterThanMax() {
        BigDecimal max = PROFITABILITY_MAX;
        DbStrategy dbStrategy = autobudgetRoiStrategy(null, null, new BigDecimal("5"), 50L, max.add(BigDecimal.ONE),
                CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.PROFITABILITY),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void crr_NotNull() {
        var dbStrategy = autobudgetCrrStrategy(new BigDecimal("10000"), null, CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.CRR), notNull())));
    }

    @Test
    public void crr_LessThanMin() {
        var min = CRR_MIN;
        var dbStrategy = autobudgetCrrStrategy(new BigDecimal("10000"), min - 1, CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.CRR),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void crr_GreaterThanMax() {
        var max = CRR_MAX;
        var dbStrategy = autobudgetCrrStrategy(new BigDecimal("10000"), max + 1, CAMPAIGN_COUNTER_GOAL_1);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.CRR),
                lessThanOrEqualTo(max))));
    }

    @Test
    public void limitClicks_NotNull() {

        DbStrategy dbStrategy = autoBudgetWeekBundle(null, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.LIMIT_CLICKS), notNull())));
    }

    @Test
    public void limitClicks_LessThanMin() {
        long min = currency.getMinAutobudgetClicksBundle();
        DbStrategy dbStrategy = autoBudgetWeekBundle(min - 1, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.LIMIT_CLICKS),
                greaterThanOrEqualTo(min))));
    }

    @Test
    public void limitClicks_GreaterThanMax() {
        long max = currency.getMaxAutobudgetClicksBundle();
        DbStrategy dbStrategy = autoBudgetWeekBundle(max + 1, null, null);
        var vr = validate(dbStrategy);
        assertThat(vr, hasDefectDefinitionWith(validationError(getFieldPath(StrategyData.LIMIT_CLICKS),
                lessThanOrEqualTo(max))));
    }

    private ValidationResult<CampaignWithCustomStrategy, Defect> validate(DbStrategy dbStrategy) {
        return validate(dbStrategy, availableFeatures,
                getCampaignBannersSupplier, getBannersSiteLinkSetsFunction);
    }

    private ValidationResult<CampaignWithCustomStrategy, Defect> validate(
            DbStrategy dbStrategy,
            Set<String> availableFeatures,
            Supplier<List<BannerWithSystemFields>> campaignBannersSupplier,
            Function<List<BannerWithSystemFields>, List<SitelinkSet>> getBannersSiteLinkSetsFunction) {
        campaign.withStrategy(dbStrategy);
        return new CampaignWithCustomStrategyValidator(currency,
                CAMPAIGN_COUNTERS_AVAILABLE_GOALS,
                campaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction, campaign,
                Set.of(StrategyName.values()),
                Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                StrategyValidatorConstantsBuilder.build(campaignType, currency), availableFeatures,
                CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L)), null)
                .apply(campaign);
    }

    private Path getFieldPath(ModelProperty<StrategyData, ?> strategyDataProperty) {
        return path(field(TextCampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY_DATA),
                field(strategyDataProperty));
    }
}
