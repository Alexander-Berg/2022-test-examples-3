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
public class
GridCampaignAggregationFieldsServiceStatusTest {
    // Московская таймзона для удобства написания тестов
    private static final GeoTimezone MSK_TIMEZONE = new GeoTimezone()
            .withRegionId(225L)
            .withTimezoneId(130L)
            .withTimezone(MSK);
    private static final TimeTargetStatusInfo STATUS_INFO = new TimeTargetStatusInfo()
            .withStatus(TimeTargetStatus.ACTIVE);
    private static final LocalDate TEST_DATE = LocalDate.parse("2017-12-27");
    private static final Instant TEST_INSTANT = TEST_DATE.atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final long CAMPAIGN_ID = 121;

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

    @Parameterized.Parameter(0)
    public GdiCampaign campaign;

    @Parameterized.Parameter(1)
    public GdCampaignStatus expectedStatus;

    @Parameterized.Parameter(2)
    public String testCaseDescription;

    @Parameterized.Parameters(name = "desc = {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {campaign(),
                        status(),
                        "ACTIVE.1 - синхронизированная кампания с деньгами"},
                {
                        campaign()
                                .withArchived(true),
                        status()
                                .withAllowDomainMonitoring(false)
                                .withReadOnly(true)
                                .withArchived(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ARCHIVED)
                                .withPrimaryStatusDesc(null),
                        "ARCHIVED.1 Кампания помещена в архив"
                },
                {
                        campaign()
                                .withArchived(true)
                                .withCurrencyCode(CurrencyCode.YND_FIXED)
                                .withCurrencyConverted(true),

                        status()
                                .withAllowDomainMonitoring(false)
                                .withReadOnly(true)
                                .withArchived(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ARCHIVED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_NOT_RECOVERED),
                        "ARCHIVED.2 - в архиве после конвертации (не восстановима)"

                },
                {
                        campaign()
                                .withArchived(true)
                                .withDelayedOperation(GdiCampaignDelayedOperation.UNARC),
                        status()
                                .withAllowDomainMonitoring(false)
                                .withReadOnly(true)
                                .withArchived(true)
                                .withWaitingForUnArchiving(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ARCHIVED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_IN_PROGRESS),
                        "ARCHIVED.3 - в архиве, в процессе разархивации"
                },
                {
                        campaign()
                                .withHasBanners(false)
                                .withHasActiveBanners(false)
                                .withStatusModerate(CampaignStatusModerate.NEW),
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
                                .withStatusModerate(CampaignStatusModerate.READY)
                                .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                .withSumToPay(BigDecimal.TEN),
                        status()
                                .withWaitingForPayment(true)
                                .withModerationStatus(GdCampaignStatusModerate.READY)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.MODERATION)
                                .withPrimaryStatusDesc(null),
                        "MODERATION.1 - кампания отправлена на модерацию - Ready"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withStatusModerate(CampaignStatusModerate.SENT)
                                .withStatusPostModerate(CampaignStatusPostmoderate.NO)

                                .withSumToPay(BigDecimal.TEN),
                        status()
                                .withWaitingForPayment(true)
                                .withModerationStatus(GdCampaignStatusModerate.SENT)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.MODERATION)
                                .withPrimaryStatusDesc(null),
                        "MODERATION.2 - кампания отправлена на модерацию - Sent"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withStatusModerate(CampaignStatusModerate.NO)
                                .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                .withSumToPay(BigDecimal.TEN),
                        status()
                                .withModerationStatus(GdCampaignStatusModerate.NO)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.MODERATION_DENIED)
                                .withPrimaryStatusDesc(null),
                        "MODERATION_DENIED.1"
                },
                {
                        campaign()
                                .withSum(BigDecimal.ZERO)
                                .withStatusModerate(CampaignStatusModerate.YES)
                                .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                .withSumToPay(BigDecimal.TEN),
                        status()
                                .withModerationStatus(GdCampaignStatusModerate.NO)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.MODERATION_DENIED)
                                .withPrimaryStatusDesc(null),
                        "MODERATION_DENIED.2 - кривое сочетание статусов stModerate, stPostModerate" //Todo(pashkus)
                },
                {
                        campaign()
                                .withHasBanners(false)
                                .withHasActiveBanners(false),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.NO_ACTIVE_BANNERS),
                        "STOPPED.1 - в кампании нет баннеров"
                },
                {
                        campaign()
                                .withHasBanners(true)
                                .withHasActiveBanners(false),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.NO_ACTIVE_BANNERS),
                        "STOPPED.2 - в кампании нет активных баннеров"
                },
                {
                        campaign()
                                .withActive(true)
                                .withShowing(false),
                        status()
                                .withActivating(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_IN_PROGRESS),
                        "STOPPED.3 - кампания остановлена пользователем. идет активизация"
                },
                {
                        campaign()
                                .withFinishDate(TEST_INSTANT.atZone(MSK).toLocalDate().minusDays(1)),
                        status()
                                .withOver(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_OVER),
                        "STOPPED.4 - кампания остановлена из-за наступления finishDate"
                },
                {
                        campaign()
                                .withActive(false)
                                .withShowing(false),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                                .withPrimaryStatusDesc(null),
                        "STOPPED.5 - кампания остановлена пользователем (давно)"
                },
                {
                        campaign()
                                .withStartDate(TEST_INSTANT.atZone(MSK).toLocalDate().plusDays(10)),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_WAITING_START),
                        "TEMPORARILY_PAUSED.1 - кампания ожидает старта"
                },
                {
                        campaign()
                                .withDayBudget(BigDecimal.TEN)
                                .withDayBudgetStopTime(TEST_DATE.atStartOfDay().plusHours(1)),
                        status()
                                .withBudgetLimitationStopTime(TEST_DATE.atStartOfDay().plusHours(1))
                                .withPrimaryStatus(GdCampaignPrimaryStatus.TEMPORARILY_PAUSED)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_PAUSED_BY_DAY_BUDGET),
                        "TEMPORARILY_PAUSED.2 - есть dayBudgetStopTime"
                },
                {
                        campaign()
                                .withDayBudget(BigDecimal.TEN)
                                .withDayBudgetStopTime(TEST_DATE.atStartOfDay().minusDays(1).plusHours(1)),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE),
                        "ACTIVE.5 - dayBudgetStopTime вчерашний"
                },
                {
                        campaign()
                                .withSum(DEFAULT_CAMPAIGN_MONEY)
                                .withSumSpent(DEFAULT_CAMPAIGN_MONEY),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.ADD_MONEY),
                        "NO_MONEY.1 на синхронизированной кампании закончились деньги"
                },
                {
                        campaign()
                                .withOrderId(0L)
                                .withSum(BigDecimal.ZERO),
                        status()
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.ADD_MONEY),
                        "NO_MONEY.2 - новая кампания прошла модерацию, но нет денег"
                },
                {
                        campaign()
                                .withOrderId(0L)
                                .withSum(BigDecimal.ZERO)
                                .withSumToPay(BigDecimal.TEN),
                        status()
                                .withWaitingForPayment(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.NO_MONEY)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.WAIT_PAYMENT),
                        "NO_MONEY.3 - клиент положил деньги на новую кампанию"
                },
                {
                        campaign()
                                .withDelayedOperation(GdiCampaignDelayedOperation.ARC),
                        status()
                                .withWaitingForArchiving(true),
                        "ACTIVE.2 - Ожидает архивации"
                },
                {
                        campaign()
                                .withWalletId(0L)
                                .withSum(BigDecimal.TEN)
                                .withSumSpent(BigDecimal.TEN.subtract(BigDecimal.valueOf(0.5)))
                                .withSumLast(BigDecimal.TEN),
                        status()
                                .withNeedsNewPayment(true),
                        "ACTIVE.3 - кампания активна. Требуется пополнение счета"
                },
                {
                        campaign()
                                .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                .withShowing(true)
                                .withActive(false),
                        status()
                                .withActivating(true)
                                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                                .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.IS_IN_PROGRESS),
                        "ACTIVE.4 - идет активизация"
                },
        });
    }

    private static GdiCampaign campaign() {
        return defaultCampaign(CAMPAIGN_ID)
                .withStartDate(TEST_DATE.minusDays(3))
                .withMoneyBlocked(false);
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
        GdCampaignStatus campaignStatus = service.extractStatus(campaign, null, TEST_INSTANT);

        assertThat(campaignStatus)
                .is(matchedBy(beanDiffer(expectedStatus)));
    }
}
