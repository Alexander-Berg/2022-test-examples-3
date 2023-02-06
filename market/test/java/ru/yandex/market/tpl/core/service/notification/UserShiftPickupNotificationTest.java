package ru.yandex.market.tpl.core.service.notification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
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
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.util.AddDeliveryTaskHelper;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@CoreTest
public class UserShiftPickupNotificationTest {
    private static final long UID = 1L;

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final AddDeliveryTaskHelper addDeliveryTaskHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;
    private final TrackingService trackingService;

    @Autowired
    @Qualifier("yaSmsClient")
    private SmsClient yaSmsClient;

    @Value("${tpl.tracking.url:https://m.pokupki.market.yandex.ru/tracking/}")
    private String trackingUrl;

    @Captor
    private ArgumentCaptor<String> smsTextCaptor;

    private LocalDate now;
    private LocalTimeInterval deliveryInterval;
    private GeoPoint geoPoint;
    private String phone;

    private User user;
    private UserShift userShift;

    private Order order;
    private Order order2;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();

        phone = "+74952234562";

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST, phone);

        geoPoint = GeoPoint.ofLatLon(new BigDecimal("55.787878"), new BigDecimal("37.656565"));
        deliveryInterval = new LocalTimeInterval(
                LocalTime.of(15, 0), LocalTime.of(18, 0)
        );
        // Это время которое считает маршртуизация
        var expectedTimeArrival = ZonedDateTime.of(
                LocalDateTime.of(now, LocalTime.of(15, 43)), DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();

        var expectedDeliveryTime = ZonedDateTime.of(
                LocalDateTime.of(now, LocalTime.of(15, 55)), DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();

        user = testUserHelper.findOrCreateUser(UID);
        var shift = testUserHelper.findOrCreateOpenShiftForSc(now, SORTING_CENTER_ID);
        userShift = testUserHelper.createEmptyShift(user, shift);

        order = createOrder();
        order2 = createOrder();

        DeliveryTask task1 = userShiftCommandService.addDeliveryTask(user,
                addDeliveryTaskHelper.createAddDeliveryTaskCommand(
                        userShift, order, expectedTimeArrival, expectedDeliveryTime, false
                )
        );

        userShiftCommandService.addDeliveryTask(user,
                addDeliveryTaskHelper.createAddDeliveryTaskCommand(
                        userShift, order2, expectedTimeArrival, expectedDeliveryTime, false
                )
        );

        OrderDeliveryTask reloaded = taskOrderDeliveryRepository.findByIdOrThrow(task1.getId());
        Assertions.assertThat(reloaded.isPartOfMultiOrder()).isTrue();
    }

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(yaSmsClient);
    }

    @Test
    void shouldMergeSmsIfEnabled() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                true);

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SMS_NOTIFICATION_SEND, 1);

        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);

        Mockito.verify(yaSmsClient, Mockito.only())
                .send(Mockito.anyString(), smsTextCaptor.capture());

        Assertions.assertThat(smsTextCaptor.getAllValues())
                .hasSize(1);

        String expectedSms = smsTextCaptor.getAllValues().get(0);

        Assertions.assertThat(equalsSmsText(expectedSms, order, order2) ||
                equalsSmsText(expectedSms, order2, order)).isTrue();
    }


    private boolean equalsSmsText(String expectedSms, Order order1, Order order2) {
        var offset = dsZoneOffsetCachingService.getOffsetForDs(order1.getDeliveryServiceId());
        var trackingLink = trackingService.getTrackingLinkByOrder(order1.getExternalOrderId()).orElseThrow();

        return expectedSms.equals(String.format(
                "Курьер привезёт заказы %s сегодня с %d:00 до %d:00. Точный интервал и детали доставки " +
                        "смотрите на %s",
                String.join(", ", order1.getExternalOrderId(), order2.getExternalOrderId()),
                DateTimeUtil.toLocalTime(order1.getDelivery().getDeliveryIntervalFrom(), offset).getHour(),
                DateTimeUtil.toLocalTime(order1.getDelivery().getDeliveryIntervalTo(), offset).getHour(),
                trackingUrl + trackingLink
        ));
    }

    @Test
    void shouldNotMergeSmsIfDisabled() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                false);

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SMS_NOTIFICATION_SEND, 2);

        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);

        Mockito.verify(yaSmsClient, Mockito.times(2))
                .send(Mockito.anyString(), smsTextCaptor.capture());

        var offset = dsZoneOffsetCachingService.getOffsetForDs(order.getDeliveryServiceId());
        var offset2 = dsZoneOffsetCachingService.getOffsetForDs(order2.getDeliveryServiceId());

        Assertions.assertThat(smsTextCaptor.getAllValues())
                .hasSize(2)
                .anyMatch(Predicate.isEqual(
                        String.format(
                                "Курьер привезёт заказ %s сегодня с %d:00 до %d:00. Точный интервал и детали доставки" +
                                        " смотрите на %s",
                                order.getExternalOrderId(),
                                DateTimeUtil.toLocalTime(order.getDelivery().getDeliveryIntervalFrom(), offset).getHour(),
                                DateTimeUtil.toLocalTime(order.getDelivery().getDeliveryIntervalTo(), offset).getHour(),
                                trackingUrl + trackingService.getTrackingLinkByOrder(order.getExternalOrderId()).orElseThrow()
                        )
                ))
                .anyMatch(Predicate.isEqual(
                        String.format(
                                "Курьер привезёт заказ %s сегодня с %d:00 до %d:00. Точный интервал и детали доставки" +
                                        " смотрите на %s",
                                order2.getExternalOrderId(),
                                DateTimeUtil.toLocalTime(order2.getDelivery().getDeliveryIntervalFrom(), offset2).getHour(),
                                DateTimeUtil.toLocalTime(order2.getDelivery().getDeliveryIntervalTo(), offset2).getHour(),
                                trackingUrl + trackingService.getTrackingLinkByOrder(order2.getExternalOrderId()).orElseThrow()
                        )
                ));


    }

    private Order createOrder() {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .deliveryDate(now)
                .deliveryInterval(deliveryInterval)
                .recipientPhone(phone)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .street("Колотушкина")
                        .house("1")
                        .build())
                .build());
    }
}
