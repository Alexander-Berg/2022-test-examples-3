package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.leangen.graphql.annotations.GraphQLNonNull;
import jdk.jfr.Description;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.SimpleTypeDiffer;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCpmYndxFrontpageCampaignShowType;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignCheckPositionInterval;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;
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
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCpmYndxFrontpageCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.libs.timetarget.TimeTargetUtils;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.FIRST_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toSmsFlags;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

@GridProcessingTest
@RunWith(Parameterized.class)
public class CampaignMutationGraphqlServiceCpmYndxFrontpageCampaignTest {

    private static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath(CommonCampaign.CREATE_TIME.name()), newPath(CommonCampaign.SOURCE.name()),
                    newPath(CommonCampaign.METATYPE.name()))
            .forFields(newPath(CommonCampaign.LAST_CHANGE.name())).useMatcher(approximatelyNow())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer())
            .forClasses(TimeTarget.class).useDiffer(new SimpleTypeDiffer());

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
    private static final TimeTarget OLD_TIME_TARGET = TimeTargetUtils.timeTarget24x7();
    private static final TimeTarget NEW_TIME_TARGET = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX" +
            "2ABCDEFGHIJKLMNOPQRSTUVWX" +
            "3ABCDEFGHIJKLMNOPQRSTUVWX" +
            "4ABCDE5ABCDE6A7A");

    private static final List<@GraphQLNonNull String> OLD_DISABLED_IPS = ImmutableList.of("1.2.3.4");
    private static final List<@GraphQLNonNull String> NEW_DISABLED_IPS = ImmutableList.of("5.6.7.8");

    private static final Boolean OLD_HAS_SITE_MONITORING = false;
    private static final Boolean NEW_HAS_SITE_MONITORING = true;

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

    private static final GdCampaignCheckPositionInterval OLD_CHECK_POSITION_INTERVAL =
            GdCampaignCheckPositionInterval.M_30;
    // Если поставить NEW_CHECK_POSITION_INTERVAL = null - тест почему-то упадёт, возможно не работает сброс на null?
    private static final GdCampaignCheckPositionInterval NEW_CHECK_POSITION_INTERVAL =
            GdCampaignCheckPositionInterval.M_15;

    private static final Set<GdCampaignSmsEvent> OLD_ENABLE_EVENTS = ImmutableSet.of(GdCampaignSmsEvent.FINISHED);
    private static final Set<GdCampaignSmsEvent> NEW_ENABLE_EVENTS = ImmutableSet.of(GdCampaignSmsEvent.MONEY_IN);

    private static final Set<FrontpageCampaignShowType> OLD_SHOW_TYPES =
            ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE);
    private static final Set<FrontpageCampaignShowType> NEW_SHOW_TYPES =
            ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE);

    private static final BigDecimal OLD_AVG_CPM = BigDecimal.valueOf(1.4);
    private static final BigDecimal NEW_AVG_CPM = BigDecimal.valueOf(10);

    private static final CampaignAttributionModel OLD_ATTRIBUTION_MODEL = LAST_CLICK;
    private static final CampaignAttributionModel NEW_ATTRIBUTION_MODEL = FIRST_CLICK;

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static LocalDateTime now = LocalDateTime.now();

    private User operator;
    private ClientInfo clientInfo;
    private CpmYndxFrontpageCampaign yndxFrontpageCampaign;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return Arrays.asList(new Object[][]{
                {"defaultCpmStrategy",
                        defaultCpmStrategy()},
                {"autoBudgetMaxReachStrategy",
                        defaultAutobudgetMaxReachDbStrategy()},
                {"autoBudgetMaxImpressionsStrategy",
                        defaultAutobudgetMaxImpressionsDbStrategy()},
                {"autoBudgetMaxReachCustomPeriodStrategy",
                        defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.of(NEW_START_DATE,
                                LocalTime.now()))},
                {"autoBudgetMaxImpressionsCustomPeriodStrategy",
                        defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.of(NEW_START_DATE,
                                LocalTime.now()))},
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.CHF));
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);

        yndxFrontpageCampaign = defaultCpmYndxFrontpageCampaign(clientInfo)
                .withName(OLD_NAME)
                .withStartDate(OLD_START_DATE)
                .withAllowedFrontpageType(OLD_SHOW_TYPES)
                .withEndDate(OLD_END_DATE)
                .withTimeZoneId(OLD_TIME_ZONE)
                .withAttributionModel(OLD_ATTRIBUTION_MODEL)
                .withSmsTime(OLD_SMS_TIME)
                .withSmsFlags(toSmsFlags(OLD_ENABLE_EVENTS))
                .withEmail(OLD_EMAIL)
                .withWarningBalance(OLD_WARNING_BALANCE)
                .withEnablePausedByDayBudgetEvent(OLD_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withEnableOfflineStatNotice(OLD_ENABLE_OFFLINE_STAT_NOTICE)
                .withTimeTarget(OLD_TIME_TARGET)
                .withTimeZoneId(OLD_TIME_ZONE)
                .withDisabledIps(OLD_DISABLED_IPS)
                .withEnableSendAccountNews(OLD_SEND_ACCOUNT_NEWS)
                .withHasSiteMonitoring(OLD_HAS_SITE_MONITORING)
                .withHasAddMetrikaTagToUrl(OLD_HAS_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(OLD_HAS_ADD_OPENSTAT_TAG_TO_URL);
        dbStrategy.getStrategyData().withAvgCpm(OLD_AVG_CPM);
        yndxFrontpageCampaign
                .withClientId(clientInfo.getClientId().asLong())
                .withUid(clientInfo.getUid())
                .withStrategy(dbStrategy);

        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                clientInfo.getUid());
        campaignModifyRepository.addCampaigns(ppcDslContextProvider.ppc(clientInfo.getShard()),
                addCampaignParametersContainer, List.of(yndxFrontpageCampaign));

        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                clientInfo.getShard(),
                yndxFrontpageCampaign.getId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));

        var adGroup = activeCpmYndxFrontpageAdGroup(yndxFrontpageCampaign.getId())
                .withGeo(List.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID));
        steps.adGroupSteps().saveAdGroup(adGroup, clientInfo);
    }

    @Test
    @Description("Проверяем изменение всех полей, кроме ContentLanguage, для него оператор должен быть супером")
    public void update_CpmYndxFrontpageCampaign() {

        GdUpdateCampaignPayload gdUpdateCampaignPayload = updateCampaign(NEW_SHOW_TYPES, NEW_AVG_CPM);

        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(yndxFrontpageCampaign.getId()));

        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns())
                .is(matchedBy(beanDiffer(expectedUpdatedCampaigns)
                        .useCompareStrategy(onlyExpectedFields())));
        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();

        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                        mapList(gdUpdateCampaignPayload.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId));

        assertThat(campaigns).hasSize(1);
        var campaign = (CpmYndxFrontpageCampaign) campaigns.get(0);

        var expectedCampaign = defaultCpmYndxFrontpageCampaign(clientInfo)
                .withId(yndxFrontpageCampaign.getId())
                .withOrderId(yndxFrontpageCampaign.getOrderId())
                .withName(NEW_NAME)
                .withAllowedFrontpageType(NEW_SHOW_TYPES)
                .withStartDate(NEW_START_DATE)
                .withEndDate(NEW_END_DATE)
                .withAttributionModel(NEW_ATTRIBUTION_MODEL)
                .withHasSiteMonitoring(NEW_HAS_SITE_MONITORING)
                .withHasAddMetrikaTagToUrl(NEW_HAS_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(NEW_HAS_ADD_OPENSTAT_TAG_TO_URL)
                .withSmsTime(NEW_SMS_TIME)
                .withSmsFlags(toSmsFlags(NEW_ENABLE_EVENTS))
                .withEmail(NEW_EMAIL)
                .withWarningBalance(NEW_WARNING_BALANCE)
                .withEnableSendAccountNews(NEW_SEND_ACCOUNT_NEWS)
                .withEnableOfflineStatNotice(NEW_ENABLE_OFFLINE_STAT_NOTICE)
                .withEnablePausedByDayBudgetEvent(NEW_ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .withDisabledIps(NEW_DISABLED_IPS)
                .withTimeTarget(NEW_TIME_TARGET)
                .withTimeZoneId(NEW_TIME_ZONE)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusArchived(false)
                .withStrategyId(campaign.getStrategyId())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withPlatform(CampaignsPlatform.CONTEXT)
                        .withStrategyName(dbStrategy.getStrategyName())
                        .withAutobudget(dbStrategy.getAutobudget())
                        .withStrategyData(dbStrategy.getStrategyData()
                                .withVersion(1L)
                                .withAutoProlongation(dbStrategy.getStrategyName() == AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD ||
                                        dbStrategy.getStrategyName() == AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD ?
                                        0L : null)
                                .withAvgCpm(NEW_AVG_CPM)
                                .withDailyChangeCount(null)
                                .withLastUpdateTime(null)
                        ));

        assertThat(campaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY
                .forFields(newPath("lastChange")).useMatcher(approximatelyNow()))));
    }

    @Test
    public void update_CpmYndxFrontpageCampaign_ChangeAvgBid_LessThanMin() {

        // меняем ставку на ту, которая меньше минимальной для desktop и меньше минимальной для mobile
        BigDecimal newAvgCpm = BigDecimal.valueOf(1.1);
        // оставляем те же площадки, что и раньше
        GdUpdateCampaignPayload gdUpdateCampaignPayload = updateCampaign(OLD_SHOW_TYPES, newAvgCpm);

        GdValidationResult vr = gdUpdateCampaignPayload.getValidationResult();
        if (dbStrategy.getStrategyName() == StrategyName.CPM_DEFAULT) {
            assertThat(vr).isNull();
        } else {
            assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).isEqualTo(singletonList(null));
            assertThat(vr).isNotNull();
            GdDefect gdDefect = vr.getErrors().get(0);
            assertThat(gdDefect.getCode()).isEqualTo(CPM_PRICE_IS_NOT_GREATER_THAN_MIN.getCode());
            assertThat(gdDefect.getPath()).isEqualTo("campaignUpdateItems[0].strategy.strategyData.avgCpm");
        }
    }

    @Test
    public void update_CpmYndxFrontpageCampaign_ChangeShowType() {

        // оставляем только desktop площадку, чтобы увеличить минимальную ставку
        Set<FrontpageCampaignShowType> newAllowedFrontpageType = Set.of(FrontpageCampaignShowType.FRONTPAGE);
        // оставляем ту же ставку, что и раньше
        GdUpdateCampaignPayload gdUpdateCampaignPayload = updateCampaign(newAllowedFrontpageType, OLD_AVG_CPM);

        GdValidationResult vr = gdUpdateCampaignPayload.getValidationResult();
        if (dbStrategy.getStrategyName() == StrategyName.CPM_DEFAULT) {
            assertThat(vr).isNull();
        } else {
            assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).isEqualTo(singletonList(null));
            assertThat(vr).isNotNull();
            GdDefect gdDefect = vr.getErrors().get(0);
            assertThat(gdDefect.getCode()).isEqualTo(CPM_PRICE_IS_NOT_GREATER_THAN_MIN.getCode());
            assertThat(gdDefect.getPath()).isEqualTo("campaignUpdateItems[0].strategy.strategyData.avgCpm");
        }
    }

    private GdUpdateCampaignPayload updateCampaign(Set<FrontpageCampaignShowType> frontpageCampaignShowTypes,
                                                   BigDecimal avgCpm) {
        StrategyData strategyData = dbStrategy.getStrategyData();
        var gdUpdateCpmYndxFrontpageCampaign = new GdUpdateCpmYndxFrontpageCampaign()
                .withId(yndxFrontpageCampaign.getId())
                .withName(NEW_NAME)
                .withAllowedFrontpageType(mapSet(frontpageCampaignShowTypes,
                        t -> GdCpmYndxFrontpageCampaignShowType.valueOf(t.name())))
                .withEndDate(NEW_END_DATE)
                .withStartDate(NEW_START_DATE)
                .withTimeTarget(CampaignDataConverter.toGdTimeTarget(NEW_TIME_TARGET).withIdTimeZone(NEW_TIME_ZONE))
                .withDisabledIps(NEW_DISABLED_IPS)
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
                .withHasSiteMonitoring(NEW_HAS_SITE_MONITORING)
                .withHasAddMetrikaTagToUrl(NEW_HAS_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(NEW_HAS_ADD_OPENSTAT_TAG_TO_URL)
                .withStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategy(GdCampaignStrategy.DIFFERENT_PLACES)
                        .withStrategyName(GdCampaignStrategyName.valueOf(dbStrategy.getStrategyName().name()))
                        .withStrategyData(new GdCampaignStrategyData()
                                .withStartDate(strategyData.getStart())
                                .withFinishDate(strategyData.getFinish())
                                .withSum(strategyData.getSum() != null ? strategyData.getSum() :
                                        strategyData.getBudget())
                        ));
        if (avgCpm != null) {
            gdUpdateCpmYndxFrontpageCampaign.getStrategy().getStrategyData().withAvgCpm(avgCpm);
        }
        GdUpdateCampaignUnion campaignUnion = new GdUpdateCampaignUnion()
                .withCpmYndxFrontpageCampaign(gdUpdateCpmYndxFrontpageCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));
        return processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator);
    }

}
