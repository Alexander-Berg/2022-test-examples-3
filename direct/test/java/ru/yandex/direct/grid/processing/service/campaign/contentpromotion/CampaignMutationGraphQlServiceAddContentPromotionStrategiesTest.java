package ru.yandex.direct.grid.processing.service.campaign.contentpromotion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.google.common.net.InetAddresses;
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
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddContentPromotionCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds.Gen.INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_SSP;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceAddContentPromotionStrategiesTest {

    private static final String MUTATION_NAME = "addCampaigns";
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
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

    private static final String INTERNAL_IP = "12.12.12.12";

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private NetAcl netAcl;

    @Autowired
    CampaignConstantsService campaignConstantsService;

    private User operator;
    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        steps.sspPlatformsSteps().addSspPlatforms(DISABLED_SSP);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);

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
    public void addContentPromotionCampaign_AutobudgetAvgClickStrategy() {
        GdAddContentPromotionCampaign gdAddContentPromotionCampaign = getDefaultGdAddContentPromotionCampaign()
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgBid(BigDecimal.valueOf(50))
                                .withSum(BigDecimal.valueOf(5000))));

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withContentPromotionCampaign(gdAddContentPromotionCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
    }

    @Test
    public void addContentPromotionCampaign_AutobudgetStrategy() {
        GdAddContentPromotionCampaign gdAddContentPromotionCampaign = getDefaultGdAddContentPromotionCampaign()
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withBid(BigDecimal.valueOf(50))
                                .withSum(BigDecimal.valueOf(5000))));

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withContentPromotionCampaign(gdAddContentPromotionCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
    }

    @Test
    public void addContentPromotionCampaign_AutobudgetWeekBundleStrategy() {
        GdAddContentPromotionCampaign gdAddContentPromotionCampaign = getDefaultGdAddContentPromotionCampaign()
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withLimitClicks(100L)
                                .withSum(BigDecimal.valueOf(50000))));

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withContentPromotionCampaign(gdAddContentPromotionCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
    }

    @Test
    public void addContentPromotionCampaign_ContextPlatform_ValidationError() {
        GdAddContentPromotionCampaign gdAddContentPromotionCampaign = getDefaultGdAddContentPromotionCampaign()
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withLimitClicks(100L)
                                .withSum(BigDecimal.valueOf(50000))));

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withContentPromotionCampaign(gdAddContentPromotionCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);
        GdValidationResult vr = gdAddCampaignPayload.getValidationResult();

        assertThat(gdAddCampaignPayload.getAddedCampaigns()).isEqualTo(singletonList(null));
        assertThat(vr).isNotNull();
        assertThat(vr.getErrors().get(0)).isEqualTo(new GdDefect()
                .withCode(INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE.getCode())
                .withPath("campaignAddItems[0].biddingStategy.platform"));
    }

    @Test
    public void addContentPromotionCampaign_BothPlatforms_ValidationError() {
        GdAddContentPromotionCampaign gdAddContentPromotionCampaign = getDefaultGdAddContentPromotionCampaign()
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withLimitClicks(100L)
                                .withSum(BigDecimal.valueOf(50000))));

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withContentPromotionCampaign(gdAddContentPromotionCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);
        GdValidationResult vr = gdAddCampaignPayload.getValidationResult();

        assertThat(gdAddCampaignPayload.getAddedCampaigns()).isEqualTo(singletonList(null));
        assertThat(vr).isNotNull();
        assertThat(vr.getErrors().get(0)).isEqualTo(new GdDefect()
                .withCode(INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE.getCode())
                .withPath("campaignAddItems[0].biddingStategy.platform"));
    }

    @Test
    public void addContentPromotionCampaign_DifferentPlacesStrategy_ValidationError() {
        addContentPromotionCampaign_WithContextStrategy_ValidationError(GdCampaignStrategy.DIFFERENT_PLACES);
    }

    @Test
    public void addContentPromotionCampaign_AutobudgetAvgCpcPerCampStrategy_ValidationError() {
        addContentPromotionCampaign_WithContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPC_PER_CAMP);
    }

    @Test
    public void addContentPromotionCampaign_AutobudgetAvgCpcPerFilterStrategy_ValidationError() {
        addContentPromotionCampaign_WithContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPC_PER_FILTER);
    }

    @Test
    public void addContentPromotionCampaign_AutobudgetAvgCpaPerCampStrategy_ValidationError() {
        addContentPromotionCampaign_WithContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPA_PER_CAMP);
    }

    @Test
    public void addContentPromotionCampaign_AutobudgetAvgCpaPerFilterStrategy_ValidationError() {
        addContentPromotionCampaign_WithContextStrategy_ValidationError(GdCampaignStrategy.AUTOBUDGET_AVG_CPA_PER_FILTER);
    }

    private void addContentPromotionCampaign_WithContextStrategy_ValidationError(GdCampaignStrategy strategy) {
        GdAddContentPromotionCampaign gdAddContentPromotionCampaign = getDefaultGdAddContentPromotionCampaign()
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withStrategy(strategy)
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgBid(BigDecimal.valueOf(50))
                                .withSum(BigDecimal.valueOf(5000))));

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withContentPromotionCampaign(gdAddContentPromotionCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);
        GdValidationResult vr = gdAddCampaignPayload.getValidationResult();

        assertThat(gdAddCampaignPayload.getAddedCampaigns()).isEqualTo(singletonList(null));
        assertThat(vr).isNotNull();
        assertThat(vr.getErrors().get(0)).isEqualTo(new GdDefect()
                .withCode(INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE.getCode())
                .withPath("campaignAddItems[0].biddingStategy.strategy"));
    }

    private static GdAddContentPromotionCampaign getDefaultGdAddContentPromotionCampaign() {
        return new GdAddContentPromotionCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(emptyList())
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
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
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN))))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withBid(BigDecimal.valueOf(50))
                                .withSum(BigDecimal.valueOf(5000))))
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel));
    }
}
