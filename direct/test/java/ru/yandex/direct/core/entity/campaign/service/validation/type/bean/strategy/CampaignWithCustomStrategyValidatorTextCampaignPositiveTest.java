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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.CAMPAIGN_COUNTERS_AVAILABLE_GOALS;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.CAMPAIGN_COUNTER_GOAL_1;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.CAMPAIGN_COUNTER_GOAL_2;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignStrategyTestDataUtils.TURBOLANDING_INTERNAL_COUNTER_GOAL_2;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autoBudgetWeekBundle;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaPayForConversionStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetCrrStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

/**
 * Тесты на то, что валидация проходит для валидных данных стратегий
 */
@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorTextCampaignPositiveTest {

    private static final Long MEANINGFUL_GOALS_GOAL_ID = 13L;
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

    private Supplier<List<BannerWithSystemFields>> getCampaignBannersSupplier = Collections::emptyList;
    private Supplier<List<AdGroupSimple>> campaignAdGroupsSupplier = Collections::emptyList;
    private Function<List<BannerWithSystemFields>, List<SitelinkSet>> getBannersSiteLinkSetsFunction =
            banners -> Collections.emptyList();

    private Currency currency;
    private TextCampaign textCampaign;
    private Set<String> availableFeatures = ImmutableSet.of(CRR_STRATEGY_ALLOWED.getName());

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        textCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return Arrays.asList(new Object[][]{
                {"default", defaultStrategy()},
                {"autobudget", defaultAutobudgetStrategy()},
                {"autobudget", defaultAutobudgetStrategy(MEANINGFUL_GOALS_GOAL_ID)},
                {"autobudgetWithAllValues", autobudgetStrategy(new BigDecimal("1000"), new BigDecimal("100"),
                        CAMPAIGN_COUNTER_GOAL_2)},
                {"autobudgetWeekBundle", autoBudgetWeekBundle(1000L, null, null)},
                {"autobudgetWeekBundleWithBid", autoBudgetWeekBundle(1000L, new BigDecimal("200"), null)},
                {"autobudgetWeekBundleWithAvgBid", autoBudgetWeekBundle(1000L, null, new BigDecimal("1234"))},
                {"autobudgetAvgClick", averageClickStrategy(new BigDecimal("123"), null)},
                {"autobudgetAvgClickWithSum", averageClickStrategy(new BigDecimal("123"), new BigDecimal("2000"))},
                {"autobudgetAvgCpa", defaultAverageCpaStrategy(CAMPAIGN_COUNTER_GOAL_2)},
                {"autobudgetAvgCpaWithZeroGoalId", defaultAverageCpaStrategy(0L)},
                {"autobudgetAvgCpaWithPayForConversion", averageCpaPayForConversionStrategy(new BigDecimal("500"),
                        TURBOLANDING_INTERNAL_COUNTER_GOAL_2,
                        new BigDecimal(500).multiply(BigDecimal.valueOf(PAY_FOR_CONVERSION_SUM_TO_AVG_CPA_MIN_RATIO)),
                        null)},
                {"autobudgetAvgCpaWithSumAndBid",
                        averageCpaStrategy(new BigDecimal("2000"), CAMPAIGN_COUNTER_GOAL_2, new BigDecimal("10000"),
                                new BigDecimal("100"))},
                {"autobudgetRoi", defaultAutobudgetRoiStrategy(CAMPAIGN_COUNTER_GOAL_2)},
                {"autobudgetCrr", defaultAutobudgetCrrStrategy(CAMPAIGN_COUNTER_GOAL_2)},
                {"autobudgetRoiWithAllValues", autobudgetRoiStrategy(new BigDecimal("10000"), new BigDecimal("110"),
                        new BigDecimal("12"), 20L, new BigDecimal("30"),
                        CAMPAIGN_COUNTER_GOAL_2)},
        });
    }

    private static List<MeaningfulGoal> getTestMeaningfulGoals() {
        return List.of(new MeaningfulGoal()
                .withGoalId(CAMPAIGN_COUNTER_GOAL_1)
                .withConversionValue(BigDecimal.TEN));
    }

    @Test
    public void checkValidatedSuccessfully() {
        textCampaign
                .withStrategy(dbStrategy)
                .withMeaningfulGoals(getTestMeaningfulGoals());
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = new CampaignWithCustomStrategyValidator(currency,
                CAMPAIGN_COUNTERS_AVAILABLE_GOALS,
                getCampaignBannersSupplier, campaignAdGroupsSupplier,
                getBannersSiteLinkSetsFunction, textCampaign, Set.of(StrategyName.values()),
                Set.of(CampOptionsStrategy.values()), Set.of(CampaignsPlatform.values()),
                new CommonStrategyValidatorConstants(currency), availableFeatures,
                CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L)), null)
                .apply(textCampaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
