package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.TimeTargetStatus;
import ru.yandex.direct.core.entity.campaign.model.TimeTargetStatusInfo;
import ru.yandex.direct.core.entity.campaign.service.TimeTargetStatusService;
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.service.GeoTimezoneMappingService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatusDesc;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatusModerate;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignDelayedOperation;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStatusBsSynced;
import ru.yandex.direct.grid.processing.model.campaign.GdWallet;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DEFAULT_CAMPAIGN_MONEY;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;

@RunWith(Parameterized.class)
public class GridCampaignWithWalletAggregationFieldsServiceStatusTest {
    // Московская таймзона для удобства написания тестов
    private static final GeoTimezone MSK_TIMEZONE = new GeoTimezone()
            .withRegionId(225L)
            .withTimezoneId(130L)
            .withTimezone(MSK);
    private static final TimeTargetStatusInfo STATUS_INFO = new TimeTargetStatusInfo()
            .withStatus(TimeTargetStatus.ACTIVE);
    private static final LocalDate TEST_DATE = LocalDate.parse("2017-12-27");
    private static final Instant TEST_INSTANT = TEST_DATE.atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Long CAMPAIGN_ID = 121L;

    @Mock
    private TimeTargetStatusService timeTargetStatusService;

    @Mock
    private GeoTimezoneMappingService geoTimezoneMappingService;

