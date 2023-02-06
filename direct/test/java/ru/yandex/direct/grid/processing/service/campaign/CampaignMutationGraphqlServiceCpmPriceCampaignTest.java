package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.leangen.graphql.annotations.GraphQLNonNull;
import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignCheckPositionInterval;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCpmPriceCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.FIRST_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_CLICK;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.getStrategy;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.feature.FeatureName.CAMPAIGN_ALLOWED_ON_ADULT_CONTENT;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_CAMPAIGN;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toSmsFlags;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphqlServiceCpmPriceCampaignTest {

    private static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath(CommonCampaign.CREATE_TIME.name()), newPath(CommonCampaign.SOURCE.name()),
                    newPath(CommonCampaign.METATYPE.name()))
            .forFields(newPath(CommonCampaign.LAST_CHANGE.name())).useMatcher(approximatelyNow())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());

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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload>
            UPDATE_CAMPAIGN_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
            GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    private static final String OLD_NAME = "old name";
    private static final String NEW_NAME = "new name";

    private static final LocalDate OLD_START_DATE = LocalDate.now().plusYears(1);
    private static final LocalDate NEW_START_DATE = LocalDate.now().plusYears(1).plusMonths(1);

    private static final LocalDate OLD_END_DATE = LocalDate.now().plusYears(2);
    private static final LocalDate NEW_END_DATE = LocalDate.now().plusYears(2).minusMonths(1);

    private static final long MOSCOW_TIMEZONE_ID = 130L;
    private static final long KALININGRAD_TIMEZONE_ID = 131L;
    private static final Long OLD_TIME_ZONE = MOSCOW_TIMEZONE_ID;
    private static final Long NEW_TIME_ZONE = KALININGRAD_TIMEZONE_ID;

    private static final List<@GraphQLNonNull String> OLD_DISABLED_IPS = ImmutableList.of("1.2.3.4");
    private static final List<@GraphQLNonNull String> NEW_DISABLED_IPS = ImmutableList.of("5.6.7.8");

    private static final Boolean OLD_HAS_ADD_METRIKA_TAG_TO_URL = false;
    private static final Boolean NEW_HAS_ADD_METRIKA_TAG_TO_URL = true;

    private static final Boolean OLD_HAS_ADD_OPENSTAT_TAG_TO_URL = true;
    private static final Boolean NEW_HAS_ADD_OPENSTAT_TAG_TO_URL = false;

    private static final TimeInterval OLD_SMS_TIME = new TimeInterval()
            .withStartHour(10).withStartMinute(0)
            .withEndHour(11).withEndMinute(0);
    private static final TimeInterval NEW_SMS_TIME = new TimeInterval()
            .withStartHour(3).withStartMinute(0)
            .withEndHour(4).withEndMinute(0);

    private static final String OLD_EMAIL = "old@email.com";
    private static final String NEW_EMAIL = "new@email.com";

    private static final Integer OLD_WARNING_BALANCE = CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE;
    private static final Integer NEW_WARNING_BALANCE = CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE + 1;

    private static final Boolean OLD_SEND_ACCOUNT_NEWS = false;
    private static final Boolean NEW_SEND_ACCOUNT_NEWS = true;

    private static final Boolean OLD_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT = true;
    private static final Boolean NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT = false;

    private static final Boolean OLD_ENABLE_OFFLINE_STAT_NOTICE = false;
    private static final Boolean NEW_ENABLE_OFFLINE_STAT_NOTICE = true;

    private static final Long ORDER_VOLUME_MIN = 50L;
    private static final Long ORDER_VOLUME_MAX = 300L;
    private static final Long OLD_FLIGHT_ORDER_VOLUME = 200L;
    private static final Long NEW_FLIGHT_ORDER_VOLUME = 100L;

    private static final GdCampaignCheckPositionInterval OLD_CHECK_POSITION_INTERVAL =
            GdCampaignCheckPositionInterval.M_30;
    // Если поставить NEW_CHECK_POSITION_INTERVAL = null - тест почему-то упадёт, возможно не работает сброс на null?
    private static final GdCampaignCheckPositionInterval NEW_CHECK_POSITION_INTERVAL =
            GdCampaignCheckPositionInterval.M_15;

    private static final Set<GdCampaignSmsEvent> OLD_ENABLE_EVENTS = ImmutableSet.of(GdCampaignSmsEvent.FINISHED);
    private static final Set<GdCampaignSmsEvent> NEW_ENABLE_EVENTS = ImmutableSet.of(GdCampaignSmsEvent.MONEY_IN);

    private static final CampaignAttributionModel OLD_ATTRIBUTION_MODEL = LAST_CLICK;
    private static final CampaignAttributionModel NEW_ATTRIBUTION_MODEL = FIRST_CLICK;

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    private User operator;
    private ClientInfo clientInfo;
    private PricePackage pricePackage;
    private CpmPriceCampaign priceCampaign;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CPM_PRICE_CAMPAIGN, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CAMPAIGN_ALLOWED_ON_ADULT_CONTENT, true);

        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);

        pricePackage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                        .withCurrency(CurrencyCode.RUB)
                        .withPrice(BigDecimal.valueOf(0.99))
                        .withOrderVolumeMin(ORDER_VOLUME_MIN)
                        .withOrderVolumeMax(ORDER_VOLUME_MAX)
                        .withDateStart(OLD_START_DATE)
                        .withDateEnd(OLD_END_DATE)
                        .withClients(List.of(allowedPricePackageClient(clientInfo))))
                .getPricePackage();

        priceCampaign = defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withName(OLD_NAME)
                .withStartDate(OLD_START_DATE)
                .withEndDate(OLD_END_DATE)
                .withTimeZoneId(OLD_TIME_ZONE)
                .withAttributionModel(OLD_ATTRIBUTION_MODEL)
                .withSmsTime(OLD_SMS_TIME)
                .withSmsFlags(toSmsFlags(OLD_ENABLE_EVENTS))
                .withEmail(OLD_EMAIL)
                .withWarningBalance(OLD_WARNING_BALANCE)
                .withEnablePausedByDayBudgetEvent(OLD_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withEnableOfflineStatNotice(OLD_ENABLE_OFFLINE_STAT_NOTICE)
                .withFlightOrderVolume(OLD_FLIGHT_ORDER_VOLUME)
                .withTimeZoneId(OLD_TIME_ZONE)
                .withDisabledIps(OLD_DISABLED_IPS)
                .withEnableSendAccountNews(OLD_SEND_ACCOUNT_NEWS)
                .withEnableCheckPositionEvent(OLD_CHECK_POSITION_INTERVAL != null)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(OLD_CHECK_POSITION_INTERVAL))
                .withHasAddMetrikaTagToUrl(OLD_HAS_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(OLD_HAS_ADD_OPENSTAT_TAG_TO_URL)
                // проставляем StatusApprove = New, чтобы все поля были "редактируемыми"
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW);
        priceCampaign.withStrategy(getStrategy(priceCampaign, pricePackage, false));

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    @Description("Проверяем изменение всех полей, кроме ContentLanguage, для него оператор должен быть супером")
    public void update_CpmPriceCampaign() {
        createPriceCampaign(priceCampaign);
        GdUpdateCampaignUnion campaignUnion = new GdUpdateCampaignUnion().withCpmPriceCampaign(
                new GdUpdateCpmPriceCampaign()
                        .withId(priceCampaign.getId())
                        .withName(NEW_NAME)
                        .withEndDate(NEW_END_DATE)
                        .withStartDate(NEW_START_DATE)
                        .withTimeTarget(new GdTimeTarget().withIdTimeZone(NEW_TIME_ZONE))
                        .withDisabledIps(NEW_DISABLED_IPS)
                        .withAutoProlongation(true)//для кампаний на главной должно игнорироваться
                        .withNotification(new GdCampaignNotificationRequest()
                                .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                        .withEmail(NEW_EMAIL)
                                        .withCheckPositionInterval(NEW_CHECK_POSITION_INTERVAL)
                                        .withSendAccountNews(NEW_SEND_ACCOUNT_NEWS)
                                        .withStopByReachDailyBudget(NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                                        .withXlsReady(NEW_ENABLE_OFFLINE_STAT_NOTICE)
                                        .withWarningBalance(NEW_WARNING_BALANCE)
                                )
                                .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                        .withSmsTime(toGdTimeInterval(NEW_SMS_TIME))
                                        .withEnableEvents(NEW_ENABLE_EVENTS)))
                        .withAttributionModel(toGdAttributionModel(NEW_ATTRIBUTION_MODEL))
                        .withHasAddMetrikaTagToUrl(NEW_HAS_ADD_METRIKA_TAG_TO_URL)
                        .withHasAddOpenstatTagToUrl(NEW_HAS_ADD_OPENSTAT_TAG_TO_URL)
                        .withFlightOrderVolume(NEW_FLIGHT_ORDER_VOLUME)
                        .withIsAllowedOnAdultContent(true)
        );

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(priceCampaign.getId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();

        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                        mapList(gdUpdateCampaignPayload.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId));

        assertThat(campaigns).hasSize(1);
        var campaign = (CpmPriceCampaign) campaigns.get(0);

        var expectedCampaign = defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withId(priceCampaign.getId())
                .withName(NEW_NAME)
                .withStartDate(NEW_START_DATE)
                .withEndDate(NEW_END_DATE)
                .withAttributionModel(NEW_ATTRIBUTION_MODEL)
                .withHasAddMetrikaTagToUrl(NEW_HAS_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(NEW_HAS_ADD_OPENSTAT_TAG_TO_URL)
                .withSmsTime(NEW_SMS_TIME)
                .withSmsFlags(toSmsFlags(NEW_ENABLE_EVENTS))
                .withEmail(NEW_EMAIL)
                .withWarningBalance(NEW_WARNING_BALANCE)
                .withEnableSendAccountNews(NEW_SEND_ACCOUNT_NEWS)
                .withEnableOfflineStatNotice(NEW_ENABLE_OFFLINE_STAT_NOTICE)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(NEW_CHECK_POSITION_INTERVAL))
                .withEnableCheckPositionEvent(NEW_CHECK_POSITION_INTERVAL != null)
                .withEnablePausedByDayBudgetEvent(NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withFlightOrderVolume(NEW_FLIGHT_ORDER_VOLUME)
                .withDisabledIps(NEW_DISABLED_IPS)
                .withTimeZoneId(NEW_TIME_ZONE)
                // StatusBsSynced должен сбросится
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusArchived(false)
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withDisabledSsp(emptyList())
                .withStrategyId(campaign.getStrategyId())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData()
                                .withName("period_fix_bid")
                                .withVersion(1L)
                                .withAutoProlongation(0L)
                                .withStart(NEW_START_DATE)
                                .withFinish(NEW_END_DATE)
                                .withBudget(BigDecimal.valueOf(0.1)))
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withPlatform(CampaignsPlatform.CONTEXT))
                .withIsAllowedOnAdultContent(true);

        assertThat(campaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    @Description("У заапрувленной кампании у клиента должна сохранится возможность менять имя.")
    public void update_CanChangeNameOfApprovedCampaign() {
        createPriceCampaign(priceCampaign
                .withFlightStatusApprove(PriceFlightStatusApprove.YES));
        GdUpdateCampaignUnion campaignUnion = new GdUpdateCampaignUnion().withCpmPriceCampaign(
                new GdUpdateCpmPriceCampaign()
                        .withId(priceCampaign.getId())
                        .withName(NEW_NAME)
                        .withStartDate(priceCampaign.getStartDate())
                        .withEndDate(priceCampaign.getEndDate())
                        .withTimeTarget(new GdTimeTarget().withIdTimeZone(NEW_TIME_ZONE))
                        .withNotification(new GdCampaignNotificationRequest()
                                .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                        .withEmail(NEW_EMAIL)
                                        .withCheckPositionInterval(NEW_CHECK_POSITION_INTERVAL)
                                        .withSendAccountNews(NEW_SEND_ACCOUNT_NEWS)
                                        .withStopByReachDailyBudget(NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                                        .withXlsReady(NEW_ENABLE_OFFLINE_STAT_NOTICE)
                                        .withWarningBalance(NEW_WARNING_BALANCE)
                                )
                                .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                        .withSmsTime(toGdTimeInterval(NEW_SMS_TIME))
                                        .withEnableEvents(NEW_ENABLE_EVENTS)))
                        .withAttributionModel(toGdAttributionModel(priceCampaign.getAttributionModel()))
                        .withHasAddMetrikaTagToUrl(priceCampaign.getHasAddMetrikaTagToUrl())
                        .withHasAddOpenstatTagToUrl(priceCampaign.getHasAddOpenstatTagToUrl())
                        .withFlightOrderVolume(priceCampaign.getFlightOrderVolume())
        );

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(priceCampaign.getId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();

        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                        mapList(gdUpdateCampaignPayload.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId));

        assertThat(campaigns).hasSize(1);
        var campaign = (CpmPriceCampaign) campaigns.get(0);

        var expectedCampaign = defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withId(priceCampaign.getId())
                .withName(NEW_NAME)
                .withEmail(NEW_EMAIL)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(NEW_CHECK_POSITION_INTERVAL))
                .withEnableCheckPositionEvent(NEW_CHECK_POSITION_INTERVAL != null)
                .withEnableSendAccountNews(NEW_SEND_ACCOUNT_NEWS)
                .withEnablePausedByDayBudgetEvent(NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withEnableOfflineStatNotice(NEW_ENABLE_OFFLINE_STAT_NOTICE)
                .withWarningBalance(NEW_WARNING_BALANCE)
                .withSmsTime(NEW_SMS_TIME)
                .withSmsFlags(toSmsFlags(NEW_ENABLE_EVENTS))
                .withTimeZoneId(NEW_TIME_ZONE)
                // StatusBsSynced должен сбросится
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusArchived(false)
                .withHasAddOpenstatTagToUrl(priceCampaign.getHasAddOpenstatTagToUrl())
                .withAttributionModel(priceCampaign.getAttributionModel())
                .withHasAddMetrikaTagToUrl(priceCampaign.getHasAddMetrikaTagToUrl())
                .withFlightOrderVolume(priceCampaign.getFlightOrderVolume())
                .withDisabledSsp(emptyList())
                .withStrategyId(campaign.getStrategyId())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData()
                                .withName("period_fix_bid")
                                .withVersion(1L)
                                .withAutoProlongation(0L)
                                .withStart(OLD_START_DATE)
                                .withFinish(OLD_END_DATE)
                                .withBudget(BigDecimal.valueOf(0.2)))
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withPlatform(CampaignsPlatform.CONTEXT));

        assertThat(campaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }


    @Test
    @Description("Проверяем изменение всех полей, кроме ContentLanguage, для него оператор должен быть супером")
    public void update_CpmPriceCampaign_WithDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        createPriceCampaign(priceCampaign);
        GdUpdateCampaignUnion campaignUnion = new GdUpdateCampaignUnion().withCpmPriceCampaign(
                new GdUpdateCpmPriceCampaign()
                        .withId(priceCampaign.getId())
                        .withName(NEW_NAME)
                        .withEndDate(NEW_END_DATE)
                        .withStartDate(NEW_START_DATE)
                        .withTimeTarget(new GdTimeTarget().withIdTimeZone(NEW_TIME_ZONE))
                        .withDisabledIps(NEW_DISABLED_IPS)
                        .withAutoProlongation(true)
                        .withNotification(new GdCampaignNotificationRequest()
                                .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                        .withEmail(NEW_EMAIL)
                                        .withCheckPositionInterval(NEW_CHECK_POSITION_INTERVAL)
                                        .withSendAccountNews(NEW_SEND_ACCOUNT_NEWS)
                                        .withStopByReachDailyBudget(NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                                        .withXlsReady(NEW_ENABLE_OFFLINE_STAT_NOTICE)
                                        .withWarningBalance(NEW_WARNING_BALANCE)
                                )
                                .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                        .withSmsTime(toGdTimeInterval(NEW_SMS_TIME))
                                        .withEnableEvents(NEW_ENABLE_EVENTS)))
                        .withAttributionModel(toGdAttributionModel(NEW_ATTRIBUTION_MODEL))
                        .withHasAddMetrikaTagToUrl(NEW_HAS_ADD_METRIKA_TAG_TO_URL)
                        .withHasAddOpenstatTagToUrl(NEW_HAS_ADD_OPENSTAT_TAG_TO_URL)
                        .withFlightOrderVolume(NEW_FLIGHT_ORDER_VOLUME)
                        .withIsAllowedOnAdultContent(true)
        );

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input,
                operator);

        GdDefect defect = new GdDefect()
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS")
                .withPath("campaignUpdateItems[0].id");
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNotNull();
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }

    private void createPriceCampaign(CampaignWithPricePackage priceCampaign) {
        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid());
        campaignModifyRepository.addCampaigns(ppcDslContextProvider.ppc(clientInfo.getShard()),
                addCampaignParametersContainer, List.of(priceCampaign));
    }

}
