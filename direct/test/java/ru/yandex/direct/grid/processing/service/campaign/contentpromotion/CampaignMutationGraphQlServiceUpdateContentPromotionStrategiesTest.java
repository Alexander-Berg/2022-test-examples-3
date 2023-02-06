package ru.yandex.direct.grid.processing.service.campaign.contentpromotion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.google.common.net.InetAddresses;
import junitparams.converters.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateContentPromotionCampaign;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds.Gen.INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_IPS;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_SSP;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceUpdateContentPromotionStrategiesTest {

    private static final long KALININGRAD_TIMEZONE_ID = 131L;
    private static final String MUTATION_NAME = "updateCampaigns";
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
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    private static final String INTERNAL_IP = "12.12.12.12";

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private NetAcl netAcl;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private CampaignInfo contentPromotionCampaignInfo;
    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        steps.sspPlatformsSteps().addSspPlatforms(DISABLED_SSP);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);

        contentPromotionCampaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        Mockito.when(netAcl.isInternalIp(InetAddresses.forString(INTERNAL_IP))).thenReturn(true);
        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @After
    public void after() {
        reset(netAcl);
    }

    @Test
    public void update_ContentPromotionCampaign_AutobudgetStrategy() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(
                        contentPromotionCampaignInfo.getCampaignId(), newName, tomorrow, dayAfterTomorrow,
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withBiddingStategy(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withBid(BigDecimal.valueOf(50))
                                        .withSum(BigDecimal.valueOf(5000)))));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(contentPromotionCampaignInfo.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();
    }

    @Test
    public void update_ContentPromotionCampaign_AutobudgetAvgClickStrategy() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(
                        contentPromotionCampaignInfo.getCampaignId(), newName, tomorrow, dayAfterTomorrow,
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withBiddingStategy(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withAvgBid(BigDecimal.valueOf(50))
                                        .withSum(BigDecimal.valueOf(5000)))));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(contentPromotionCampaignInfo.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();
    }

    @Test
    public void update_ContentPromotionCampaign_AutobudgetWeekBundleStrategy() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(
                        contentPromotionCampaignInfo.getCampaignId(), newName, tomorrow, dayAfterTomorrow,
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withBiddingStategy(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withLimitClicks(100L)
                                        .withSum(BigDecimal.valueOf(50000)))));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(contentPromotionCampaignInfo.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();
    }

    @Test
    public void update_ContentPromotionCampaign_ContextPlatform_ValidationError() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(
                        contentPromotionCampaignInfo.getCampaignId(), newName, tomorrow, dayAfterTomorrow,
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withBiddingStategy(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withLimitClicks(100L)
                                        .withSum(BigDecimal.valueOf(50000)))));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);
        GdValidationResult vr = gdUpdateCampaignPayload.getValidationResult();

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).isEqualTo(singletonList(null));
        assertThat(vr).isNotNull();
        assertThat(vr.getErrors().get(0)).isEqualTo(new GdDefect()
                .withCode(INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE.getCode())
                .withPath("campaignUpdateItems[0].biddingStategy.platform"));
    }

    @Test
    public void update_ContentPromotionCampaign_BothPlatforms_ValidationError() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(
                        contentPromotionCampaignInfo.getCampaignId(), newName, tomorrow, dayAfterTomorrow,
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withBiddingStategy(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.BOTH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withLimitClicks(100L)
                                        .withSum(BigDecimal.valueOf(50000)))));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);
        GdValidationResult vr = gdUpdateCampaignPayload.getValidationResult();

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).isEqualTo(singletonList(null));
        assertThat(vr).isNotNull();
        assertThat(vr.getErrors().get(0)).isEqualTo(new GdDefect()
                .withCode(INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE.getCode())
                .withPath("campaignUpdateItems[0].biddingStategy.platform"));
    }

    @Test
    public void update_ContentPromotionCampaign_UpdateDifferentPlacesStrategy_ValidationError() {
        update_ContentPromotionCampaign_UpdateContextStrategy_ValidationError(GdCampaignStrategy.DIFFERENT_PLACES);
    }

    @Test
    public void update_ContentPromotionCampaign_UpdateAutobudgetAvgCpcPerCampStrategy_ValidationError() {
        update_ContentPromotionCampaign_UpdateContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPC_PER_CAMP);
    }

    @Test
    public void update_ContentPromotionCampaign_UpdateAutobudgetAvgCpaPerCampStrategy_ValidationError() {
        update_ContentPromotionCampaign_UpdateContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPA_PER_CAMP);
    }

    @Test
    public void update_ContentPromotionCampaign_UpdateAutobudgetAvgCpaPerFilterStrategy_ValidationError() {
        update_ContentPromotionCampaign_UpdateContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPA_PER_FILTER);
    }

    @Test
    public void update_ContentPromotionCampaign_UpdateAutobudgetAvgCpcPerFilterStrategy_ValidationError() {
        update_ContentPromotionCampaign_UpdateContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPC_PER_FILTER);
    }

    private void update_ContentPromotionCampaign_UpdateContextStrategy_ValidationError(GdCampaignStrategy strategy) {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(
                        contentPromotionCampaignInfo.getCampaignId(), newName, tomorrow, dayAfterTomorrow,
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withBiddingStategy(new GdCampaignBiddingStrategy()
                                .withStrategy(strategy)
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withLimitClicks(100L)
                                        .withSum(BigDecimal.valueOf(50000)))));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);
        GdValidationResult vr = gdUpdateCampaignPayload.getValidationResult();

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).isEqualTo(singletonList(null));
        assertThat(vr).isNotNull();
        assertThat(vr.getErrors().get(0)).isEqualTo(new GdDefect()
                .withCode(INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE.getCode())
                .withPath("campaignUpdateItems[0].biddingStategy.strategy"));
    }

    private static GdUpdateContentPromotionCampaign contentPromotionCampaign(Long id, String name, LocalDate startDate,
                                                                             @Nullable LocalDate endDate,
                                                                             List<String> disabledIps,
                                                                             @Nullable GdTimeTarget timeTarget) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);

        return new GdUpdateContentPromotionCampaign()
                .withId(id)
                .withName(name)
                .withEndDate(endDate)
                .withStartDate(startDate)
                .withTimeTarget(timeTarget)
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategy(GdCampaignStrategy.DIFFERENT_PLACES)
                        .withStrategyData(new GdCampaignStrategyData()))
                .withDisabledIps(disabledIps)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withContentLanguage(null)
                .withAttributionModel(null)
                .withHasExtendedGeoTargeting(true)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel));
    }

    private static GdUpdateCampaignUnion getContentPromotionGdUpdateCampaignUnion(GdUpdateContentPromotionCampaign contentPromotionCampaign) {
        return new GdUpdateCampaignUnion()
                .withContentPromotionCampaign(contentPromotionCampaign);
    }
}
