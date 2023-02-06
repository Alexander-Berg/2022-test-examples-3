package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignDeviceTypeTargeting;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignNetworkTargeting;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.data.TestGdAddCampaigns;
import ru.yandex.direct.grid.processing.data.TestGdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMobileContentCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;
import ru.yandex.direct.validation.result.DefectIds;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.CAMP_FINISHED_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.MODERATE_RESULT_SMS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED;
import static ru.yandex.direct.feature.FeatureName.SHOW_SKADNETWORK_ON_NEW_IOS_ENABLED;
import static ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignNetworkTargeting.CELLULAR;
import static ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignNetworkTargeting.WI_FI;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toTimeTarget;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на обновление РМП кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignMutationUpdateGraphqlServiceMobileContentCampaignTest {

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
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";

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
    private static long mobileAppId;
    private long campaignId;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        ClientInfo clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), IN_APP_EVENTS_IN_RMP_ENABLED, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_SKADNETWORK_ON_NEW_IOS_ENABLED, true);
        TestAuthHelper.setDirectAuthentication(operator);

        MobileAppInfo mobileAppInfo =
                steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    /**
     * Проверка обновления РМП кампании
     */
    @Test
    public void updateCampaign() {
        GdUpdateMobileContentCampaign mobileCampaign =
                TestGdUpdateCampaigns.defaultMobileContentCampaign(campaignId, mobileAppId)
                        .withIsSkadNetworkEnabled(true);

        GdUpdateCampaignPayload payload = sendRequest(mobileCampaign);
        checkState(payload.getValidationResult() == null && payload.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                .withId(campaignId)
                .withName(mobileCampaign.getName())
                .withStartDate(LocalDate.now().plusDays(2))
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withEmail(mobileCampaign.getNotification().getEmailSettings().getEmail())
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
                .withDisabledDomains(mobileCampaign.getDisabledPlaces())
                .withDisabledIps(mobileCampaign.getDisabledIps())
                .withDeviceTypeTargeting(EnumSet.of(MobileAppDeviceTypeTargeting.TABLET))
                .withAttributionModel(campaignConstantsService.getDefaultAttributionModel())
                .withNetworkTargeting(EnumSet.of(MobileAppNetworkTargeting.WI_FI))
                .withIsSkadNetworkEnabled(true);

        checkCampaign(payload, expectedCampaign);
    }

    @SuppressWarnings("unused")
    private static Object[] networkTargetingParameters() {
        return new Object[][]{
                {"Передан список из WI_FI -> нет ошибок",
                        Set.of(WI_FI),
                        EnumSet.of(MobileAppNetworkTargeting.WI_FI), null},
                {"Передан список из CELLULAR -> нет ошибок",
                        Set.of(CELLULAR),
                        EnumSet.of(MobileAppNetworkTargeting.CELLULAR), null},
                {"Передан список из WI_FI,CELLULAR -> нет ошибок",
                        Set.of(WI_FI, CELLULAR),
                        EnumSet.of(MobileAppNetworkTargeting.WI_FI, MobileAppNetworkTargeting.CELLULAR), null},
                {"Передан пустой список networkTargeting -> CANNOT_BE_EMPTY",
                        Set.of(),
                        EnumSet.noneOf(MobileAppNetworkTargeting.class), CollectionDefectIds.Gen.CANNOT_BE_EMPTY},
                {"Передан null список networkTargeting -> CANNOT_BE_NULL",
                        null,
                        null, CollectionDefectIds.Gen.CANNOT_BE_EMPTY},
        };
    }

    /**
     * Проверка обновления РМП при разных значениях таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("[{index}] {0}")
    public void updateCampaign_WithDifferentNetworkTargeting(@SuppressWarnings("unused") String description,
                                                             Set<GdMobileContentCampaignNetworkTargeting> networkTargeting,
                                                             EnumSet<MobileAppNetworkTargeting> expectNetworkTargeting,
                                                             @Nullable CollectionDefectIds.Gen expectedDefect) {
        GdUpdateMobileContentCampaign mobileCampaign =
                TestGdUpdateCampaigns.defaultMobileContentCampaign(campaignId, mobileAppId)
                        .withNetworkTargeting(networkTargeting);

        GdUpdateCampaignPayload payload = sendRequest(mobileCampaign);

        if (expectedDefect == null) {
            MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                    .withNetworkTargeting(expectNetworkTargeting);
            checkCampaign(payload, expectedCampaign);
        } else {
            checkError(payload, expectedDefect.getCode(), "campaignUpdateItems[0].networkTargeting");
        }
    }

    @SuppressWarnings("unused")
    private static Object[] deviceTypeTargetingParameters() {
        return new Object[][]{
                {"Передан список из PHONE -> нет ошибок",
                        Set.of(PHONE),
                        EnumSet.of(MobileAppDeviceTypeTargeting.PHONE), null},
                {"Передан список из TABLET -> нет ошибок",
                        Set.of(TABLET),
                        EnumSet.of(MobileAppDeviceTypeTargeting.TABLET), null},
                {"Передан список из PHONE,TABLET -> нет ошибок",
                        Set.of(PHONE, TABLET),
                        EnumSet.of(MobileAppDeviceTypeTargeting.PHONE, MobileAppDeviceTypeTargeting.TABLET), null},
                {"Передан пустой список deviceTypeTargeting -> CANNOT_BE_EMPTY",
                        Set.of(),
                        EnumSet.noneOf(MobileAppDeviceTypeTargeting.class), CollectionDefectIds.Gen.CANNOT_BE_EMPTY},
                {"Передан null список deviceTypeTargeting -> CANNOT_BE_NULL",
                        null,
                        null, CollectionDefectIds.Gen.CANNOT_BE_EMPTY},
        };
    }

    /**
     * Проверка обновления РМП при разных значениях таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "deviceTypeTargetingParameters")
    @TestCaseName("[{index}] {0}")
    public void updateCampaign_WithDifferentDeviceTypeTargeting(@SuppressWarnings("unused") String description,
                                                                Set<GdMobileContentCampaignDeviceTypeTargeting> deviceTargeting,
                                                                EnumSet<MobileAppDeviceTypeTargeting> expectDeviceTargeting,
                                                                @Nullable CollectionDefectIds.Gen expectedDefect) {
        GdUpdateMobileContentCampaign mobileCampaign =
                TestGdUpdateCampaigns.defaultMobileContentCampaign(campaignId, mobileAppId)
                        .withDeviceTypeTargeting(deviceTargeting);

        GdUpdateCampaignPayload payload = sendRequest(mobileCampaign);

        if (expectedDefect == null) {
            MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                    .withDeviceTypeTargeting(expectDeviceTargeting);
            checkCampaign(payload, expectedCampaign);
        } else {
            checkError(payload, expectedDefect.getCode(), "campaignUpdateItems[0].deviceTypeTargeting");
        }
    }

    @SuppressWarnings("unused")
    private static Object[] mobileAppIdParameters() {
        return new Object[][]{
                {"Передан существующий appId -> нет ошибок",
                        true, null},
                {"Передан не существующий appId -> OBJECT_NOT_FOUND",
                        false, DefectIds.OBJECT_NOT_FOUND},
        };
    }

    /**
     * Проверка обновления РМП при разных передаваемых значениях id приложения (MobileAppId)
     */
    @Test
    @Parameters(method = "mobileAppIdParameters")
    @TestCaseName("[{index}] {0}")
    public void updateCampaign_WithDifferentMobileAppId(@SuppressWarnings("unused") String description,
                                                        @Nullable Boolean sendExistingMobileAppId,
                                                        @Nullable DefectIds expectedDefect) {
        GdUpdateMobileContentCampaign mobileCampaign =
                TestGdUpdateCampaigns.defaultMobileContentCampaign(campaignId, mobileAppId);
        if (sendExistingMobileAppId != null) {
            mobileCampaign.setMobileAppId(sendExistingMobileAppId ? mobileAppId : RandomUtils.nextLong());
        } else {
            mobileCampaign.setMobileAppId(null);
        }

        GdUpdateCampaignPayload payload = sendRequest(mobileCampaign);

        if (expectedDefect == null) {
            MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                    .withMobileAppId(mobileAppId);
            checkCampaign(payload, expectedCampaign);
        } else {
            checkError(payload, expectedDefect.getCode(), "campaignUpdateItems[0].mobileAppId");
        }
    }

    @SuppressWarnings("unused")
    private static Object[] goalIdInCpiStrategyParameters() {
        return new Object[][]{
                {"Не передана цель в CPI стратегии", null, null},
                {"Передана цель 'Установки приложения'", TestGdAddCampaigns.DEFAULT_CPI_GOAL_ID, null},
                {"Передана цель 'Добавлено в корзину'", 38403071L, 38403071L},
        };
    }

    /**
     * Проверка обновления РМП при разных передаваемых целях в CPI стратегии
     */
    @Test
    @Parameters(method = "goalIdInCpiStrategyParameters")
    @TestCaseName("[{index}] {0}")
    public void updateCampaign_WithDifferentGoalIdInCpiStrategy(@SuppressWarnings("unused") String description,
                                                                @Nullable Long sendGoalId,
                                                                @Nullable Long expectedGoalId) {
        GdUpdateMobileContentCampaign mobileCampaign =
                TestGdUpdateCampaigns.defaultMobileContentCampaign(campaignId, mobileAppId)
                        .withBiddingStrategy(new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.SEARCH)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPI)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withAvgCpi(BigDecimal.valueOf(100L))
                                        .withGoalId(sendGoalId)));

        GdUpdateCampaignPayload payload = sendRequest(mobileCampaign);

        MobileContentCampaign expectedCampaign = new MobileContentCampaign()
                .withStrategy((DbStrategy) new DbStrategy()
                        .withPlatform(CampaignsPlatform.SEARCH)
                        .withAutobudget(CampaignsAutobudget.YES)
                        .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
                        .withStrategyData(new StrategyData()
                                .withName(CampaignsStrategyName.autobudget_avg_cpi.getLiteral())
                                .withAvgCpi(BigDecimal.valueOf(100L))
                                .withVersion(1L)
                                .withGoalId(expectedGoalId)
                                .withUnknownFields(emptyMap())));

        MobileContentCampaign campaign = checkCampaign(payload, expectedCampaign);
        assertThat(campaign.getStrategy().getStrategyData().getGoalId()).as("цель")
                .isEqualTo(expectedGoalId);
    }

    private MobileContentCampaign checkCampaign(GdUpdateCampaignPayload payload,
                                                MobileContentCampaign expectedCampaign) {
        checkState(payload.getValidationResult() == null && payload.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<Long> campaignIds = mapList(payload.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId);
        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard, campaignIds);
        MobileContentCampaign actualCampaign = (MobileContentCampaign) Iterables.getFirst(campaigns, null);
        checkNotNull(actualCampaign, "campaign not found");

        assertThat(actualCampaign).as("mobile content campaign")
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(onlyExpectedFields())));
        return (MobileContentCampaign) campaigns.get(0);
    }

    private void checkError(GdUpdateCampaignPayload payload,
                            String expectedDefectCode,
                            String path) {
        GdValidationResult vr = payload.getValidationResult();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload.getUpdatedCampaigns()).as("list of campaign data")
                    .isEqualTo(singletonList(null));
            soft.assertThat(vr).as("validation results")
                    .isNotNull();
            soft.assertThat(vr.getErrors().get(0)).as("validation error")
                    .isEqualTo(new GdDefect()
                            .withCode(expectedDefectCode)
                            .withPath(path));
        });
    }

    private GdUpdateCampaignPayload sendRequest(GdUpdateMobileContentCampaign campaign) {
        GdUpdateCampaignUnion gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withMobileContentCampaign(campaign);
        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(List.of(gdUpdateCampaignUnion));
        return processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);
    }
}
