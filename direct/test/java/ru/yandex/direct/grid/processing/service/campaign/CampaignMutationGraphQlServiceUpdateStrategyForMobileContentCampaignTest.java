package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPI;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.feature.FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_STRATEGY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceUpdateStrategyForMobileContentCampaignTest {
    private static final BigDecimal SUM = Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudget();
    private static final BigDecimal BID = Currencies.getCurrency(CurrencyCode.RUB).getMaxAutobudgetBid();
    private static final BigDecimal AVG_CPI = BigDecimal.valueOf(100.5);
    private static final Long GOAL_ID = 38403071L;
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
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";

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
    private long mobileAppId;
    private TypedCampaignInfo campaignInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        userInfo = clientInfo.getChiefUserInfo();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), IN_APP_EVENTS_IN_RMP_ENABLED, true);

        campaignInfo = steps.typedCampaignSteps().createMobileContentCampaign(userInfo, clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withMobileAppId(mobileAppId));
    }

    @Test
    public void updateStrategy() {
        var input = createRequest(List.of(campaignInfo), BID, null);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));
        var actualCampaign = (MobileContentCampaign) campaignTypedRepository
                .getTypedCampaigns(campaignInfo.getShard(), Set.of(campaignInfo.getId())).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
            soft.assertThat(actualCampaign.getDayBudget()).as("дневной бюджет")
                    .isEqualByComparingTo(ZERO);
            soft.assertThat(actualCampaign.getDayBudgetShowMode()).as("режим показа объявлений")
                    .isEqualTo(DayBudgetShowMode.DEFAULT_);
            soft.assertThat(actualCampaign.getStrategy().getPlatform()).as("платформа")
                    .isEqualTo(CampaignsPlatform.SEARCH);
            soft.assertThat(actualCampaign.getStrategy().getAutobudget()).as("автобюджет")
                    .isEqualTo(CampaignsAutobudget.YES);
            soft.assertThat(actualCampaign.getStrategy().getStrategyName()).as("стратегия")
                    .isEqualTo(AUTOBUDGET_AVG_CPI);
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getAvgCpi()).as("средняя цена клика")
                    .isEqualByComparingTo(AVG_CPI);
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getSum()).as("недельный бюджет")
                    .isEqualByComparingTo(SUM);
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getBid()).as("максимальная цена")
                    .isEqualByComparingTo(BID);
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getGoalId()).as("цель")
                    .isEqualByComparingTo(GOAL_ID);
            // Для РМП модель атрибуции всегда LAST_YANDEX_DIRECT_CLICK
            soft.assertThat(actualCampaign.getAttributionModel()).as("модель атрибуции")
                    .isEqualTo(LAST_YANDEX_DIRECT_CLICK);
        });
    }

    @Test
    public void updateStrategy_TwoCampaigns() {
        TypedCampaignInfo anotherCampaignInfo = steps.typedCampaignSteps().createMobileContentCampaign(userInfo,
                clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withMobileAppId(mobileAppId));

        var input = createRequest(List.of(campaignInfo, anotherCampaignInfo), BID, null);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getId());
        var anotherExpectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(anotherCampaignInfo.getId());
        var actualCampaigns = StreamEx.of(campaignTypedRepository
                .getTypedCampaigns(clientInfo.getShard(), Set.of(campaignInfo.getId(), anotherCampaignInfo.getId())))
                .select(MobileContentCampaign.class).toList();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload.getUpdatedCampaigns())
                    .is(matchedBy(containsInAnyOrder(expectedPayloadItem, anotherExpectedPayloadItem)));
            for (MobileContentCampaign actualCampaign : actualCampaigns) {
                soft.assertThat(actualCampaign.getDayBudget()).as("дневной бюджет")
                        .isEqualByComparingTo(ZERO);
                soft.assertThat(actualCampaign.getDayBudgetShowMode()).as("режим показа объявлений")
                        .isEqualTo(DayBudgetShowMode.DEFAULT_);
                soft.assertThat(actualCampaign.getStrategy().getPlatform()).as("платформа")
                        .isEqualTo(CampaignsPlatform.SEARCH);
                soft.assertThat(actualCampaign.getStrategy().getAutobudget()).as("автобюджет")
                        .isEqualTo(CampaignsAutobudget.YES);
                soft.assertThat(actualCampaign.getStrategy().getStrategyName()).as("стратегия")
                        .isEqualTo(AUTOBUDGET_AVG_CPI);
                soft.assertThat(actualCampaign.getStrategy().getStrategyData().getAvgCpi()).as("средняя цена клика")
                        .isEqualByComparingTo(AVG_CPI);
                soft.assertThat(actualCampaign.getStrategy().getStrategyData().getSum()).as("недельный бюджет")
                        .isEqualByComparingTo(SUM);
                soft.assertThat(actualCampaign.getStrategy().getStrategyData().getBid()).as("максимальная цена")
                        .isEqualByComparingTo(BID);
                soft.assertThat(actualCampaign.getStrategy().getStrategyData().getGoalId()).as("цель")
                        .isEqualByComparingTo(GOAL_ID);
                // Для РМП модель атрибуции всегда LAST_YANDEX_DIRECT_CLICK
                soft.assertThat(actualCampaign.getAttributionModel()).as("модель атрибуции")
                        .isEqualTo(LAST_YANDEX_DIRECT_CLICK);
            }
        });
    }

    @Test
    public void updateStrategy_WithPayForConversions() {
        var input = createRequest(List.of(campaignInfo), null, true);

        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(campaignInfo.getId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));
        var actualCampaign = (MobileContentCampaign) campaignTypedRepository
                .getTypedCampaigns(campaignInfo.getShard(), Set.of(campaignInfo.getId())).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
            soft.assertThat(actualCampaign.getStrategy().getStrategyData().getPayForConversion())
                    .as("оплата за конверсии")
                    .isTrue();
        });
    }

    @Test
    public void updateStrategy_BidHigherThanMax_validationError() {
        var input = createRequest(List.of(campaignInfo), BID.add(BigDecimal.TEN), null);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator);

        assertThat(payload.getValidationResult().getErrors()).hasSize(1);
    }

    private static GdUpdateCampaignsStrategy createRequest(List<TypedCampaignInfo> infos,
                                                           BigDecimal bid,
                                                           Boolean withPayForConversion) {
        return new GdUpdateCampaignsStrategy()
                .withType(GdCampaignType.MOBILE_CONTENT)
                .withCampaignIds(mapList(infos, TypedCampaignInfo::getId))
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPI)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgCpi(AVG_CPI)
                                .withGoalId(GOAL_ID)
                                .withBid(bid)
                                .withSum(SUM)
                                .withPayForConversion(withPayForConversion)
                        )
                )
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withContextLimit(100)
                .withEnableCpcHold(false)
                .withDayBudget(ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_);
    }
}
