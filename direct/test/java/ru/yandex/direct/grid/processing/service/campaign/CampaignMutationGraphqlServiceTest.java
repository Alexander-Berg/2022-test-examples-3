package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.net.InetAddresses;
import junitparams.converters.Nullable;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.campaign.service.validation.type.TimeIntervalValidator;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DialogInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlacementType;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdBroadMatchRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateContentPromotionCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMcBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMobileContentCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.SEARCH;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.CAMP_FINISHED_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.MODERATE_RESULT_SMS;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_WEEK_BUNDLE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_HAS_TURBO_SMARTS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestCampaigns.DEFAULT_BANNER_HREF_PARAMS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMcBannerCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_DOMAINS;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_IPS;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_SSP;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.PLACEMENT_TYPES;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toTimeTarget;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.data.TestGdUpdateCampaigns.defaultMcBannerCampaign;
import static ru.yandex.direct.grid.processing.data.TestGdUpdateCampaigns.defaultMobileContentCampaign;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toBroadMatch;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toPlacementTypes;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toSmsFlags;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphqlServiceTest {

    private static final long KALININGRAD_TIMEZONE_ID = 131L;
    private static final Long COUNTER_ID = 5L;
    private static final Long MEANINGFUL_GOAL_ID = 1L;

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
    private static final int INVALID_HOUR = TimeIntervalValidator.MIN_HOUR - 1;

    private static final String PRIVATE_IP = "192.168.1.1";
    private static final String INTERNAL_IP = "12.12.12.12";
    private static final String INVALID_IP = "999.999.999.999";
    private static final long VALID_GOAL_ID = 0L;
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";

    private static final GdBroadMatchRequest BROAD_MATCH = new GdBroadMatchRequest()
            .withBroadMatchFlag(false)
            .withBroadMatchLimit(70);

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    ClientDialogsRepository clientDialogsRepository;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private NetAcl netAcl;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;
    private CampaignInfo textCampaignInfoFirst;
    private CampaignInfo textCampaignInfoSecond;
    private CampaignInfo textCampaignInfoThird;
    private CampaignInfo contentPromotionCampaignInfo;
    private Long mobileAppId;

    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        steps.sspPlatformsSteps().addSspPlatforms(DISABLED_SSP);
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.AUTO_BUDGET_MEANINGFUL_GOALS_VALUES_FROM_METRIKA, true);

        textCampaignInfoFirst = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        textCampaignInfoSecond = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        textCampaignInfoThird = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        contentPromotionCampaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);

        DialogInfo dialog = steps.dialogSteps().createStandaloneDefaultDialog(clientInfo);
        clientDialogsRepository.addDialogToCampaign(textCampaignInfoThird.getShard(),
                textCampaignInfoThird.getCampaignId(), dialog.getDialog().getId());

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        Mockito.when(netAcl.isInternalIp(InetAddresses.forString(INTERNAL_IP))).thenReturn(true);
        metrikaClientStub.addUserCounter(clientInfo.getUid(), COUNTER_ID.intValue());
        metrikaClientStub.addCounterGoal(COUNTER_ID.intValue(), MEANINGFUL_GOAL_ID.intValue());

        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @After
    public void after() {
        reset(netAcl);
    }

    @Test
    public void update_ContentPromotionCampaign() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnion =
                getContentPromotionGdUpdateCampaignUnion(contentPromotionCampaign(contentPromotionCampaignInfo.getCampaignId(),
                        newName, tomorrow, dayAfterTomorrow, DISABLED_IPS,
                        defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID)));

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(contentPromotionCampaignInfo.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();

        GdCampaignNotificationRequest notification = campaignUnion.getContentPromotionCampaign().getNotification();
        TimeInterval smsTime = toTimeInterval(notification.getSmsSettings().getSmsTime());
        EnumSet<SmsFlag> smsFlags = toSmsFlags(notification.getSmsSettings().getEnableEvents());
        GdCampaignEmailSettingsRequest emailSettings = notification.getEmailSettings();

        ContentPromotionCampaign expectedCampaign = new ContentPromotionCampaign()
                .withId(contentPromotionCampaignInfo.getCampaignId())
                .withClientId(contentPromotionCampaignInfo.getClientId().asLong())
                .withWalletId(contentPromotionCampaignInfo.getCampaign().getWalletId())
                .withName(newName)
                .withStartDate(tomorrow)
                .withEndDate(dayAfterTomorrow)
                .withType(CampaignType.CONTENT_PROMOTION)
                .withDisabledIps(DISABLED_IPS)
                .withHasExtendedGeoTargeting(true)
                .withTimeZoneId(KALININGRAD_TIMEZONE_ID)
                .withSmsTime(smsTime)
                .withSmsFlags(smsFlags)
                .withEmail(emailSettings.getEmail())
                .withWarningBalance(emailSettings.getWarningBalance())
                .withEnablePausedByDayBudgetEvent(true)
                .withDisabledSsp(emptyList())
                .withStrategy((DbStrategy) defaultStrategy()
                        .withAutobudget(CampaignsAutobudget.YES)
                        .withStrategy(null)
                        .withStrategyName(AUTOBUDGET_WEEK_BUNDLE)
                        .withPlatform(SEARCH)
                        .withStrategyData(new StrategyData()
                                .withLimitClicks(1234L)
                                .withAvgBid(BigDecimal.valueOf(123))))
                .withBrandSafetyCategories(List.of(4_294_967_298L, 4_294_967_297L));

        Map<Long, ContentPromotionCampaign> validCampaigns = listToMap(campaignTypedRepository.getTypedCampaigns(
                        clientInfo.getShard(),
                        singletonList(contentPromotionCampaignInfo.getCampaignId())),
                BaseCampaign::getId, ContentPromotionCampaign.class::cast
        );
        ContentPromotionCampaign actualCampaign = validCampaigns.get(contentPromotionCampaignInfo.getCampaignId());

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));
        assertThat(testCampaignRepository.getCampaignFieldValue(contentPromotionCampaignInfo.getShard(),
                contentPromotionCampaignInfo.getCampaignId(), CAMP_OPTIONS.STATUS_CLICK_TRACK)).isEqualTo(1L);
    }

    @Test
    public void update_EmptyListOfCampaigns() {
        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(emptyList());
        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();
        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).isEmpty();
    }

    @Test
    public void update_TextCampaignWithBannerHrefParams() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);
        Long existingDialogId = steps.dialogSteps().createStandaloneDefaultDialog(clientInfo).getDialog().getId();

        GdUpdateCampaignUnion campaignUnion =
                getTextGdUpdateCampaignUnion(textCampaign(
                                textCampaignInfoFirst.getCampaignId(), newName,
                                tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                                DISABLED_IPS, PLACEMENT_TYPES, BROAD_MATCH, existingDialogId,
                                defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID)
                        ).withBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS)
                );

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(List.of(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = asList(
                new GdUpdateCampaignPayloadItem().withId(textCampaignInfoFirst.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();


        TextCampaign expectedCampaign = new TextCampaign()
                .withId(textCampaignInfoFirst.getCampaignId())
                .withClientId(textCampaignInfoFirst.getClientId().asLong())
                .withWalletId(textCampaignInfoFirst.getCampaign().getWalletId())
                .withName(newName)
                .withStartDate(tomorrow)
                .withEndDate(dayAfterTomorrow)
                .withType(CampaignType.TEXT)
                .withDisabledDomains(DISABLED_DOMAINS)
                .withDisabledSsp(DISABLED_SSP)
                .withDisabledIps(DISABLED_IPS)
                .withPlacementTypes(toPlacementTypes(PLACEMENT_TYPES))
                .withBroadMatch(toBroadMatch(BROAD_MATCH))
                .withHasTitleSubstitution(false)
                .withHasTurboApp(false)
                .withHasExtendedGeoTargeting(true)
                .withTimeZoneId(KALININGRAD_TIMEZONE_ID)
                .withStrategy(defaultStrategy())
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withClientDialogId(existingDialogId)
                .withBrandSafetyCategories(emptyList())
                .withBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS);

        Map<Long, TextCampaign> validCampaigns = listToMap(campaignTypedRepository.getTypedCampaigns(
                        clientInfo.getShard(),
                        List.of(textCampaignInfoFirst.getCampaignId())),
                BaseCampaign::getId, TextCampaign.class::cast
        );
        TextCampaign actualCampaign = validCampaigns.get(textCampaignInfoFirst.getCampaignId());
        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update_ThreeCampaignsTwoValidOneInvalid() {
        String newName = "newName";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);
        List<Long> allowedPageIds = List.of(123L, 456L, 789L);
        Boolean enableCompanyInfo = false;
        Boolean excludePausedCompetingAds = true;
        Long existingDialogId = steps.dialogSteps().createStandaloneDefaultDialog(clientInfo).getDialog().getId();
        Long invalidDialogId = -1L;

        GdUpdateCampaignUnion campaignUnionFirst =
                getTextGdUpdateCampaignUnion(textCampaign(textCampaignInfoFirst.getCampaignId(), newName,
                        tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                        DISABLED_IPS, PLACEMENT_TYPES, BROAD_MATCH, existingDialogId,
                        defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID)));
        campaignUnionFirst.getTextCampaign()
                .withAllowedPageIds(allowedPageIds)
                .withEnableCompanyInfo(enableCompanyInfo)
                .withExcludePausedCompetingAds(excludePausedCompetingAds);
        GdUpdateCampaignUnion campaignUnionSecond =
                getTextGdUpdateCampaignUnion(textCampaign(textCampaignInfoSecond.getCampaignId(), newName,
                        dayAfterTomorrow, tomorrow, List.of(DISABLED_DOMAINS.iterator().next(), "mail.ru",
                                DISABLED_SSP.iterator().next(), "notssp"),
                        List.of(DISABLED_IPS.iterator().next(), PRIVATE_IP, INTERNAL_IP, INVALID_IP), null, BROAD_MATCH,
                        invalidDialogId, defaultGdTimeTarget()));
        GdCampaignNotificationRequest secondCampaignNotification =
                campaignUnionSecond.getTextCampaign().getNotification();
        secondCampaignNotification.getSmsSettings().getSmsTime().getStartTime().setHour(INVALID_HOUR);
        secondCampaignNotification.getEmailSettings().setEmail("invalidEmail");

        GdUpdateCampaignUnion campaignUnionThird =
                getTextGdUpdateCampaignUnion(textCampaign(textCampaignInfoThird.getCampaignId(), newName,
                        tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                        DISABLED_IPS, emptySet(), BROAD_MATCH, null,
                        defaultGdTimeTarget()));

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(campaignUnionFirst, campaignUnionSecond, campaignUnionThird));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = asList(
                new GdUpdateCampaignPayloadItem().withId(textCampaignInfoFirst.getCampaignId()),
                null,
                new GdUpdateCampaignPayloadItem().withId(textCampaignInfoThird.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors()).isNotEmpty();

        GdCampaignNotificationRequest notification = campaignUnionFirst.getTextCampaign().getNotification();
        TimeInterval smsTime = toTimeInterval(notification.getSmsSettings().getSmsTime());
        EnumSet<SmsFlag> smsFlags = toSmsFlags(notification.getSmsSettings().getEnableEvents());
        GdCampaignEmailSettingsRequest emailSettings = notification.getEmailSettings();

        TextCampaign expectedCampaign = new TextCampaign()
                .withId(textCampaignInfoFirst.getCampaignId())
                .withClientId(textCampaignInfoFirst.getClientId().asLong())
                .withWalletId(textCampaignInfoFirst.getCampaign().getWalletId())
                .withName(newName)
                .withStartDate(tomorrow)
                .withEndDate(dayAfterTomorrow)
                .withType(CampaignType.TEXT)
                .withDisabledDomains(DISABLED_DOMAINS)
                .withDisabledSsp(DISABLED_SSP)
                .withDisabledIps(DISABLED_IPS)
                .withPlacementTypes(toPlacementTypes(PLACEMENT_TYPES))
                .withBroadMatch(toBroadMatch(BROAD_MATCH))
                .withHasTitleSubstitution(false)
                .withHasTurboApp(false)
                .withHasExtendedGeoTargeting(true)
                .withTimeZoneId(KALININGRAD_TIMEZONE_ID)
                .withSmsTime(smsTime)
                .withSmsFlags(smsFlags)
                .withEmail(emailSettings.getEmail())
                .withEnableCheckPositionEvent(emailSettings.getCheckPositionInterval() != null)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(emailSettings.getCheckPositionInterval()))
                .withWarningBalance(emailSettings.getWarningBalance())
                .withEnableSendAccountNews(emailSettings.getSendAccountNews())
                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnablePausedByDayBudgetEvent(emailSettings.getStopByReachDailyBudget())
                .withHasSiteMonitoring(campaignUnionFirst.getTextCampaign().getHasSiteMonitoring())
                .withHasAddMetrikaTagToUrl(campaignUnionFirst.getTextCampaign().getHasAddMetrikaTagToUrl())
                .withHasAddOpenstatTagToUrl(campaignUnionFirst.getTextCampaign().getHasAddOpenstatTagToUrl())
                .withStrategy(defaultStrategy())
                .withAllowedPageIds(allowedPageIds)
                .withEnableCompanyInfo(enableCompanyInfo)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withExcludePausedCompetingAds(excludePausedCompetingAds)
                .withClientDialogId(existingDialogId)
                .withBrandSafetyCategories(emptyList());

        Map<Long, TextCampaign> validCampaigns = listToMap(campaignTypedRepository.getTypedCampaigns(
                        clientInfo.getShard(),
                        List.of(textCampaignInfoFirst.getCampaignId(), textCampaignInfoThird.getCampaignId())),
                BaseCampaign::getId, TextCampaign.class::cast
        );
        TextCampaign actualFirstCampaign = validCampaigns.get(textCampaignInfoFirst.getCampaignId());
        assertThat(actualFirstCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));

        TextCampaign actualThirdCampaign = validCampaigns.get(textCampaignInfoThird.getCampaignId());
        assertThat(actualThirdCampaign.getClientDialogId()).isNull();
    }

    @Test
    public void update_ThreeMcBannerCampaignsTwoValidOneInvalid() {
        Long campaignIdFirst = steps.typedCampaignSteps()
                .createDefaultMcBannerCampaign(clientInfo.getChiefUserInfo(), clientInfo).getId();
        Long campaignIdSecond = steps.typedCampaignSteps()
                .createDefaultMcBannerCampaign(clientInfo.getChiefUserInfo(), clientInfo).getId();
        Long campaignIdThird = steps.typedCampaignSteps()
                .createDefaultMcBannerCampaign(clientInfo.getChiefUserInfo(), clientInfo).getId();

        String newName = "new mcbanner campaign";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnionFirst = getMcBannerGdUpdateCampaignUnion(
                defaultMcBannerCampaign(campaignIdFirst)
                        .withName(newName)
                        .withStartDate(tomorrow)
                        .withEndDate(dayAfterTomorrow)
                        .withDisabledPlaces(ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP))
                        .withDisabledIps(DISABLED_IPS)
                        .withTimeTarget(defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID))
                        .withHasExtendedGeoTargeting(true));

        GdUpdateCampaignUnion campaignUnionSecond = getMcBannerGdUpdateCampaignUnion(
                defaultMcBannerCampaign(campaignIdSecond)
                        .withStartDate(dayAfterTomorrow)
                        .withEndDate(tomorrow));

        GdCampaignNotificationRequest secondCampaignNotification =
                campaignUnionSecond.getMcBannerCampaign().getNotification();
        secondCampaignNotification.getSmsSettings().getSmsTime().getStartTime().setHour(INVALID_HOUR);
        secondCampaignNotification.getEmailSettings().setEmail("invalidEmail");

        GdUpdateCampaignUnion campaignUnionThird = getMcBannerGdUpdateCampaignUnion(
                defaultMcBannerCampaign(campaignIdThird));

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(campaignUnionFirst, campaignUnionSecond, campaignUnionThird));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = asList(
                new GdUpdateCampaignPayloadItem().withId(campaignIdFirst),
                null,
                new GdUpdateCampaignPayloadItem().withId(campaignIdThird));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors()).isNotEmpty();

        GdCampaignNotificationRequest notification = campaignUnionFirst.getMcBannerCampaign().getNotification();
        TimeInterval smsTime = toTimeInterval(notification.getSmsSettings().getSmsTime());
        EnumSet<SmsFlag> smsFlags = toSmsFlags(notification.getSmsSettings().getEnableEvents());
        GdCampaignEmailSettingsRequest emailSettings = notification.getEmailSettings();

        McBannerCampaign expectedCampaign = new McBannerCampaign()
                .withId(campaignIdFirst)
                .withClientId(clientInfo.getClientId().asLong())
                .withWalletId(0L)
                .withName(newName)
                .withStartDate(tomorrow)
                .withEndDate(dayAfterTomorrow)
                .withType(CampaignType.MCBANNER)
                .withDisabledDomains(DISABLED_DOMAINS)
                .withDisabledSsp(DISABLED_SSP)
                .withDisabledIps(DISABLED_IPS)
                .withHasExtendedGeoTargeting(true)
                .withTimeZoneId(KALININGRAD_TIMEZONE_ID)
                .withSmsTime(smsTime)
                .withSmsFlags(smsFlags)
                .withEmail(emailSettings.getEmail())
                .withWarningBalance(emailSettings.getWarningBalance())
                .withEnableSendAccountNews(emailSettings.getSendAccountNews())
                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnablePausedByDayBudgetEvent(emailSettings.getStopByReachDailyBudget())
                .withHasSiteMonitoring(campaignUnionFirst.getMcBannerCampaign().getHasSiteMonitoring())
                .withHasAddMetrikaTagToUrl(campaignUnionFirst.getMcBannerCampaign().getHasAddMetrikaTagToUrl())
                .withHasAddOpenstatTagToUrl(campaignUnionFirst.getMcBannerCampaign().getHasAddOpenstatTagToUrl())
                .withStrategy((DbStrategy) defaultStrategy()
                        .withPlatform(SEARCH)
                        .withStrategy(null))
                .withBrandSafetyCategories(emptyList());

        Map<Long, McBannerCampaign> validCampaigns = listToMap(campaignTypedRepository.getTypedCampaigns(
                        clientInfo.getShard(),
                        List.of(campaignIdFirst, campaignIdThird)),
                BaseCampaign::getId, McBannerCampaign.class::cast
        );
        McBannerCampaign actualFirstCampaign = validCampaigns.get(campaignIdFirst);
        assertThat(actualFirstCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update_ThreeSmartCampaignsTwoValidOneInvalid() {
        Long smartCampaignIdFirst = createSmartCampaign();
        Long smartCampaignIdSecond = createSmartCampaign();
        Long smartCampaignIdThird = createSmartCampaign();

        String newName = "new smart campaign";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);

        GdUpdateCampaignUnion campaignUnionFirst =
                getSmartGdUpdateCampaignUnion(smartCampaign(smartCampaignIdFirst, newName,
                        tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                        DISABLED_IPS, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID)));
        GdUpdateCampaignUnion campaignUnionSecond =
                getSmartGdUpdateCampaignUnion(smartCampaign(smartCampaignIdSecond, newName,
                        dayAfterTomorrow, tomorrow, List.of(DISABLED_DOMAINS.iterator().next(), "mail.ru",
                                DISABLED_SSP.iterator().next(), "notssp"),
                        List.of(DISABLED_IPS.iterator().next(), PRIVATE_IP, INTERNAL_IP, INVALID_IP),
                        defaultGdTimeTarget()));
        GdCampaignNotificationRequest secondCampaignNotification =
                campaignUnionSecond.getSmartCampaign().getNotification();
        secondCampaignNotification.getSmsSettings().getSmsTime().getStartTime().setHour(INVALID_HOUR);
        secondCampaignNotification.getEmailSettings().setEmail("invalidEmail");

        GdUpdateCampaignUnion campaignUnionThird =
                getSmartGdUpdateCampaignUnion(smartCampaign(smartCampaignIdThird, newName,
                        tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                        DISABLED_IPS, defaultGdTimeTarget()));

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(campaignUnionFirst, campaignUnionSecond, campaignUnionThird));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = asList(
                new GdUpdateCampaignPayloadItem().withId(smartCampaignIdFirst),
                null,
                new GdUpdateCampaignPayloadItem().withId(smartCampaignIdThird));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors()).isNotEmpty();

        GdCampaignNotificationRequest notification = campaignUnionFirst.getSmartCampaign().getNotification();
        TimeInterval smsTime = toTimeInterval(notification.getSmsSettings().getSmsTime());
        EnumSet<SmsFlag> smsFlags = toSmsFlags(notification.getSmsSettings().getEnableEvents());
        GdCampaignEmailSettingsRequest emailSettings = notification.getEmailSettings();

        DbStrategy expectedStrategy = defaultAutobudgetRoiStrategy(0L, false);
        expectedStrategy.getStrategyData().setGoalId(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);

        SmartCampaign expectedCampaign = new SmartCampaign()
                .withId(smartCampaignIdFirst)
                .withClientId(clientInfo.getClientId().asLong())
                .withWalletId(0L)
                .withName(newName)
                .withStartDate(tomorrow)
                .withEndDate(dayAfterTomorrow)
                .withType(CampaignType.PERFORMANCE)
                .withDisabledDomains(DISABLED_DOMAINS)
                .withDisabledSsp(DISABLED_SSP)
                .withDisabledIps(DISABLED_IPS)
                .withHasTurboApp(false)
                .withTimeZoneId(KALININGRAD_TIMEZONE_ID)
                .withSmsTime(smsTime)
                .withSmsFlags(smsFlags)
                .withEmail(emailSettings.getEmail())
                .withWarningBalance(emailSettings.getWarningBalance())
                .withEnableSendAccountNews(emailSettings.getSendAccountNews())
                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnablePausedByDayBudgetEvent(emailSettings.getStopByReachDailyBudget())
                .withHasAddMetrikaTagToUrl(campaignUnionFirst.getSmartCampaign().getHasAddMetrikaTagToUrl())
                .withStrategy(expectedStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true)
                .withEnableCompanyInfo(true)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withHasTurboSmarts(DEFAULT_HAS_TURBO_SMARTS)
                .withBrandSafetyCategories(emptyList());

        Map<Long, SmartCampaign>

                validCampaigns = listToMap(campaignTypedRepository.getTypedCampaigns(
                        clientInfo.getShard(),
                        List.of(smartCampaignIdFirst, smartCampaignIdThird)),
                BaseCampaign::getId, SmartCampaign.class::cast
        );
        SmartCampaign actualFirstCampaign = validCampaigns.get(smartCampaignIdFirst);
        assertThat(actualFirstCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update_ThreeDynamicCampaignsTwoValidOneInvalid() {
        CampaignInfo dynamicCampaignInfoFirst = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        CampaignInfo dynamicCampaignInfoSecond = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        CampaignInfo dynamicCampaignInfoThird = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);

        String newName = "new dynamic campaign";
        LocalDate now = LocalDate.now();

        LocalDate tomorrow = now.plusDays(1);
        LocalDate dayAfterTomorrow = now.plusDays(2);
        List<Long> allowedPageIds = List.of(123L, 456L, 789L);
        Boolean enableCompanyInfo = false;

        GdUpdateCampaignUnion campaignUnionFirst =
                getDynamicGdUpdateCampaignUnion(dynamicCampaign(dynamicCampaignInfoFirst.getCampaignId(), newName,
                        tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP), DISABLED_IPS,
                        PLACEMENT_TYPES, defaultGdTimeTarget().withIdTimeZone(KALININGRAD_TIMEZONE_ID)));
        campaignUnionFirst.getDynamicCampaign()
                .withAllowedPageIds(allowedPageIds)
                .withEnableCompanyInfo(enableCompanyInfo);
        GdUpdateCampaignUnion campaignUnionSecond =
                getDynamicGdUpdateCampaignUnion(dynamicCampaign(dynamicCampaignInfoSecond.getCampaignId(), newName,
                        dayAfterTomorrow, tomorrow, List.of(DISABLED_DOMAINS.iterator().next(), "mail.ru",
                                DISABLED_SSP.iterator().next(), "notssp"),
                        List.of(DISABLED_IPS.iterator().next(), PRIVATE_IP, INTERNAL_IP, INVALID_IP), null,
                        defaultGdTimeTarget()));
        GdCampaignNotificationRequest secondCampaignNotification =
                campaignUnionSecond.getDynamicCampaign().getNotification();
        secondCampaignNotification.getSmsSettings().getSmsTime().getStartTime().setHour(INVALID_HOUR);
        secondCampaignNotification.getEmailSettings().setEmail("invalidEmail");

        GdUpdateCampaignUnion campaignUnionThird =
                getDynamicGdUpdateCampaignUnion(dynamicCampaign(dynamicCampaignInfoThird.getCampaignId(), newName,
                        tomorrow, dayAfterTomorrow, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP), DISABLED_IPS,
                        emptySet(), defaultGdTimeTarget()));

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(campaignUnionFirst, campaignUnionSecond, campaignUnionThird));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = asList(
                new GdUpdateCampaignPayloadItem().withId(dynamicCampaignInfoFirst.getCampaignId()),
                null,
                new GdUpdateCampaignPayloadItem().withId(dynamicCampaignInfoThird.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors()).isNotEmpty();

        GdCampaignNotificationRequest notification = campaignUnionFirst.getDynamicCampaign().getNotification();
        TimeInterval smsTime = toTimeInterval(notification.getSmsSettings().getSmsTime());
        EnumSet<SmsFlag> smsFlags = toSmsFlags(notification.getSmsSettings().getEnableEvents());
        GdCampaignEmailSettingsRequest emailSettings = notification.getEmailSettings();

        DynamicCampaign expectedCampaign = new DynamicCampaign()
                .withId(dynamicCampaignInfoFirst.getCampaignId())
                .withClientId(dynamicCampaignInfoFirst.getClientId().asLong())
                .withWalletId(dynamicCampaignInfoFirst.getCampaign().getWalletId())
                .withName(newName)
                .withStartDate(tomorrow)
                .withEndDate(dayAfterTomorrow)
                .withType(CampaignType.DYNAMIC)
                .withDisabledDomains(DISABLED_DOMAINS)
                .withDisabledSsp(DISABLED_SSP)
                .withDisabledIps(DISABLED_IPS)
                .withPlacementTypes(toPlacementTypes(PLACEMENT_TYPES))
                .withHasTitleSubstitution(false)
                .withHasTurboApp(false)
                .withHasExtendedGeoTargeting(true)
                .withTimeZoneId(KALININGRAD_TIMEZONE_ID)
                .withSmsTime(smsTime)
                .withSmsFlags(smsFlags)
                .withEmail(emailSettings.getEmail())
                .withEnableCheckPositionEvent(emailSettings.getCheckPositionInterval() != null)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(emailSettings.getCheckPositionInterval()))
                .withWarningBalance(emailSettings.getWarningBalance())
                .withEnableSendAccountNews(emailSettings.getSendAccountNews())
                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnablePausedByDayBudgetEvent(emailSettings.getStopByReachDailyBudget())
                .withHasSiteMonitoring(campaignUnionFirst.getDynamicCampaign().getHasSiteMonitoring())
                .withHasAddMetrikaTagToUrl(campaignUnionFirst.getDynamicCampaign().getHasAddMetrikaTagToUrl())
                .withHasAddOpenstatTagToUrl(campaignUnionFirst.getDynamicCampaign().getHasAddOpenstatTagToUrl())
                .withStrategy(defaultStrategy())
                .withAllowedPageIds(allowedPageIds)
                .withEnableCompanyInfo(enableCompanyInfo)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withBrandSafetyCategories(emptyList());

        Map<Long, DynamicCampaign> validCampaigns = listToMap(campaignTypedRepository.getTypedCampaigns(
                        clientInfo.getShard(),
                        List.of(dynamicCampaignInfoFirst.getCampaignId(), dynamicCampaignInfoThird.getCampaignId())),
                BaseCampaign::getId, DynamicCampaign.class::cast
        );
        DynamicCampaign actualFirstCampaign = validCampaigns.get(dynamicCampaignInfoFirst.getCampaignId());
        assertThat(actualFirstCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update_ThreeMobileContentCampaignsTwoValidOneInvalid() {
        CampaignInfo mobileContentCampaignInfoFirst = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        CampaignInfo mobileContentCampaignInfoSecond = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        CampaignInfo mobileContentCampaignInfoThird = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        List<Long> allowedPageIds = List.of(123L, 456L, 789L);

        GdUpdateMobileContentCampaign updateFirstMobileContentCampaign =
                defaultMobileContentCampaign(mobileContentCampaignInfoFirst.getCampaignId(), mobileAppId);
        GdUpdateCampaignUnion campaignUnionFirst =
                getMobileContentGdUpdateCampaignUnion(updateFirstMobileContentCampaign);

        GdUpdateCampaignUnion campaignUnionSecond =
                getMobileContentGdUpdateCampaignUnion(defaultMobileContentCampaign(
                        mobileContentCampaignInfoSecond.getCampaignId(), mobileAppId));
        GdCampaignNotificationRequest secondCampaignNotification =
                campaignUnionSecond.getMobileContentCampaign().getNotification();
        secondCampaignNotification.getSmsSettings().getSmsTime().getStartTime().setHour(INVALID_HOUR);
        secondCampaignNotification.getEmailSettings().setEmail("invalidEmail");

        GdUpdateCampaignUnion campaignUnionThird =
                getMobileContentGdUpdateCampaignUnion(defaultMobileContentCampaign(
                        mobileContentCampaignInfoThird.getCampaignId(), mobileAppId));
        campaignUnionThird.getMobileContentCampaign()
                .withAllowedPageIds(allowedPageIds);

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(campaignUnionFirst, campaignUnionSecond, campaignUnionThird));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = asList(
                new GdUpdateCampaignPayloadItem().withId(mobileContentCampaignInfoFirst.getCampaignId()),
                null,
                new GdUpdateCampaignPayloadItem().withId(mobileContentCampaignInfoThird.getCampaignId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors()).isNotEmpty();

        MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                .withId(mobileContentCampaignInfoFirst.getCampaignId())
                .withName(updateFirstMobileContentCampaign.getName())
                .withStartDate(LocalDate.now().plusDays(2))
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withEmail(updateFirstMobileContentCampaign.getNotification().getEmailSettings().getEmail())
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(EnumSet.of(CAMP_FINISHED_SMS, MODERATE_RESULT_SMS))
                .withStrategy((DbStrategy) new DbStrategy()
                        .withPlatform(CampaignsPlatform.BOTH)
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withStrategyData(new StrategyData()
                                .withName("default")
                                .withSum(BigDecimal.valueOf(5500))
                                .withVersion(1L)
                                .withUnknownFields(emptyMap())))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO.setScale(2))
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withBrandSafetyCategories(emptyList())
                .withMobileAppId(mobileAppId)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDeviceTypeTargeting(EnumSet.of(MobileAppDeviceTypeTargeting.TABLET))
                .withNetworkTargeting(EnumSet.of(MobileAppNetworkTargeting.WI_FI));

        Map<Long, MobileContentCampaign> validCampaigns =
                listToMap(campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                                List.of(mobileContentCampaignInfoFirst.getCampaignId(),
                                        mobileContentCampaignInfoThird.getCampaignId())),
                        BaseCampaign::getId, MobileContentCampaign.class::cast);
        MobileContentCampaign actualFirstCampaign = validCampaigns.get(mobileContentCampaignInfoFirst.getCampaignId());
        assertThat(actualFirstCampaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));

        MobileContentCampaign actualThirdCampaign = validCampaigns.get(mobileContentCampaignInfoThird.getCampaignId());
        assertThat(actualThirdCampaign.getAllowedPageIds()).isEqualTo(allowedPageIds);
    }

    @Test
    @Description("Проверка проставления дефолтных значений для необязательных полей, которые в базе nonNull")
    public void update_checkSetDefaultValuesForNullableFields() {
        String newName = "newName" + RandomStringUtils.randomAlphabetic(4);
        GdUpdateTextCampaign gdUpdateTextCampaign = textCampaign(textCampaignInfoFirst.getCampaignId(), newName,
                LocalDate.now(), null, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                DISABLED_IPS, null, BROAD_MATCH, null, defaultGdTimeTarget());
        gdUpdateTextCampaign.getNotification().getEmailSettings()
                .withCheckPositionInterval(null)
                .withSendAccountNews(null)
                .withStopByReachDailyBudget(null)
                .withXlsReady(null)
                .withWarningBalance(null);
        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(getTextGdUpdateCampaignUnion(gdUpdateTextCampaign)));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        GdUpdateCampaignPayload expectedPayload = new GdUpdateCampaignPayload()
                .withUpdatedCampaigns(List.of(
                        new GdUpdateCampaignPayloadItem().withId(textCampaignInfoFirst.getCampaignId())))
                .withValidationResult(null);
        assertThat(gdUpdateCampaignPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        TextCampaign expectedCampaign = new TextCampaign()
                .withId(textCampaignInfoFirst.getCampaignId())
                .withName(newName)
                .withEnableCheckPositionEvent(CampaignConstants.DEFAULT_ENABLE_CHECK_POSITION_EVENT)
                .withCheckPositionIntervalEvent(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(CampaignConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS)
                .withEnableOfflineStatNotice(CampaignConstants.DEFAULT_ENABLE_OFFLINE_STAT_NOTICE)
                .withAttributionModel(defaultAttributionModel)
                .withEnablePausedByDayBudgetEvent(CampaignConstants.DEFAULT_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withBrandSafetyCategories(emptyList())
                .withPlacementTypes(emptySet());

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfoFirst.getShard(),
                        singletonList(textCampaignInfoFirst.getCampaignId()));

        assertThat(typedCampaigns)
                .hasSize(1)
                .element(0)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    @Description("Проверка проставления дефолтных значений у McBanner для необязательных полей, которые в базе nonNull")
    public void update_checkSetDefaultValuesForNullableFieldsForMcBannerCampaign() {
        Long campaignIdFirst = steps.typedCampaignSteps()
                .createMcBannerCampaign(clientInfo.getChiefUserInfo(), clientInfo,
                        defaultMcBannerCampaignWithSystemFields(clientInfo)
                                .withWarningBalance(DEFAULT_CAMPAIGN_WARNING_BALANCE)
                                .withEnablePausedByDayBudgetEvent(false)
                                .withEnableOfflineStatNotice(false)).getId();

        String newName = "new mcbanner campaign " + RandomStringUtils.randomAlphabetic(4);
        GdUpdateMcBannerCampaign gdUpdateCampaign = defaultMcBannerCampaign(campaignIdFirst)
                .withName(newName);

        gdUpdateCampaign.getNotification().getEmailSettings()
                .withCheckPositionInterval(null)
                .withSendAccountNews(null)
                .withStopByReachDailyBudget(null)
                .withXlsReady(null)
                .withWarningBalance(null);
        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(getMcBannerGdUpdateCampaignUnion(gdUpdateCampaign)));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        GdUpdateCampaignPayload expectedPayload = new GdUpdateCampaignPayload()
                .withUpdatedCampaigns(List.of(
                        new GdUpdateCampaignPayloadItem().withId(campaignIdFirst)))
                .withValidationResult(null);
        assertThat(gdUpdateCampaignPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        McBannerCampaign expectedCampaign = new McBannerCampaign()
                .withId(campaignIdFirst)
                .withName(newName)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableOfflineStatNotice(CampaignConstants.DEFAULT_ENABLE_OFFLINE_STAT_NOTICE)
                .withAttributionModel(defaultAttributionModel)
                .withEnablePausedByDayBudgetEvent(CampaignConstants.DEFAULT_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withBrandSafetyCategories(emptyList());

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(), singletonList(campaignIdFirst));

        assertThat(typedCampaigns)
                .hasSize(1)
                .element(0)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    @Description("Проверка проставления дефолтных значений у Smart для необязательных полей, которые в базе nonNull")
    public void update_checkSetDefaultValuesForNullableFieldsForSmartCampaigns() {
        Campaign campaign = activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withMetrikaCounters(List.of(COUNTER_ID));
        CampaignInfo campaignInfoFirst = steps.campaignSteps().createCampaign(campaign, clientInfo);
        var campaignId = campaignInfoFirst.getCampaignId();

        String newName = "newName" + RandomStringUtils.randomAlphabetic(4);
        GdUpdateSmartCampaign gdUpdateSmartCampaign = smartCampaign(campaignId, newName,
                LocalDate.now(), null, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP),
                DISABLED_IPS, defaultGdTimeTarget());
        gdUpdateSmartCampaign.withMetrikaCounters(List.of(COUNTER_ID.intValue()));
        gdUpdateSmartCampaign.getNotification().getEmailSettings()
                .withCheckPositionInterval(null)
                .withSendAccountNews(null)
                .withStopByReachDailyBudget(null)
                .withXlsReady(null)
                .withWarningBalance(null);
        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(getSmartGdUpdateCampaignUnion(gdUpdateSmartCampaign)));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        GdUpdateCampaignPayload expectedPayload = new GdUpdateCampaignPayload()
                .withUpdatedCampaigns(List.of(
                        new GdUpdateCampaignPayloadItem().withId(campaignId)))
                .withValidationResult(null);
        assertThat(gdUpdateCampaignPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        SmartCampaign expectedCampaign = new SmartCampaign()
                .withId(campaignId)
                .withName(newName)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(CampaignConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS)
                .withEnableOfflineStatNotice(CampaignConstants.DEFAULT_ENABLE_OFFLINE_STAT_NOTICE)
                .withAttributionModel(defaultAttributionModel)
                .withEnablePausedByDayBudgetEvent(CampaignConstants.DEFAULT_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withBrandSafetyCategories(emptyList());

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(), singletonList(campaignId));

        assertThat(typedCampaigns)
                .hasSize(1)
                .element(0)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    @Description("Проверка проставления дефолтных значений у ДО для необязательных полей, которые в базе nonNull")
    public void update_checkSetDefaultValuesForNullableFieldsForDynamicCampaigns() {
        CampaignInfo dynamicCampaignInfoFirst = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);

        String newName = "newName" + RandomStringUtils.randomAlphabetic(4);
        GdUpdateDynamicCampaign gdUpdateDynamicCampaign = dynamicCampaign(dynamicCampaignInfoFirst.getCampaignId(),
                newName, LocalDate.now(), null, ListUtils.union(DISABLED_DOMAINS, DISABLED_SSP), DISABLED_IPS,
                null, defaultGdTimeTarget());
        gdUpdateDynamicCampaign.getNotification().getEmailSettings()
                .withCheckPositionInterval(null)
                .withSendAccountNews(null)
                .withStopByReachDailyBudget(null)
                .withXlsReady(null)
                .withWarningBalance(null);
        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(getDynamicGdUpdateCampaignUnion(gdUpdateDynamicCampaign)));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        GdUpdateCampaignPayload expectedPayload = new GdUpdateCampaignPayload()
                .withUpdatedCampaigns(List.of(
                        new GdUpdateCampaignPayloadItem().withId(dynamicCampaignInfoFirst.getCampaignId())))
                .withValidationResult(null);
        assertThat(gdUpdateCampaignPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        DynamicCampaign expectedCampaign = new DynamicCampaign()
                .withId(dynamicCampaignInfoFirst.getCampaignId())
                .withName(newName)
                .withEnableCheckPositionEvent(CampaignConstants.DEFAULT_ENABLE_CHECK_POSITION_EVENT)
                .withCheckPositionIntervalEvent(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(CampaignConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS)
                .withEnableOfflineStatNotice(CampaignConstants.DEFAULT_ENABLE_OFFLINE_STAT_NOTICE)
                .withAttributionModel(defaultAttributionModel)
                .withEnablePausedByDayBudgetEvent(CampaignConstants.DEFAULT_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withBrandSafetyCategories(emptyList())
                .withPlacementTypes(emptySet());

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(dynamicCampaignInfoFirst.getShard(),
                        singletonList(dynamicCampaignInfoFirst.getCampaignId()));

        assertThat(typedCampaigns)
                .hasSize(1)
                .element(0)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    @Description("Проверка проставления дефолтных значений у РМП для необязательных полей, которые в базе nonNull")
    public void update_checkSetDefaultValuesForNullableFieldsForMobileContentCampaigns() {
        CampaignInfo mobileContaintCampaignInfoFirst = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        GdUpdateMobileContentCampaign gdUpdateMobileContentCampaign =
                defaultMobileContentCampaign(mobileContaintCampaignInfoFirst.getCampaignId(), mobileAppId);
        gdUpdateMobileContentCampaign.getNotification().getEmailSettings()
                .withCheckPositionInterval(null)
                .withSendAccountNews(null)
                .withStopByReachDailyBudget(null)
                .withXlsReady(null)
                .withWarningBalance(null);
        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(getMobileContentGdUpdateCampaignUnion(gdUpdateMobileContentCampaign)));

        GdUpdateCampaignPayload gdUpdateCampaignPayload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);

        GdUpdateCampaignPayload expectedPayload = new GdUpdateCampaignPayload()
                .withUpdatedCampaigns(List.of(
                        new GdUpdateCampaignPayloadItem().withId(mobileContaintCampaignInfoFirst.getCampaignId())))
                .withValidationResult(null);
        assertThat(gdUpdateCampaignPayload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                .withId(mobileContaintCampaignInfoFirst.getCampaignId())
                .withName(gdUpdateMobileContentCampaign.getName())
                .withEnableCheckPositionEvent(CampaignConstants.DEFAULT_ENABLE_CHECK_POSITION_EVENT)
                .withCheckPositionIntervalEvent(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(CampaignConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS)
                .withEnableOfflineStatNotice(CampaignConstants.DEFAULT_ENABLE_OFFLINE_STAT_NOTICE)
                .withEnablePausedByDayBudgetEvent(CampaignConstants.DEFAULT_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withBrandSafetyCategories(emptyList());

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(mobileContaintCampaignInfoFirst.getShard(),
                        singletonList(mobileContaintCampaignInfoFirst.getCampaignId()));

        assertThat(typedCampaigns)
                .hasSize(1)
                .element(0)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private static GdUpdateTextCampaign textCampaign(Long id, String name, LocalDate startDate,
                                                     @Nullable LocalDate endDate,
                                                     List<String> disabledPlaces, List<String> disabledIps,
                                                     @Nullable Set<GdCampaignPlacementType> placementTypes,
                                                     GdBroadMatchRequest broadMatch,
                                                     @Nullable Long dialogId,
                                                     @Nullable GdTimeTarget timeTarget) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);

        return new GdUpdateTextCampaign()
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
                .withDisabledPlaces(disabledPlaces)
                .withDisabledIps(disabledIps)
                .withPlacementTypes(placementTypes)
                .withBroadMatch(broadMatch)
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
                .withHasTitleSubstitute(false)
                .withContentLanguage(null)
                .withAttributionModel(null)
                .withHasExtendedGeoTargeting(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withHasSiteMonitoring(false)
                .withClientDialogId(dialogId)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withEnableCompanyInfo(true)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withExcludePausedCompetingAds(false)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withClientDialogId(dialogId);
    }

    private static GdUpdateSmartCampaign smartCampaign(Long id,
                                                       String name,
                                                       LocalDate startDate,
                                                       @Nullable LocalDate endDate,
                                                       List<String> disabledPlaces,
                                                       List<String> disabledIps,
                                                       @Nullable GdTimeTarget timeTarget) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);

        return new GdUpdateSmartCampaign()
                .withId(id)
                .withName(name)
                .withEndDate(endDate)
                .withStartDate(startDate)
                .withTimeTarget(timeTarget)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true)
                .withMeaningfulGoals(List.of(new GdMeaningfulGoalRequest()
                        .withGoalId(MEANINGFUL_GOAL_ID)
                        .withConversionValue(BigDecimal.TEN)))
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_ROI)
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withRoiCoef(new BigDecimal("1"))
                                .withReserveReturn(20L)
                                .withProfitability(new BigDecimal("20"))
                                .withGoalId(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)))
                .withDisabledPlaces(disabledPlaces)
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
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withMetrikaCounters(List.of(COUNTER_ID.intValue()));
    }

    private Long createSmartCampaign() {
        SmartCampaign campaign = TestCampaigns.defaultSmartCampaignWithSystemFields(clientInfo)
                .withMetrikaCounters(List.of(COUNTER_ID));

        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid());
        List<Long> campaignIds = campaignModifyRepository.addCampaigns(dslContextProvider.ppc(clientInfo.getShard()),
                addCampaignParametersContainer, Collections.singletonList(campaign));
        return campaignIds.get(0);
    }

    private static GdUpdateDynamicCampaign dynamicCampaign(Long id, String name, LocalDate startDate,
                                                           @Nullable LocalDate endDate,
                                                           List<String> disabledPlaces, List<String> disabledIps,
                                                           @Nullable Set<GdCampaignPlacementType> placementTypes,
                                                           @Nullable GdTimeTarget timeTarget) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);

        return new GdUpdateDynamicCampaign()
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
                .withDisabledPlaces(disabledPlaces)
                .withDisabledIps(disabledIps)
                .withPlacementTypes(placementTypes)
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
                .withHasTitleSubstitute(false)
                .withContentLanguage(null)
                .withAttributionModel(null)
                .withHasExtendedGeoTargeting(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withHasSiteMonitoring(false)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withEnableCompanyInfo(true)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel));
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
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_WEEK_BUNDLE)
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withLimitClicks(1234L)
                                .withAvgBid(BigDecimal.valueOf(123))))
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
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withBrandSafetyCategories(List.of(4_294_967_298L, 4_294_967_297L));
    }

    private static GdUpdateCampaignUnion getTextGdUpdateCampaignUnion(GdUpdateTextCampaign textCampaign) {
        return new GdUpdateCampaignUnion()
                .withTextCampaign(textCampaign);
    }

    private static GdUpdateCampaignUnion getMcBannerGdUpdateCampaignUnion(GdUpdateMcBannerCampaign mcBannerCampaign) {
        return new GdUpdateCampaignUnion()
                .withMcBannerCampaign(mcBannerCampaign);
    }

    private static GdUpdateCampaignUnion getSmartGdUpdateCampaignUnion(GdUpdateSmartCampaign smartCampaign) {
        return new GdUpdateCampaignUnion()
                .withSmartCampaign(smartCampaign);
    }

    private static GdUpdateCampaignUnion getDynamicGdUpdateCampaignUnion(GdUpdateDynamicCampaign dynamicCampaign) {
        return new GdUpdateCampaignUnion()
                .withDynamicCampaign(dynamicCampaign);
    }

    private static GdUpdateCampaignUnion getContentPromotionGdUpdateCampaignUnion(GdUpdateContentPromotionCampaign contentPromotionCampaign) {
        return new GdUpdateCampaignUnion()
                .withContentPromotionCampaign(contentPromotionCampaign);
    }

    private static GdUpdateCampaignUnion getMobileContentGdUpdateCampaignUnion(GdUpdateMobileContentCampaign mobileContentCampaign) {
        return new GdUpdateCampaignUnion()
                .withMobileContentCampaign(mobileContentCampaign);
    }
}
