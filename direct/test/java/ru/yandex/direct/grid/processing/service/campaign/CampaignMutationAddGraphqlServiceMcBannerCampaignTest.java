package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Iterables;
import junitparams.JUnitParamsRunner;
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
import ru.yandex.direct.grid.processing.data.TestGdAddCampaigns;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAgeType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdGenderType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddMcBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
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
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.CAMP_FINISHED_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.MODERATE_RESULT_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_METRICA_CONTROL_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_ORDER_MONEY_IN_SMS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toTimeTarget;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.ADD_CAMPAIGNS;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на добавление ГО/MCBANNER кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignMutationAddGraphqlServiceMcBannerCampaignTest {
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
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_CAMPAIGNS, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

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

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        ClientInfo clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();

        TestAuthHelper.setDirectAuthentication(operator);
    }

    /**
     * Проверка добавления ГО/McBanner кампании
     */
    @Test
    public void addCampaign() {
        List<GdMeaningfulGoalRequest> gdMeaningfulGoalRequests = List.of(new GdMeaningfulGoalRequest()
                .withGoalId(ENGAGED_SESSION_GOAL_ID)
                .withConversionValue(BigDecimal.ONE));

        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierDemographics(
                new GdUpdateBidModifierDemographics()
                        .withType(GdBidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withEnabled(true)
                        .withCampaignId(0L)
                        .withAdjustments(List.of(new GdUpdateBidModifierDemographicsAdjustmentItem()
                                .withPercent(55)
                                .withAge(GdAgeType._0_17)
                                .withGender(GdGenderType.FEMALE))));

        GdAddMcBannerCampaign mcBannerCampaign = TestGdAddCampaigns.defaultMcBannerCampaign()
                .withMetrikaCounters(List.of(COUNTER_ID))
                .withMeaningfulGoals(gdMeaningfulGoalRequests)
                .withBidModifiers(bidModifiers)
                .withDayBudget(BigDecimal.valueOf(500L));

        GdAddCampaignPayload payload = sendRequest(mcBannerCampaign);
        checkState(payload.getValidationResult() == null && payload.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        bidModifiers.getBidModifierDemographics().setCampaignId(payload.getAddedCampaigns().get(0).getId());

        McBannerCampaign expectedCampaign = new McBannerCampaign()
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

    @Test
    public void addCampaign_WithoutMetrikaCounter() {
        GdAddMcBannerCampaign mcBannerCampaign = TestGdAddCampaigns.defaultMcBannerCampaign()
                .withMetrikaCounters(emptyList());

        GdAddCampaignPayload payload = sendRequest(mcBannerCampaign);
        checkState(payload.getValidationResult() == null && payload.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        McBannerCampaign actualCampaign = checkAndGetActualCampaign(payload);
        assertThat(actualCampaign.getMetrikaCounters()).isNull();
    }

    @Test
    public void addCampaign_WithCpcPerCampStrategy() {
        BigDecimal avgBid = BigDecimal.valueOf(100.5);
        BigDecimal bid = BigDecimal.valueOf(101.1);
        BigDecimal sum = BigDecimal.valueOf(5000);

        GdAddMcBannerCampaign mcBannerCampaign = TestGdAddCampaigns.defaultMcBannerCampaign()
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategy(GdCampaignStrategy.AUTOBUDGET_AVG_CPC_PER_CAMP)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgBid(avgBid)
                                .withBid(bid)
                                .withSum(sum)));

        GdAddCampaignPayload payload = sendRequest(mcBannerCampaign);
        checkState(payload.getValidationResult() == null && payload.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        DbStrategy expectedStrategy = (DbStrategy) TestCampaigns.defaultAverageCpcPerCamprStrategy(avgBid, bid, sum)
                .withPlatform(CampaignsPlatform.SEARCH);

        McBannerCampaign actualCampaign = checkAndGetActualCampaign(payload);

        assertThat(actualCampaign.getStrategy()).is(matchedBy(
                beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())));
    }

    private McBannerCampaign checkAndGetActualCampaign(GdAddCampaignPayload payload) {
        checkState(payload.getValidationResult() == null && payload.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<Long> campaignIds = mapList(payload.getAddedCampaigns(), GdAddCampaignPayloadItem::getId);
        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard, campaignIds);
        McBannerCampaign actualCampaign = (McBannerCampaign) Iterables.getFirst(campaigns, null);
        checkNotNull(actualCampaign, "campaign not found");
        return actualCampaign;
    }

    private GdAddCampaignPayload sendRequest(GdAddMcBannerCampaign campaign) {
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withMcBannerCampaign(campaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));
        return processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input, operator);
    }
}
