package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.McBannerCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsStrategy;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType;
import static ru.yandex.direct.grid.model.campaign.strategy.GdCampaignFlatStrategy.PLATFORM;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy.STRATEGY_NAME;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsStrategy.BIDDING_STRATEGY;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_STRATEGY;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@GridProcessingTest
@RunWith(Parameterized.class)
public class CampaignMutationGraphQlServiceUpdateStrategyForMcBannerCampaignTest {

    private static final BigDecimal SUM = Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudget();
    private static final BigDecimal BID = Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudgetBid();
    private static final BigDecimal AVG_BID = BigDecimal.valueOf(300.5);
    private static final BigDecimal DAY_BUDGET = BigDecimal.valueOf(305.1);

    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_STRATEGY;
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsStrategy, GdUpdateCampaignPayload> UPDATE_CAMPAIGNS_STRATEGY_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsStrategy.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor testExecutor;
    @Autowired
    public Steps steps;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private User operator;
    private ClientInfo clientInfo;
    private UserInfo userInfo;
    private McBannerCampaignInfo campaignInfo;

    @Before
    public void before() throws Exception {
        new TestContextManager(this.getClass()).prepareTestInstance(this);

        clientInfo = steps.clientSteps().createDefaultClient();
        userInfo = clientInfo.getChiefUserInfo();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        campaignInfo = steps.mcBannerCampaignSteps().createDefaultCampaign(clientInfo);
    }

    private static GdUpdateCampaignsStrategy createRequest(GdCampaignBiddingStrategy biddingStrategy,
                                                           GdCampaignAttributionModel attributionModel,
                                                           Integer contextLimit,
                                                           boolean enableCpcPhold,
                                                           BigDecimal dayBudget,
                                                           GdDayBudgetShowMode dayBudgetShowMode) {
        return new GdUpdateCampaignsStrategy()
                .withType(GdCampaignType.MCBANNER)
                .withBiddingStrategy(biddingStrategy)
                .withAttributionModel(attributionModel)
                .withContextLimit(contextLimit)
                .withEnableCpcHold(enableCpcPhold)
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(dayBudgetShowMode);

    }

    private static StrategyData strategyData(StrategyName strategyName, BigDecimal avgBid, BigDecimal bid, BigDecimal sum) {
        return new StrategyData()
                .withName(StrategyName.toSource(strategyName).getLiteral())
                .withAvgBid(avgBid)
                .withBid(bid)
                .withSum(sum)
                .withVersion(1L);
    }

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public GdUpdateCampaignsStrategy input;

    @Parameterized.Parameter(2)
    public BigDecimal expectedDayBudget;

    @Parameterized.Parameter(3)
    public DayBudgetShowMode expectedDayBudgetShowMode;

    @Parameterized.Parameter(4)
    public CampaignsPlatform expectedPlatform;

    @Parameterized.Parameter(5)
    public CampaignsAutobudget expectedAutobudget;

    @Parameterized.Parameter(6)
    public StrategyName expectedStrategyName;

    @Parameterized.Parameter(7)
    public StrategyData expectedStrategyData;

    @Parameterized.Parameter(8)
    public CampaignAttributionModel expectedAttributionModel;

