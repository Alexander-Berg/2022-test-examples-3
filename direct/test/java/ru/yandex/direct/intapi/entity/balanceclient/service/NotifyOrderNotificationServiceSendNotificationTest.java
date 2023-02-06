package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.eventlog.service.EventLogService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.CampFinishedMailNotification;
import ru.yandex.direct.core.entity.notification.container.Notification;
import ru.yandex.direct.core.entity.notification.container.NotificationType;
import ru.yandex.direct.core.entity.notification.container.NotifyOrderMailNotification;
import ru.yandex.direct.core.entity.notification.container.NotifyOrderPayType;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderNotificationService.calcSumPayedUnitsRate;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.ID_NOT_SET;

/**
 * Тесты на методы sendNotification, getNotifyOrderMailNotification, getCampFinishedMailNotification
 * сервиса NotifyOrderNotificationService
 *
 * @see NotifyOrderNotificationService
 */
public class NotifyOrderNotificationServiceSendNotificationTest {

    private static final CurrencyCode CURRENCY = CurrencyCode.RUB;

    private NotifyOrderNotificationService spyService;
    private CampaignDataForNotifyOrder dbCampaignData;
    private Money sum;
    private Money sumPayed;
    private BigDecimal sumUnits;
    private Money sumDelta;
    private Long productRate;
    private NotifyOrderPayType payType;
    private Percent nds;
    private String email;

    @Mock
    private EventLogService eventLogService;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> captor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        spyService =
                spy(new NotifyOrderNotificationService(null, null, null, null, eventLogService, notificationService));

        dbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder();
        sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY);
        sumPayed = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY);
        sumUnits = RandomNumberUtils.nextPositiveBigDecimal();
        sumDelta = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY);
        productRate = RandomNumberUtils.nextPositiveLong();
        payType = NotifyOrderPayType.ANY;
        nds = Percent.fromPercent(BigDecimal.valueOf(RandomUtils.nextDouble(1, 100)));
        email = RandomStringUtils.randomAlphanumeric(10);

        doReturn(payType).when(spyService)
                .getPayType(eq(dbCampaignData));
        doReturn(nds).when(spyService)
                .getAgencyOrClientNds(dbCampaignData.getAgencyId(), dbCampaignData.getClientId());
        doReturn(email).when(spyService)
                .getAgencyEmail(dbCampaignData.getAgencyUid());
    }


    @Test
    public void checkNotSendNotificationWhenGetFirstAfterCopyConvert() {
        dbCampaignData.withFirstAfterCopyConvert(true);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verifyZeroInteractions(notificationService, eventLogService);
    }

    @Test
    public void checkNotSendNotificationWhenSumPayedIsZero() {
        sumPayed = Money.valueOf(BigDecimal.ZERO, CURRENCY);
        dbCampaignData.withFirstAfterCopyConvert(false);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verifyZeroInteractions(notificationService, eventLogService);
    }

    @Test
    public void checkNotSendNotificationWhenPayTypeNotBlocked() {
        sumDelta = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal().negate(), CURRENCY);
        dbCampaignData.withFirstAfterCopyConvert(true);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verifyZeroInteractions(notificationService, eventLogService);
    }

    @Test
    public void checkNotSendNotificationWhenSumDeltaIsZero() {
        sumDelta = Money.valueOf(BigDecimal.ZERO, CURRENCY);
        doReturn(NotifyOrderPayType.BLOCKED).when(spyService)
                .getPayType(eq(dbCampaignData));
        dbCampaignData.withFirstAfterCopyConvert(true);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verifyZeroInteractions(notificationService, eventLogService);
    }

    @Test
    public void checkNotSendNotifyOrderMailNotificationWithMoneyInType_ForCampaignUnderWallet() {
        dbCampaignData.withFirstAfterCopyConvert(false);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verifyZeroInteractions(notificationService, eventLogService);
    }

    @Test
    public void checkNotSendNotifyOrderMailNotificationWithMoneyInType_ForCashbackOnly() {
        dbCampaignData.withFirstAfterCopyConvert(false);
//        dbCampaignData.withFirstAfterCopyConvert(false)
//                .withWalletId(RandomNumberUtils.nextPositiveLong());

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, true);
        verifyZeroInteractions(notificationService, eventLogService);
    }

    @Test
    public void checkAddEventLogForMcbCampaign_andSendNotifyOrderMailNotification() {
        dbCampaignData.withType(CampaignType.MCB)
                .withFirstAfterCopyConvert(false)
                .withWalletId(ID_NOT_SET);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verify(eventLogService).addMoneyInEventLog(dbCampaignData.getCampaignId(), dbCampaignData.getType(), sumPayed,
                dbCampaignData.getClientId());
        verify(notificationService).addNotification(captor.capture());

        NotifyOrderMailNotification expectedNotifyOrderMailNotification = spyService
                .getNotifyOrderMailNotification(NotificationType.NOTIFY_ORDER_MONEY_IN_WO_EVENTLOG, dbCampaignData,
                        sumUnits, sum,
                        sumPayed, productRate, payType);
        assertThat("отправили уведомление с ожидаемыми параметрами", captor.getValue(),
                beanDiffer(expectedNotifyOrderMailNotification));
    }

    @Test
    public void checkSendNotifyOrderMailNotificationWithMoneyInType() {
        dbCampaignData.withFirstAfterCopyConvert(false)
                .withWalletId(ID_NOT_SET);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verify(eventLogService).addMoneyInEventLog(dbCampaignData.getCampaignId(), dbCampaignData.getType(), sumPayed,
                dbCampaignData.getClientId());
        verify(notificationService).addNotification(captor.capture());

        NotifyOrderMailNotification expectedNotifyOrderMailNotification = spyService
                .getNotifyOrderMailNotification(NotificationType.NOTIFY_ORDER_MONEY_IN_WO_EVENTLOG, dbCampaignData,
                        sumUnits, sum,
                        sumPayed, productRate, payType);
        assertThat("отправили уведомление с ожидаемыми параметрами", captor.getValue(),
                beanDiffer(expectedNotifyOrderMailNotification));
    }

    @Test
    public void checkSendCampFinishedMailNotificationAndNotifyOrderMailNotificationWithMoneyInType() {
        dbCampaignData.withFirstAfterCopyConvert(false)
                .withFinishDate(LocalDate.now().minusDays(1))
                .withWalletId(ID_NOT_SET);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verify(eventLogService).addMoneyInEventLog(dbCampaignData.getCampaignId(), dbCampaignData.getType(), sumPayed,
                dbCampaignData.getClientId());
        verify(eventLogService).addCampFinishedEventLog(dbCampaignData.getCampaignId(), dbCampaignData.getFinishDate(),
                dbCampaignData.getClientId());
        verify(notificationService, times(2)).addNotification(captor.capture());

        assertThat("отправили уведомление NOTIFY_ORDER_MONEY_IN_WO_EVENTLOG",
                captor.getAllValues().get(0).getNotificationType(),
                equalTo(NotificationType.NOTIFY_ORDER_MONEY_IN_WO_EVENTLOG));

        CampFinishedMailNotification expectedCampFinishedMailNotification =
                NotifyOrderNotificationService.getCampFinishedMailNotification(dbCampaignData);
        assertThat("отправили уведомление с ожидаемыми параметрами", captor.getAllValues().get(1),
                beanDiffer(expectedCampFinishedMailNotification));
    }

    @Test
    public void checkSendNotifyOrderMailNotificationWithMoneyOutBlockingType() {
        sumDelta = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal().negate(), CURRENCY);
        payType = NotifyOrderPayType.BLOCKED;
        doReturn(payType).when(spyService)
                .getPayType(eq(dbCampaignData));
        dbCampaignData.withFirstAfterCopyConvert(true);

        spyService.sendNotification(dbCampaignData, sumUnits, sum, sumPayed, sumDelta, productRate, false);
        verifyZeroInteractions(eventLogService);
        verify(notificationService).addNotification(captor.capture());

        NotifyOrderMailNotification expectedNotifyOrderMailNotification = spyService
                .getNotifyOrderMailNotification(NotificationType.NOTIFY_ORDER_MONEY_OUT_BLOCKING, dbCampaignData,
                        sumUnits, sum, sumPayed, productRate, payType);
        assertThat("отправили уведомление с ожидаемыми параметрами", captor.getValue(),
                beanDiffer(expectedNotifyOrderMailNotification));
    }

    @Test
    public void checkGetNotifyOrderMailNotification() {
        NotificationType notificationType = NotificationType.NOTIFY_ORDER_MONEY_OUT_BLOCKING;
        BigDecimal sumPayedUnitsRate = calcSumPayedUnitsRate(sumUnits, dbCampaignData.getSumUnits(), productRate);
        Money sumPayedWithoutNds = sumPayed.subtractNds(nds);

        NotifyOrderMailNotification notifyOrderMailNotification = spyService
                .getNotifyOrderMailNotification(notificationType, dbCampaignData, sumUnits, sum, sumPayed,
                        productRate, payType);

        NotifyOrderMailNotification expectedNotifyOrderMailNotification =
                new NotifyOrderMailNotification(notificationType)
                        .withCampaignId(dbCampaignData.getCampaignId())
                        .withCampaignName(dbCampaignData.getName())
                        .withCampaignType(dbCampaignData.getType())
                        .withWalletId(dbCampaignData.getWalletId())
                        .withClientId(dbCampaignData.getClientId())
                        .withClientUserId(dbCampaignData.getUid())
                        .withClientFullName(dbCampaignData.getFio())
                        .withClientLogin(dbCampaignData.getLogin())
                        .withClientPhone(dbCampaignData.getPhone())
                        .withClientEmail(dbCampaignData.getEmail())
                        .withStartTimeTs(dbCampaignData.getStartTimeTs())
                        .withStartTimeInFuture(dbCampaignData.getStartTimeInFuture())
                        .withAgencyUserId(dbCampaignData.getAgencyUid())
                        .withAgencyEmail(email)
                        .withSum(sum)
                        .withSumPayedOriginal(sumPayed)
                        .withSumPayed(sumPayedWithoutNds)
                        .withWithoutNds(true)
                        .withSumPayedUnitsRate(sumPayedUnitsRate)
                        .withNds(nds.asRatio())
                        .withPayType(payType);
        assertThat("метод вернул объект с ожидаемыми параметрами", notifyOrderMailNotification,
                beanDiffer(expectedNotifyOrderMailNotification));
    }

    @Test
    public void checkGetCampFinishedMailNotification() {
        CampFinishedMailNotification campFinishedMailNotification =
                NotifyOrderNotificationService.getCampFinishedMailNotification(dbCampaignData);

        CampFinishedMailNotification expectedCampFinishedMailNotification = new CampFinishedMailNotification()
                .withCampaignId(dbCampaignData.getCampaignId())
                .withFinishDate(dbCampaignData.getFinishDate())
                .withCampaignName(dbCampaignData.getName())
                .withAgencyUid(dbCampaignData.getAgencyUid())
                .withClientId(dbCampaignData.getClientId())
                .withClientUserId(dbCampaignData.getUid())
                .withClientEmail(dbCampaignData.getEmail())
                .withClientFullName(dbCampaignData.getFio())
                .withClientLogin(dbCampaignData.getLogin())
                .withClientPhone(dbCampaignData.getPhone())
                .withClientLang(dbCampaignData.getLang());
        assertThat("метод вернул объект с ожидаемыми параметрами", campFinishedMailNotification,
                beanDiffer(expectedCampFinishedMailNotification));
    }
}