    @InjectMocks
    private GridCampaignAggregationFieldsService service;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(MSK_TIMEZONE)
                .when(geoTimezoneMappingService).getRegionIdByTimezoneId(anyLong());
        doReturn(STATUS_INFO)
                .when(timeTargetStatusService).getTimeTargetStatus(any(), any(), any());
    }

    @Parameterized.Parameter
    public GdiCampaign campaign;

    @Parameterized.Parameter(1)
    public GdWallet wallet;

    @Parameterized.Parameter(2)
    public GdCampaignStatus expectedStatus;

    @Parameterized.Parameter(3)
    public String testCaseDescription;

    @Parameterized.Parameters(name = "desc = {3}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        campaign(),
                        wallet(),
                        status(),
                        "ACTIVE.1 - синхронизированная кампания с деньгами на ОС"
                },
                {
                        campaign()
                                .withSum(DEFAULT_CAMPAIGN_MONEY.add(BigDecimal.TEN))
                                .withSumSpent(DEFAULT_CAMPAIGN_MONEY),
                        wallet()
                                .withSum(BigDecimal.ZERO)
                                .withSumOnCampaigns(BigDecimal.TEN),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE),
                        "ACTIVE.2 - новая кампания; синхронизирована; денег на ОС нет, но есть остатки на кампании"
                },
                {
                        campaign()
                                .withArchived(true),
                        wallet(),
                        status()
                                .withReadOnly(true)
                                .withArchived(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ARCHIVED)
                                .withPrimaryStatusDesc(null)
                                .withAllowDomainMonitoring(false),
                        "ARCHIVED.1 - кампания помещена в архив"
                },
                {
                        campaign()
                                .withArchived(true)
                                .withCurrencyCode(CurrencyCode.YND_FIXED)
                                .withCurrencyConverted(true),
                        wallet(),
                        status()
                                .withReadOnly(true)
                                .withArchived(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ARCHIVED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_NOT_RECOVERED)
                                .withAllowDomainMonitoring(false),
                        "ARCHIVED.2 - в архиве после конвертации (не восстановима)"

                },
                {
                        campaign()
                                .withArchived(true)
                                .withDelayedOperation(GdiCampaignDelayedOperation.UNARC),
                        wallet(),
                        status()
                                .withReadOnly(true)
                                .withArchived(true)
                                .withWaitingForUnArchiving(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ARCHIVED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_IN_PROGRESS)
                                .withAllowDomainMonitoring(false),
                        "ARCHIVED.3 - в архиве, в процессе разархивации"
                },
                {
                        campaign()
                                .withHasBanners(false)
                                .withHasActiveBanners(false)
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        wallet(),
                        status()
                                .withDraft(true)
                                .withModerationStatus(GdCampaignStatusModerate.NEW)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.DRAFT)
                                .withPrimaryStatusDesc(null),
                        "DRAFT.1"
                },
                {
                        campaign()
                                .withOrderId(0L)
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        wallet(),
                        status()
                                .withDraft(true)
                                .withModerationStatus(GdCampaignStatusModerate.NEW)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.DRAFT)
                                .withPrimaryStatusDesc(null),
                        "DRAFT.2"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withStatusModerate(CampaignStatusModerate.NEW)
                                .withSumToPay(BigDecimal.TEN),
                        wallet(),
                        status()
                                .withModerationStatus(GdCampaignStatusModerate.NEW)
                                .withDraft(true)
                                .withWaitingForPayment(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.DRAFT)
                                .withPrimaryStatusDesc(null),
                        "DRAFT.3"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withActive(false)
                                .withShowing(false)
                                .withStatusModerate(CampaignStatusModerate.NEW)
                                .withSumToPay(BigDecimal.TEN),
                        wallet(),
                        status()
                                .withModerationStatus(GdCampaignStatusModerate.NEW)
                                .withDraft(true)
                                .withWaitingForPayment(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.DRAFT)
                                .withPrimaryStatusDesc(null),
                        "DRAFT.4 - остановленный черновик"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withStatusModerate(CampaignStatusModerate.SENT)
                                .withSumToPay(BigDecimal.TEN),
                        wallet(),
                        status()
                                .withWaitingForPayment(true)
                                .withModerationStatus(GdCampaignStatusModerate.SENT)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.MODERATION)
                                .withPrimaryStatusDesc(null),
                        "MODERATION.1"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withStatusModerate(CampaignStatusModerate.YES)
                                .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                .withSumToPay(BigDecimal.TEN),
                        wallet(),
                        status()
                                .withModerationStatus(GdCampaignStatusModerate.NO)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.MODERATION_DENIED)
                                .withPrimaryStatusDesc(null),
                        "MODERATION_DENIED.1"
                },
                {
                        campaign()
                                .withHasBanners(false)
                                .withHasActiveBanners(false),
                        wallet(),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.NO_ACTIVE_BANNERS),
                        "STOPPED.1 - в кампании нет баннеров"
                },
                {
                        campaign()
                                .withHasBanners(true)
                                .withHasActiveBanners(false),
                        wallet(),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.NO_ACTIVE_BANNERS),
                        "STOPPED.2 - в кампании нет активных баннеров"
                },
                {
                        campaign()
                                .withActive(false)
                                .withShowing(false),
                        wallet(),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(null),
                        "STOPPED.3 - кампания остановлена пользователем (давно)"
                },
                {
                        campaign()
                                .withFinishDate(TEST_INSTANT.atZone(MSK).toLocalDate().minusDays(1)),
                        wallet(),
                        status()
                                .withOver(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_OVER),
                        "STOPPED.2 - кампания остановлена из-за наступления finishDate"
                },
                {
                        campaign()
                                .withStartDate(TEST_INSTANT.atZone(MSK).toLocalDate().plusDays(10)),
                        wallet(),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_WAITING_START),
                        "TEMPORARILY_PAUSED.1 - кампания ожидает даты старта (есть деньги на ОС)"
                },
                {
                        campaign()
                                .withDayBudget(BigDecimal.TEN)
                                .withDayBudgetStopTime(TEST_DATE.atStartOfDay().plusHours(1)),
                        wallet(),
                        status()
                                .withBudgetLimitationStopTime(TEST_DATE.atStartOfDay().plusHours(1))
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_PAUSED_BY_DAY_BUDGET),
                        "TEMPORARILY_PAUSED.2"
                },
                {
                        campaign()
                                .withDayBudget(BigDecimal.TEN)
                                .withDayBudgetStopTime(TEST_DATE.atStartOfDay().minusDays(1).plusHours(1)),
                        wallet(),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE),
                        "ACTIVE.6 - dayBudgetStopTime вчерашний"
                },
                {
                        campaign(),
                        wallet()
                                .withStatus(walletStatus().withBudgetLimitationStopTime(
                                TEST_INSTANT.atZone(MSK).toLocalDateTime().minusMinutes(30))
                        ),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_PAUSED_BY_WALLET_DAY_BUDGET),
                        "TEMPORARILY_PAUSED.4 - остановлено из-за ограничения на общем счете"
                },
                {
                        campaign(),
                        wallet()
                                .withStatus(walletStatus().withBudgetLimitationStopTime(
                                TEST_INSTANT.atZone(MSK).toLocalDateTime().minusHours(3))
                        ),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_PAUSED_BY_WALLET_DAY_BUDGET),
                        "TEMPORARILY_PAUSED.5 - остановлено из-за ограничения на ОС (начало суток c поправкой на MSK)"
                },
                {
                        campaign(),
                        wallet()
                                .withStatus(walletStatus().withBudgetLimitationStopTime(
                                TEST_INSTANT.atZone(MSK).toLocalDateTime().plusHours(20).plusMinutes(59))
                        ),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_PAUSED_BY_WALLET_DAY_BUDGET),
                        "TEMPORARILY_PAUSED.6 - остановлено из-за ограничения на общем счете (конец суток с поправкой на MSK)"
                },
                {
                        campaign()
                                .withOrderId(0L)
                                .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                .withActive(false)
                                .withSum(BigDecimal.ZERO),
                        wallet()
                                .withSum(BigDecimal.ZERO),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.ADD_MONEY_TO_WALLET),
                        "NO_MONEY.1 - новая кампания; прошла модерацию; денег на ОС нет."
                },
                {
                        campaign(),
                        wallet()
                                .withSum(BigDecimal.ZERO),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.ADD_MONEY_TO_WALLET),
                        "NO_MONEY.2 - новая кампания; синхронизирована; денег на ОС нет."
                },
                {
                        campaign(),
                        wallet()
                                .withSum(BigDecimal.ZERO)
                                .withSumOnCampaigns(BigDecimal.TEN)
                                .withStatus(walletStatus().withWaitingForPayment(true)),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.WAIT_PAYMENT),
                        "NO_MONEY.4 - новая кампания; Общий счет ожидает оплаты"
                },
                {
                        campaign()
                                .withStartDate(TEST_INSTANT.atZone(MSK).toLocalDate().plusDays(10)),
                        wallet()
                                .withSum(BigDecimal.ZERO),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.ADD_MONEY_TO_WALLET),
                        "NO_MONEY.5 - на кошельке нет денег vs ожидает старта"
                },
                {
                        campaign()
                                .withDelayedOperation(GdiCampaignDelayedOperation.ARC),
                        wallet(),
                        status()
                                .withWaitingForArchiving(true),
                        "ACTIVE.2 - Ожидает архивации"
                },
                {
                        campaign()
                                .withSum(BigDecimal.TEN)
                                .withSumSpent(BigDecimal.TEN.subtract(BigDecimal.valueOf(0.5)))
                                .withSumLast(BigDecimal.TEN),
                        wallet(),
                        status(),
                        "ACTIVE.3 - на кампанию были зачисления, затем переведена на ОС. Баланс положительный"
                },
                {
                        campaign()
                                .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                .withShowing(true)
                                .withActive(false),
                        wallet(),
                        status()
                                .withActivating(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_IN_PROGRESS),
                        "ACTIVE.4 - идет активизация"
                },
                {
                        campaign()
                                .withSumSpent(DEFAULT_CAMPAIGN_MONEY),
                        wallet()
                                // Все деньги с кошелька потрачены
                                .withSum(BigDecimal.ZERO)
                                .withSumOnCampaigns(DEFAULT_CAMPAIGN_MONEY.negate())
                                // И автоовердрафта нет
                                .withAutoOverdraftAddition(BigDecimal.valueOf(0)),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.ADD_MONEY_TO_WALLET),
                        "NO_MONEY.6 - на кошельке нет денег"
                },
                {
                        campaign()
                                .withSumSpent(DEFAULT_CAMPAIGN_MONEY),
                        wallet()
                                // Все деньги с кошелька потрачены
                                .withSum(BigDecimal.ZERO)
                                .withSumOnCampaigns(DEFAULT_CAMPAIGN_MONEY.negate())
                                // Но есть автоовердрафт
                                .withAutoOverdraftAddition(BigDecimal.valueOf(0.01)),
                        status(),
                        "ACTIVE.5 - синхронизированная кампания с деньгами на ОС"
                },
        });
    }

    private static GdiCampaign campaign() {
        return defaultCampaign(CAMPAIGN_ID)
                .withEmpty(false)
                .withWalletId(42L)
                .withSum(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withStartDate(TEST_DATE.minusDays(3))
                .withMoneyBlocked(false);
    }

    private static GdWallet wallet() {
        return new GdWallet()
                .withId(42L)
                .withCurrency(CurrencyCode.RUB)
                .withSum(DEFAULT_CAMPAIGN_MONEY)
                .withSumOnCampaigns(BigDecimal.ZERO)
                .withAutoOverdraftAddition(BigDecimal.ZERO)
                .withStatus(walletStatus());
    }

    private static GdWalletStatus walletStatus() {
        return new GdWalletStatus()
                .withBudgetLimitationStopTime(null)
                .withEnabled(true)
                .withMoneyBlocked(false)
                .withWaitingForPayment(false)
                .withNeedsNewPayment(false);
    }


    private static GdCampaignStatus status() {
        return new GdCampaignStatus()
                .withCampaignId(CAMPAIGN_ID)
                .withReadOnly(false)
                .withOver(false)
                .withActivating(false)
                .withArchived(false)
                .withDraft(false)
                .withWaitingForUnArchiving(false)
                .withWaitingForArchiving(false)
                .withWaitingForPayment(false)
                .withNeedsNewPayment(false)
                .withMoneyBlocked(false)
                .withAllowDomainMonitoring(true)
                .withModerationStatus(GdCampaignStatusModerate.YES)
                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                .withTimeTargetStatus(STATUS_INFO);
    }


    @Test
    public void testStatus() {
        GdCampaignStatus campaignStatus = service.extractStatus(campaign, wallet, TEST_INSTANT);

        assertThat(campaignStatus)
                .is(matchedBy(beanDiffer(expectedStatus)));
    }
}