    @Parameterized.Parameter(9)
    public Matcher<GdValidationResult> validationResultMatcher;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                {
                        "default стратегия",
                        createRequest(new GdCampaignBiddingStrategy()
                                        .withPlatform(GdCampaignPlatform.SEARCH)
                                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                                        .withStrategyData(new GdCampaignStrategyData()),
                                GdCampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK,
                                100,
                                true,
                                ZERO,
                                GdDayBudgetShowMode.DEFAULT_),
                        ZERO,
                        DayBudgetShowMode.DEFAULT_,
                        CampaignsPlatform.SEARCH,
                        CampaignsAutobudget.NO,
                        StrategyName.DEFAULT_,
                        strategyData(StrategyName.DEFAULT_, null, null, null),
                        LAST_YANDEX_DIRECT_CLICK,
                        null
                },
                {
                        "default стратегия с ненулевым дневным бюджетом",
                        createRequest(new GdCampaignBiddingStrategy()
                                        .withPlatform(GdCampaignPlatform.SEARCH)
                                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                                        .withStrategyData(new GdCampaignStrategyData()),
                                GdCampaignAttributionModel.LAST_CLICK,
                                10,
                                false,
                                DAY_BUDGET,
                                GdDayBudgetShowMode.STRETCHED),
                        DAY_BUDGET,
                        DayBudgetShowMode.STRETCHED,
                        CampaignsPlatform.SEARCH,
                        CampaignsAutobudget.NO,
                        StrategyName.DEFAULT_,
                        strategyData(StrategyName.DEFAULT_, null, null, null),
                        LAST_YANDEX_DIRECT_CLICK,
                        null
                },
                {
                        "default стратегия с слишком большим дневным бюджетом",
                        createRequest(new GdCampaignBiddingStrategy()
                                        .withPlatform(GdCampaignPlatform.SEARCH)
                                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                                        .withStrategyData(new GdCampaignStrategyData()),
                                GdCampaignAttributionModel.LAST_CLICK,
                                10,
                                false,
                                DAY_BUDGET.multiply(BigDecimal.TEN.pow(5)),
                                GdDayBudgetShowMode.DEFAULT_),
                        DAY_BUDGET.multiply(BigDecimal.TEN.pow(5)),
                        DayBudgetShowMode.DEFAULT_,
                        CampaignsPlatform.SEARCH,
                        CampaignsAutobudget.NO,
                        StrategyName.DEFAULT_,
                        strategyData(StrategyName.DEFAULT_, null, null, null),
                        LAST_YANDEX_DIRECT_CLICK,
                        null
                },
                {
                        "default стратегия с платформой context",
                        createRequest(new GdCampaignBiddingStrategy()
                                        .withPlatform(GdCampaignPlatform.CONTEXT)
                                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                                        .withStrategyData(new GdCampaignStrategyData()),
                                GdCampaignAttributionModel.LAST_CLICK,
                                10,
                                false,
                                DAY_BUDGET,
                                GdDayBudgetShowMode.DEFAULT_),
                        DAY_BUDGET,
                        DayBudgetShowMode.DEFAULT_,
                        CampaignsPlatform.SEARCH,
                        CampaignsAutobudget.NO,
                        StrategyName.DEFAULT_,
                        strategyData(StrategyName.DEFAULT_, null, null, null),
                        LAST_YANDEX_DIRECT_CLICK,
                        hasErrorsWith(gridDefect(
                                path(field(CAMPAIGN_UPDATE_ITEMS), index(0), field(BIDDING_STRATEGY), field(PLATFORM)),
                                inconsistentStrategyToCampaignType()))
                },
                {
                        "autobudget_avg_cpc_per_camp стратегия",
                        createRequest(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withAvgBid(AVG_BID)
                                        .withBid(BID)
                                        .withSum(SUM)
                                ),
                                GdCampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK,
                                100,
                                true,
                                ZERO,
                                GdDayBudgetShowMode.DEFAULT_),
                        ZERO,
                        DayBudgetShowMode.DEFAULT_,
                        CampaignsPlatform.SEARCH,
                        CampaignsAutobudget.YES,
                        StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP,
                        strategyData(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP, AVG_BID, BID, SUM),
                        LAST_YANDEX_DIRECT_CLICK,
                        null
                },
                {
                        "autobudget_avg_cpa_per_camp стратегия (невалидная для mcbanner)",
                        createRequest(new GdCampaignBiddingStrategy()
                                        .withPlatform(GdCampaignPlatform.SEARCH)
                                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                                        .withStrategyData(new GdCampaignStrategyData()
                                                .withAvgBid(AVG_BID)
                                                .withBid(BID)
                                                .withSum(SUM)
                                        ),
                                GdCampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK,
                                100,
                                true,
                                ZERO,
                                GdDayBudgetShowMode.DEFAULT_),
                        ZERO,
                        DayBudgetShowMode.DEFAULT_,
                        CampaignsPlatform.SEARCH,
                        CampaignsAutobudget.YES,
                        StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP,
                        strategyData(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP, AVG_BID, BID, SUM),
                        LAST_YANDEX_DIRECT_CLICK,
                        hasErrorsWith(gridDefect(
                                path(field(CAMPAIGN_UPDATE_ITEMS), index(0), field(BIDDING_STRATEGY), field(STRATEGY_NAME)),
                                inconsistentStrategyToCampaignType()))
                },
        };
    }

    @Test
    public void testOneCampaign() {
        input.withCampaignIds(List.of(campaignInfo.getId()));

        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));
        var actualCampaign = (McBannerCampaign) campaignTypedRepository
                .getTypedCampaigns(campaignInfo.getShard(), Set.of(campaignInfo.getId())).get(0);

        if (validationResultMatcher != null) {
            assertThat(payload.getValidationResult()).is(matchedBy(validationResultMatcher));
        } else {
            assertActualCampaign(actualCampaign, payload, expectedPayload);
        }
    }

    private void assertActualCampaign(McBannerCampaign actualCampaign,
                                      GdUpdateCampaignPayload payload,
                                      GdUpdateCampaignPayload expectedPayload) {
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
            soft.assertThat(actualCampaign.getDayBudget()).as("дневной бюджет")
                    .isEqualByComparingTo(expectedDayBudget);
            soft.assertThat(actualCampaign.getDayBudgetShowMode()).as("режим показа объявлений")
                    .isEqualTo(expectedDayBudgetShowMode);
            soft.assertThat(actualCampaign.getStrategy().getPlatform()).as("платформа")
                    .isEqualTo(expectedPlatform);
            soft.assertThat(actualCampaign.getStrategy().getAutobudget()).as("автобюджет")
                    .isEqualTo(expectedAutobudget);
            soft.assertThat(actualCampaign.getStrategy().getStrategyName()).as("стратегия")
                    .isEqualTo(expectedStrategyName);
            soft.assertThat(actualCampaign.getStrategy().getStrategyData()).as("данные стратегии")
                    .is(matchedBy(beanDiffer(expectedStrategyData)));
            soft.assertThat(actualCampaign.getAttributionModel()).as("модель атрибуции")
                    .isEqualTo(expectedAttributionModel);
        });
    }
}
