package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor;
import ru.yandex.market.loyalty.core.dao.coin.CoinNotificationDao;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.SqlMonitorService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.core.model.notification.SendStatus.ERROR_ON_SEND;
import static ru.yandex.market.loyalty.core.model.notification.SendStatus.NEED_TO_SEND;
import static ru.yandex.market.loyalty.core.utils.MonitorHelper.assertMonitor;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor({CheckouterEventRestProcessor.class, CoinNotificationProcessor.class})
public class CoinNotificationServiceTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    @Autowired
    private CoinNotificationProcessor coinNotificationProcessor;
    @Autowired
    private CoinNotificationDao coinNotificationDao;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private SqlMonitorService sqlMonitorService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private CheckouterEventRestProcessor checkouterEventProcessor;

    @Override
    protected boolean shouldCheckConsistence() {
        return false;
    }

    @Test
    public void shouldNotRetry400() throws PersNotifyClientException {
        doThrow(new PersNotifyClientException(new HttpClientErrorException(HttpStatus.BAD_REQUEST))).when(persNotifyClient).createEvent(any());
        sendCoin();

        coinNotificationProcessor.notifyUsersAboutCoinActivation();
        coinNotificationProcessor.notifyUsersAboutCoinActivation();

        verify(persNotifyClient).createEvent(argThat(hasProperty("uid", equalTo(DEFAULT_UID))));
        assertThat(coinNotificationDao.getCoinsWithoutNotifications(100), empty());
        assertEquals(ERROR_ON_SEND, coinNotificationDao.getAll().get(0).getSendStatus());
    }


    @Test
    public void notifyUsersAboutCoinActivationAfterError() throws PersNotifyClientException {
        doThrow(new PersNotifyClientException()).when(persNotifyClient).createEvent(any());
        sendCoin();
        coinNotificationProcessor.notifyUsersAboutCoinActivation();
        reset(persNotifyClient);
        coinNotificationProcessor.notifyUsersAboutCoinActivation();
        verify(persNotifyClient)
                .createEvent(argThat(hasProperty("uid", equalTo(DEFAULT_UID))));
    }

    @Test
    public void notifyUsersAboutCoinActivation() throws PersNotifyClientException {
        sendCoin();
        coinNotificationProcessor.notifyUsersAboutCoinActivation();
        verify(persNotifyClient)
                .createEvent(argThat(hasProperty("uid", equalTo(DEFAULT_UID))));
    }

    @Test
    public void shouldNotNotifyUsersAboutTaggedCoinActivation() throws PersNotifyClientException {
        sendCoin(true);
        coinNotificationProcessor.notifyUsersAboutCoinActivation();
        verify(persNotifyClient, never()).createEvent(any());
    }

    @Test
    public void shouldFireMonitorWhenAnyNot400error() throws PersNotifyClientException {
        clock.spendTime(-1, ChronoUnit.HOURS);
        doThrow(new PersNotifyClientException()).when(persNotifyClient).createEvent(any());
        sendCoin();
        coinNotificationProcessor.notifyUsersAboutCoinActivation();
        assertEquals(NEED_TO_SEND, coinNotificationDao.getAll().get(0).getSendStatus());

        ComplicatedMonitoring.Result check = sqlMonitorService.checkDbState();
        assertMonitor(MonitoringStatus.CRITICAL, check);
        assertThat(check.getMessage(), containsString("notification was not send"));
    }

    private void sendCoin() {
        sendCoin(false);
    }

    private void sendCoin(boolean tagged) {
        // генерируем по две монеты
        SmartShoppingPromoBuilder builder = PromoUtils.SmartShopping.defaultFixed();
        if (tagged) {
            builder.setCreateInactiveCoin();
        }
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(builder),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(builder),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setPaymentType(PaymentType.PREPAID)
                        .addItem(CheckouterUtils.defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        checkouterEventProcessor.processCheckouterEvents(0, 4, 0);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setUid(DEFAULT_UID)
                        .setPaymentType(PaymentType.PREPAID)
                        .addItem(CheckouterUtils.defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        checkouterEventProcessor.processCheckouterEvents(0, 4, 0);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
    }
}
