package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Iterables;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.data.TestGdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAgeType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdGenderType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMcBannerCampaign;
import ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter;
import ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.CAMP_FINISHED_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.MODERATE_RESULT_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_METRICA_CONTROL_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_ORDER_MONEY_IN_SMS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.ADVANCED_GEOTARGETING;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toTimeTarget;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на обновление ГО/MCBANNER кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignMutationUpdateGraphqlServiceMcBannerCampaignTest {
    private static final int COUNTER_ID = 555;
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
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_CAMPAIGNS, MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private int shard;
    private Long campaignId;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        ClientInfo clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();

        TestAuthHelper.setDirectAuthentication(operator);

        campaignId = steps.typedCampaignSteps()
                .createDefaultMcBannerCampaign(clientInfo.getChiefUserInfo(), clientInfo).getId();
    }

    /**
     * Проверка обновления ГО/McBanner кампании
     */
    @Test
    public void updateCampaign() {
        List<GdMeaningfulGoalRequest> gdMeaningfulGoalRequests = List.of(new GdMeaningfulGoalRequest()
                .withGoalId(ENGAGED_SESSION_GOAL_ID)
                .withConversionValue(BigDecimal.ONE));

        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierDemographics(
                new GdUpdateBidModifierDemographics()
                        .withType(GdBidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withEnabled(true)
                        .withCampaignId(campaignId)
                        .withAdjustments(List.of(new GdUpdateBidModifierDemographicsAdjustmentItem()
                                .withPercent(55)
                                .withAge(GdAgeType._0_17)
                                .withGender(GdGenderType.FEMALE))));

        GdUpdateMcBannerCampaign mcBannerCampaign = TestGdUpdateCampaigns.defaultMcBannerCampaign(campaignId)
                .withMetrikaCounters(List.of(COUNTER_ID))
                .withMeaningfulGoals(gdMeaningfulGoalRequests)
                .withBidModifiers(bidModifiers)
                .withDayBudget(BigDecimal.valueOf(500L));

        GdUpdateCampaignPayload payload = sendRequest(mcBannerCampaign);
        checkState(payload.getValidationResult() == null && payload.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        McBannerCampaign expectedCampaign = new McBannerCampaign()
                .withId(campaignId)
                .withMetrikaCounters(List.of((long) COUNTER_ID))
                .withName(mcBannerCampaign.getName())
                .withStartDate(LocalDate.now().plusDays(1))
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withEmail(mcBannerCampaign.getNotification().getEmailSettings().getEmail())
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withEnableOfflineStatNotice(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withSmsFlags(EnumSet.of(MODERATE_RESULT_SMS, NOTIFY_ORDER_MONEY_IN_SMS,
                        NOTIFY_METRICA_CONTROL_SMS, CAMP_FINISHED_SMS))
                .withStrategy((DbStrategy) new DbStrategy()
                        .withPlatform(CampaignsPlatform.SEARCH)
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withStrategyData(new StrategyData()
                                .withName("default")
                                .withSum(BigDecimal.valueOf(5000))
                                .withVersion(1L)
                                .withUnknownFields(emptyMap())))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(mcBannerCampaign.getDayBudget())
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                .withBrandSafetyCategories(emptyList())
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDisabledDomains(mcBannerCampaign.getDisabledPlaces())
                .withDisabledIps(mcBannerCampaign.getDisabledIps())
                .withAttributionModel(campaignConstantsService.getDefaultAttributionModel())
                .withMeaningfulGoals(CommonCampaignConverter.toMeaningfulGoals(gdMeaningfulGoalRequests))
                .withBidModifiers(BidModifierDataConverter.toBidModifiers(bidModifiers));

        McBannerCampaign actualCampaign = checkAndGetActualCampaign(payload);

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath("meaningfulGoals", "0", "conversionValue")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("dayBudget")).useDiffer(new BigDecimalDiffer());

        assertThat(actualCampaign).as("mcbanner campaign")
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(compareStrategy)));
    }

    /**
     * Проверка обновления кампании без счетчиков метрики
     */
    @Test
    public void updateCampaign_WithoutMetrikaCounter() {
        GdUpdateMcBannerCampaign mcBannerCampaign = TestGdUpdateCampaigns.defaultMcBannerCampaign(campaignId)
                .withMetrikaCounters(null);

        GdUpdateCampaignPayload payload = sendRequest(mcBannerCampaign);
        checkState(payload.getValidationResult() == null && payload.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        McBannerCampaign actualCampaign = checkAndGetActualCampaign(payload);
        assertThat(actualCampaign.getMetrikaCounters()).isNull();
    }

    /**
     * Проверка обновления кампании со сратегией AUTOBUDGET_AVG_CPC_PER_CAMP
     */
    @Test
    public void updateCampaign_WithCpcPerCampStrategy() {
        BigDecimal avgBid = BigDecimal.valueOf(100.5);
        BigDecimal bid = BigDecimal.valueOf(101.1);
        BigDecimal sum = BigDecimal.valueOf(5000);

        GdUpdateMcBannerCampaign mcBannerCampaign = TestGdUpdateCampaigns.defaultMcBannerCampaign(campaignId)
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategy(GdCampaignStrategy.AUTOBUDGET_AVG_CPC_PER_CAMP)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgBid(avgBid)
                                .withBid(bid)
                                .withSum(sum)));

        GdUpdateCampaignPayload payload = sendRequest(mcBannerCampaign);
        checkState(payload.getValidationResult() == null && payload.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        DbStrategy expectedStrategy = (DbStrategy) TestCampaigns.defaultAverageCpcPerCamprStrategy(avgBid, bid, sum)
                .withPlatform(CampaignsPlatform.SEARCH);

        McBannerCampaign actualCampaign = checkAndGetActualCampaign(payload);

        assertThat(actualCampaign.getStrategy()).is(matchedBy(
                beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())));
    }

    public static Object[] parametersForCheckAdvancedGeoTargeting() {
        return new Object[][]{
                {false, null, null, true},
                {true, true, true, false},
                {true, true, false, true},
                {true, true, true, true},
                {true, false, true, false},
                {true, true, null, true},
                {true, null, true, true},
        };
    }

    @Test
    @Parameters(method = "parametersForCheckAdvancedGeoTargeting")
    @TestCaseName("feature enabled: {0}, currentRegion: {1}, regularRegion: {2}, hasExtendedGeoTargeting: {3}")
    public void updateMcBannerCampaign_CheckAdvancedGeoTargeting(
            boolean featureEnabled,
            Boolean useCurrentRegion,
            Boolean useRegularRegion,
            Boolean hasExtendedGeoTargeting) {
        steps.featureSteps().addClientFeature(operator.getClientId(), ADVANCED_GEOTARGETING, featureEnabled);

        var campaign = TestGdUpdateCampaigns.defaultMcBannerCampaign(campaignId)
                .withUseCurrentRegion(useCurrentRegion)
                .withUseRegularRegion(useRegularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeoTargeting);

        GdUpdateCampaignPayload response = sendRequest(campaign);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).hasSize(1);
            soft.assertThat(response.getValidationResult()).isNull();
        });

        McBannerCampaign actualCampaign = checkAndGetActualCampaign(response);

        var expect = new McBannerCampaign()
                .withUseCurrentRegion(useCurrentRegion != null && useCurrentRegion)
                .withUseRegularRegion(useRegularRegion != null && useRegularRegion)
                .withHasExtendedGeoTargeting(!featureEnabled
                        || (hasExtendedGeoTargeting != null && hasExtendedGeoTargeting));


            assertThat(actualCampaign)
                    .as("mcbanner campaign")
                    .is(matchedBy(beanDiffer(expect).useCompareStrategy(onlyExpectedFields())));
    }

    private McBannerCampaign checkAndGetActualCampaign(GdUpdateCampaignPayload payload) {
        checkState(payload.getValidationResult() == null && payload.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<Long> campaignIds = mapList(payload.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId);
        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard, campaignIds);
        McBannerCampaign actualCampaign = (McBannerCampaign) Iterables.getFirst(campaigns, null);
        checkNotNull(actualCampaign, "campaign not found");
        return actualCampaign;
    }

    private GdUpdateCampaignPayload sendRequest(GdUpdateMcBannerCampaign campaign) {
        GdUpdateCampaignUnion gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withMcBannerCampaign(campaign);
        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(List.of(gdUpdateCampaignUnion));
        return processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);
    }
}
