package ru.yandex.market.tpl.core.service.notification;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.sms.SmsClient;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.domain.user.User;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@CoreTest
public class UserShiftPickupNotificationSingleOrderTest {
    private static final long UID = 1L;

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TrackingService trackingService;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;


    @Value("${tpl.tracking.url:https://m.pokupki.market.yandex.ru/tracking/}")
    private String trackingUrl;

    @Autowired
    @Qualifier("yaSmsClient")
    private SmsClient yaSmsClient;

    private LocalDate now;
    private User user;
    private UserShift userShift;
    private Order order;

    @Captor
    private ArgumentCaptor<String> smsTextCaptor;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());

        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createOpenedShift(user, order, now, SORTING_CENTER_ID);

    }

    @Test
    void shouldSendCorrectNotificationIfMergeIsEnabled() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SMS_NOTIFICATION_SEND, 1);

        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);
        Mockito.verify(yaSmsClient, Mockito.only())
                .send(Mockito.anyString(), smsTextCaptor.capture());

        Assertions.assertThat(smsTextCaptor.getAllValues())
                .hasSize(1);

        var offset = dsZoneOffsetCachingService.getOffsetForDs(order.getDeliveryServiceId());
        var trackingLink = trackingService.getTrackingLinkByOrder(order.getExternalOrderId()).orElseThrow();

        Assertions.assertThat(smsTextCaptor.getAllValues().get(0))
                .isEqualTo(String.format(
                        "Курьер привезёт заказ %s сегодня с %d:00 до %d:00. Точный интервал и детали доставки " +
                                "смотрите на %s",
                        order.getExternalOrderId(),
                        DateTimeUtil.toLocalTime(order.getDelivery().getDeliveryIntervalFrom(), offset).getHour(),
                        DateTimeUtil.toLocalTime(order.getDelivery().getDeliveryIntervalTo(), offset).getHour(),
                        trackingUrl + trackingLink
                ));
    }
}
