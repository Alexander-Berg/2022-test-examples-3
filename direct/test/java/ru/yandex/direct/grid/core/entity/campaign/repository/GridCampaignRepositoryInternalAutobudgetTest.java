package ru.yandex.direct.grid.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.internalads.repository.PlaceRepository;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.repository.TestProductRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsCalcType;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsCurrency;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsType;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsUnit;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStatusBsSynced;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.strategyDataFromDb;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.timeTarget24x7;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@GridCoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GridCampaignRepositoryInternalAutobudgetTest {
    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private Steps steps;

    @Autowired
    private GridCampaignRepository gridCampaignRepository;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TestProductRepository testProductRepository;

    private ClientInfo clientInfo;
    public static final long PLACE_ID = TemplatePlaceRepositoryMockUtils.PLACE_1;
    private User operator;
    private Long campaignId;
    private InternalAutobudgetCampaign originalCampaign;

    @Before
    public void setUp() {
        // TODO оторвать, когда в production появится продукт + он доедет до products.data.sql
        testProductRepository.addProduct(510815L, "Внутренняя реклама. Автобюджетные заказы",
                ProductsType.internal_autobudget, ProductsCurrency.RUB, "Bucks", 67L, ProductsUnit.clicks,
                ProductsCalcType.cpc);

        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        UserInfo operatorInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN)
                        .getChiefUserInfo();
        operator = operatorInfo.getUser();

        originalCampaign = new InternalAutobudgetCampaign()
                .withType(CampaignType.INTERNAL_AUTOBUDGET)

                .withName("Campaign name")
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withSmsTime(new TimeInterval().withEndHour(12).withEndMinute(30).withStartHour(1).withStartMinute(15))
                .withSmsFlags(EnumSet.of(SmsFlag.MODERATE_RESULT_SMS, SmsFlag.CAMP_FINISHED_SMS))
                .withEmail("1@1.ru")
                .withWarningBalance(20)
                .withEnableSendAccountNews(false)
                .withEnablePausedByDayBudgetEvent(false)
                .withEnableOfflineStatNotice(false)
                .withEnableCheckPositionEvent(false)
                .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._15)
                .withTimeTarget(timeTarget24x7())
                .withTimeZoneId(130L)

                .withStrategy(TestCampaigns.defaultAutobudgetStrategy())
                .withMetrikaCounters(Collections.emptyList())
                .withMeaningfulGoals(Collections.emptyList())
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withIsMobile(false)
                .withPageId(Collections.emptyList())
                .withImpressionRateIntervalDays(1)
                .withImpressionRateCount(100)
                .withUid(clientInfo.getUid())
                .withCurrency(clientInfo.getClient().getWorkCurrency())
                .withStatusEmpty(false)
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.YES)
                .withStatusShow(true)
                .withStatusActive(true)
                .withOrderId(123L)
                .withStatusBsSynced(CampaignStatusBsSynced.YES)
                .withAutobudgetForecastDate(now().minusDays(1))
                .withLastChange(now())
                .withSum(BigDecimal.valueOf(100L))
                .withSumToPay(BigDecimal.valueOf(100L))
                .withSumLast(BigDecimal.valueOf(100L))
                .withSumSpent(BigDecimal.valueOf(100L))
                .withFio("fio " + RandomStringUtils.randomAlphabetic(5))

                .withPlaceId(PLACE_ID)
                .withAgencyId(null)
                .withWalletId(null)
                .withClientId(null)
                .withIsServiceRequested(null)
                .withHasTurboApp(false)
                .withPaidByCertificate(false);

        var operation = campaignOperationService.createRestrictedCampaignAddOperation(
                singletonList(originalCampaign), operator.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                new CampaignOptions());

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getSuccessfulCount()).isEqualTo(1);

        campaignId = result.get(0).getResult();
    }

    @Test
    public void fetchGdiCampaign() {
        List<GdiCampaign> allCampaigns = gridCampaignRepository.getAllCampaigns(
                UidClientIdShard.of(operator.getUid(), clientInfo.getClientId(),
                        clientInfo.getShard()));

        List<GdiCampaign> filteredCampaigns =
                filterList(allCampaigns, campaign -> campaign.getId().equals(campaignId));

        assertThat(filteredCampaigns).hasSize(1);
        var fetchedCampaign = filteredCampaigns.get(0);

        assertThat(fetchedCampaign)
                .hasFieldOrPropertyWithValue(GdiCampaign.ID.name(), campaignId)
                .hasFieldOrPropertyWithValue(GdiCampaign.TYPE.name(), originalCampaign.getType())
                .hasFieldOrPropertyWithValue(GdiCampaign.NAME.name(), originalCampaign.getName())
                .hasFieldOrPropertyWithValue(GdiCampaign.START_DATE.name(), originalCampaign.getStartDate())
                .hasFieldOrPropertyWithValue(GdiCampaign.FINISH_DATE.name(), originalCampaign.getEndDate())
                .hasFieldOrPropertyWithValue(GdiCampaign.SMS_TIME.name(), originalCampaign.getSmsTime())
                .hasFieldOrPropertyWithValue(GdiCampaign.SMS_FLAGS.name(), originalCampaign.getSmsFlags())
                .hasFieldOrPropertyWithValue(GdiCampaign.EMAIL.name(), originalCampaign.getEmail())
                .hasFieldOrPropertyWithValue(GdiCampaign.WARNING_BALANCE.name(), originalCampaign.getWarningBalance())
                .hasFieldOrPropertyWithValue(GdiCampaign.ENABLE_SEND_ACCOUNT_NEWS.name(),
                        originalCampaign.getEnableSendAccountNews())
                .hasFieldOrPropertyWithValue(GdiCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT.name(),
                        originalCampaign.getEnablePausedByDayBudgetEvent())
                .hasFieldOrPropertyWithValue(GdiCampaign.ENABLE_OFFLINE_STAT_NOTICE.name(),
                        originalCampaign.getEnableOfflineStatNotice())
                .hasFieldOrPropertyWithValue(GdiCampaign.ENABLE_CHECK_POSITION_EVENT.name(),
                        originalCampaign.getEnableCheckPositionEvent())
                .hasFieldOrPropertyWithValue(GdiCampaign.CHECK_POSITION_INTERVAL.name(),
                        originalCampaign.getCheckPositionIntervalEvent())
                .hasFieldOrPropertyWithValue(GdiCampaign.TIME_TARGET.name(), originalCampaign.getTimeTarget())
                .hasFieldOrPropertyWithValue(GdiCampaign.TIMEZONE_ID.name(), originalCampaign.getTimeZoneId())
                .hasFieldOrPropertyWithValue(GdiCampaign.STRATEGY_NAME.name(), GdiCampaignStrategyName.fromSource(
                        StrategyName.toSource(originalCampaign.getStrategy().getStrategyName())))
                .hasFieldOrPropertyWithValue(GdiCampaign.METRIKA_COUNTERS.name(), null)
                .hasFieldOrPropertyWithValue(GdiCampaign.MEANINGFUL_GOALS.name(), null)
                .hasFieldOrPropertyWithValue(GdiCampaign.ATTRIBUTION_MODEL.name(),
                        originalCampaign.getAttributionModel())
                .hasFieldOrPropertyWithValue(GdiCampaign.INTERNAL_AD_PLACE_ID.name(), originalCampaign.getPlaceId())
                .hasFieldOrPropertyWithValue(GdiCampaign.INTERNAL_AD_IS_MOBILE.name(), originalCampaign.getIsMobile())
                .hasFieldOrPropertyWithValue(GdiCampaign.INTERNAL_AD_PAGE_ID.name(), originalCampaign.getPageId())
                .hasFieldOrPropertyWithValue(GdiCampaign.IMPRESSION_RATE_INTERVAL_DAYS.name(),
                        originalCampaign.getImpressionRateIntervalDays())
                .hasFieldOrPropertyWithValue(GdiCampaign.IMPRESSION_RATE_COUNT.name(),
                        originalCampaign.getImpressionRateCount())
                .hasFieldOrPropertyWithValue(GdiCampaign.USER_ID.name(), originalCampaign.getUid())
                .hasFieldOrPropertyWithValue(GdiCampaign.CURRENCY_CODE.name(), originalCampaign.getCurrency())
                .hasFieldOrPropertyWithValue(GdiCampaign.EMPTY.name(), false)
                .hasFieldOrPropertyWithValue(GdiCampaign.STATUS_MODERATE.name(), originalCampaign.getStatusModerate())
                .hasFieldOrPropertyWithValue(GdiCampaign.STATUS_POST_MODERATE.name(),
                        originalCampaign.getStatusPostModerate())
                .hasFieldOrPropertyWithValue(GdiCampaign.SHOWING.name(), originalCampaign.getStatusShow())
                .hasFieldOrPropertyWithValue(GdiCampaign.ACTIVE.name(), originalCampaign.getStatusActive())
                .hasFieldOrPropertyWithValue(GdiCampaign.ORDER_ID.name(), originalCampaign.getOrderId())
                .hasFieldOrPropertyWithValue(GdiCampaign.STATUS_BS_SYNCED.name(), GdiCampaignStatusBsSynced.fromSource(
                        CampaignStatusBsSynced.toSource(originalCampaign.getStatusBsSynced())))
                .hasFieldOrPropertyWithValue(GdiCampaign.SUM.name(),
                        originalCampaign.getSum().setScale(fetchedCampaign.getSum().scale(), RoundingMode.UNNECESSARY))
                .hasFieldOrPropertyWithValue(GdiCampaign.SUM_TO_PAY.name(),
                        originalCampaign.getSumToPay().setScale(fetchedCampaign.getSumToPay().scale(),
                                RoundingMode.UNNECESSARY))
                .hasFieldOrPropertyWithValue(GdiCampaign.SUM_LAST.name(),
                        originalCampaign.getSumLast().setScale(fetchedCampaign.getSumLast().scale(),
                                RoundingMode.UNNECESSARY))
                .hasFieldOrPropertyWithValue(GdiCampaign.SUM_SPENT.name(),
                        originalCampaign.getSumSpent().setScale(fetchedCampaign.getSumSpent().scale(),
                                RoundingMode.UNNECESSARY))
                .hasFieldOrPropertyWithValue(GdiCampaign.AGENCY_ID.name(), originalCampaign.getAgencyId())
                .hasFieldOrPropertyWithValue(GdiCampaign.WALLET_ID.name(), originalCampaign.getWalletId())
                .hasFieldOrPropertyWithValue(GdiCampaign.CLIENT_ID.name(), originalCampaign.getClientId());

        StrategyData fetchedStrategyData = strategyDataFromDb(fetchedCampaign.getStrategyData());
        assertThat(fetchedStrategyData)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualTo(originalCampaign.getStrategy().getStrategyData());
    }

}
