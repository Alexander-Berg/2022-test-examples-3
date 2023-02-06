package ru.yandex.market.tpl.core.domain.push_beru.listeners;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.push_beru.notification.PushBeruNotificationRepository;
import ru.yandex.market.tpl.core.domain.push_beru.notification.model.PushBeruNotification;
import ru.yandex.market.tpl.core.domain.push_beru.notification.model.PushBeruPayloadFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand.CheckIn;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.external.xiva.PushSendService;
import ru.yandex.market.tpl.core.external.xiva.model.PushBeruEvent;
import ru.yandex.market.tpl.core.external.xiva.model.PushSendRequest;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@Log4j2
@CoreTest
class PushBeruListenerTest {

    private static final String TRANSIT_ID = "transitId";
    private static final long UID = 123L;

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final PushBeruNotificationRepository pushBeruNotificationRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final PushBeruPayloadFactory pushBeruPayloadFactory;

    private RoutePoint routePoint;
    private long userShiftId;
    private Shift shift;
    private User user;
    private Order order;

    @MockBean
    @Qualifier("sendBeruXivaClient")
    private PushSendService pushSendService;
    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        user = testUserHelper.findOrCreateUser(UID, LocalDate.now(clock));
        shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        routePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId,
                Instant.now(clock).plus(10, ChronoUnit.MINUTES),
                Instant.now(clock).plus(10, ChronoUnit.MINUTES));
        var task = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .buyerYandexUid(UID)
                        .build());
        order = orderRepository.getOne(task.getOrderId());
        commandService.checkin(user, new CheckIn(userShiftId));

        when(pushSendService.send(any())).thenReturn(TRANSIT_ID);

        configurationServiceAdapter.insertValue(ConfigurationProperties.PUSH_BERU_NOTIFICATION_ENABLED, true);
    }

    @Test
    void pushAfterPickUpWithSuitableExpectedDeliveryTime() {
        UserShift byIdOrThrow = userShiftRepository.findByIdOrThrow(userShiftId);
        testUserHelper.checkinAndFinishPickup(byIdOrThrow);

        var notifications = pushBeruNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        var notification = notifications.iterator().next();
        dbQueueTestUtil.assertQueueHasSingleEvent(QueueType.PUSH_BERU_NOTIFICATION,
                String.valueOf(notification.getId()));
        dbQueueTestUtil.executeSingleQueueItem(QueueType.PUSH_BERU_NOTIFICATION);
        PushBeruNotification actualNotification = pushBeruNotificationRepository.findById(notifications.get(0).getId())
                .orElseThrow();
        assertThat(actualNotification).extracting(PushBeruNotification::getTransitId).isEqualTo(TRANSIT_ID);
        assertThat(notification).isEqualToIgnoringGivenFields(
                PushBeruNotification.builder()
                        .event(PushBeruEvent.COURIER_GO_TO_CLIENT)
                        .xivaUserId(UID + "")
                        .title("Отслеживайте заказ на карте")
                        .body("В течение 15 минут он будет у вас")
                        .ttlSec(-1)
                        .orderId(order.getId())
                        .shiftId(shift.getId())
                        .payload(PushNotificationPayload.EMPTY)
                        .multiOrderId(order.getId() + "")
                        .build(),
                "ttlSec", "payload", "id", "createdAt", "updatedAt", "transitId", "sendTime", "retryNum"
        );

        var trackingId = trackingRepository.findByOrderId(order.getId()).map(Tracking::getId).orElseThrow();
        assertThat(actualNotification.getPayload().getPayload()).isEqualToIgnoringGivenFields(
                pushBeruPayloadFactory.createCourierGoToClientPayload(trackingId, "title", "body"),
                "message");

        verify(pushSendService).send(argThat(arg -> {
            assertThat((PushSendRequest) arg).extracting(PushSendRequest::getSkipFcm).isEqualTo(true);
            return true;
        }));
    }

    @Test
    void pushAfterDeliveryWhenNextRoutePointIsDeliveryInSuitableTime() {
        var rp2 = testDataFactory.createEmptyRoutePoint(user, userShiftId,
                Instant.now(clock).plus(30, ChronoUnit.MINUTES),
                Instant.now(clock).plus(30, ChronoUnit.MINUTES));

        OrderDeliveryTask newTask = testDataFactory.addDeliveryTaskManual(user, userShiftId, rp2.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .buyerYandexUid(UID)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());

        doReturn(clock.instant().plus(5, ChronoUnit.MINUTES)).when(clock).instant();
        testUserHelper.checkinAndFinishPickup(userShiftRepository.findByIdOrThrow(userShiftId));

        testUserHelper.finishDelivery(routePoint, false);

        var notifications = pushBeruNotificationRepository.findAll();
        assertEquals(2, notifications.size());
    }

    @Test
    void noPushForFailedOrderDeliveryTask() {
        var rp2 = testDataFactory.createEmptyRoutePoint(user, userShiftId,
                Instant.now(clock).plus(30, ChronoUnit.MINUTES),
                Instant.now(clock).plus(30, ChronoUnit.MINUTES));
        var task2 = testDataFactory.addDeliveryTaskManual(user, userShiftId, rp2.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .buyerYandexUid(UID)
                        .build());

        doReturn(clock.instant().plus(5, ChronoUnit.MINUTES)).when(clock).instant();
        testUserHelper.checkinAndFinishPickup(userShiftRepository.findByIdOrThrow(userShiftId));

        //пользователь отменяет доставку, хотя заказ уже у курьера
        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(
                        userShiftId, rp2.getId(), task2.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED, null,
                                null, Source.DELIVERY)
                ));

        testUserHelper.finishDelivery(routePoint, false);

        var notifications = pushBeruNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(order.getId(), notifications.iterator().next().getOrderId());
    }

    @Test
    void noPushForDeliveryBySellerOrder() {
        User dbsUser = testUserHelper.createOrFindDbsUser();
        long dbsUserShiftId = testDataFactory.createEmptyShift(shift.getId(), dbsUser);
        routePoint = testDataFactory.createEmptyRoutePoint(dbsUser, dbsUserShiftId,
                Instant.now(clock).plus(10, ChronoUnit.MINUTES),
                Instant.now(clock).plus(10, ChronoUnit.MINUTES));
        var task = testDataFactory.addDeliveryTaskManual(dbsUser, dbsUserShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CASH)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .buyerYandexUid(dbsUser.getUid())
                        .build());
        Order dbsOrder = orderRepository.getOne(task.getOrderId());
        commandService.checkin(dbsUser, new CheckIn(dbsUserShiftId));


        UserShift byIdOrThrow = userShiftRepository.findByIdOrThrow(dbsUserShiftId);
        testUserHelper.checkinAndFinishPickup(byIdOrThrow);

        verifyNoMoreInteractions(pushSendService);
    }
}
