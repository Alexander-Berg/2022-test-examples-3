package ru.yandex.market.tpl.core.domain.push.listeners;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.tpl.api.model.notification.PushRedirect;
import ru.yandex.market.tpl.api.model.notification.PushRedirectType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.equeue.event.EqueueInviteToLoadingEvent;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.push.notification.PushNotificationRepository;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotification;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.events.DeliveryTaskFailedEvent;
import ru.yandex.market.tpl.core.domain.usershift.events.DeliveryTaskRescheduledEvent;
import ru.yandex.market.tpl.core.external.xiva.XivaClient;
import ru.yandex.market.tpl.core.external.xiva.model.PushEvent;
import ru.yandex.market.tpl.core.external.xiva.model.PushSendRequest;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.CLIENT_REQUEST;
import static ru.yandex.market.tpl.core.service.notification.PushNotificationFactory.PUSH_TITLE;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class PushNotificationListenerTest {

    private static final String TRANSIT_ID = "myTransitId";

    private final ApplicationEventPublisher eventPublisher;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderRepository orderRepository;
    private final PushNotificationRepository pushNotificationRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;

    private OrderDeliveryTask task;
    private OrderDeliveryTask notCurrentTask;
    private Order order;
    private User user;

    @MockBean
    private XivaClient xivaClient;
    @MockBean
    private Clock clock;

    @BeforeEach
    void initOrderDeliveryTask() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        user = testUserHelper.findOrCreateUser(1L, LocalDate.now(clock));
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var routePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId);
        task = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        var routePoint2 = testDataFactory.createEmptyRoutePoint(user, userShiftId);
        notCurrentTask = testDataFactory.addDeliveryTaskManual(user, userShiftId, routePoint2.getId(),
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        testUserHelper.checkinAndFinishPickup(userShiftRepository.findByIdOrThrow(userShiftId));
        order = orderRepository.getOne(task.getOrderId());
        doReturn(TRANSIT_ID).when(xivaClient).send(any());
    }

    @Test
    void noPushFailedEventFromCourier() {
        eventPublisher.publishEvent(new DeliveryTaskFailedEvent(
                task, new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, "", null, Source.COURIER),
                true
        ));

        assertNoPush();
    }

    @Test
    void processFailedEvent() {
        eventPublisher.publishEvent(new DeliveryTaskFailedEvent(
                task, new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, "", null, Source.DELIVERY),
                true
        ));

        List<PushNotification> notifications = pushNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        dbQueueTestUtil.assertQueueHasSingleEvent(QueueType.PUSH_NOTIFICATION_SEND,
                String.valueOf(notifications.get(0).getId()));
        dbQueueTestUtil.executeSingleQueueItem(QueueType.PUSH_NOTIFICATION_SEND);

        verify(xivaClient).send(argThat(arg -> {
            assertThat(arg).usingRecursiveComparison().isEqualTo(new PushSendRequest(
                            "1", PushEvent.ORDER_CANCEL, PUSH_TITLE,
                            "Доставка заказа " + order.getExternalOrderId() + " отменена", 60 * 60,
                            new PushNotificationPayload(
                                    new PushRedirect(PushRedirectType.ROUTE_POINT_PAGE,
                                            task.getRoutePoint().getId(), task.getId(), null)
                            ), null
                    )
            );
            return true;
        }));
        assertThat(pushNotificationRepository.findById(notifications.get(0).getId()).orElseThrow().getTransitId())
                .isEqualTo(TRANSIT_ID);
    }

    @Test
    void noPushForNotCurrentRoutePoint() {
        eventPublisher.publishEvent(new DeliveryTaskFailedEvent(
                notCurrentTask,
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, "", null, Source.DELIVERY),
                false
        ));

        assertNoPush();
    }

    @Test
    void noPushRescheduleFromCourier() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        Instant todayRescheduleFrom = clock.instant().plus(1L, ChronoUnit.HOURS);
        Instant todayRescheduleTo = clock.instant().plus(3L, ChronoUnit.HOURS);

        DeliveryReschedule reschedule = DeliveryReschedule.fromCourier(user, todayRescheduleFrom, todayRescheduleTo,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);

        eventPublisher.publishEvent(new DeliveryTaskRescheduledEvent(
                task,
                reschedule,
                clock.instant(),
                false,
                task.getOrderIds()
        ));

        assertNoPush();
    }

    @Test
    void processRescheduledForTodayEvent() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime().plusHours(15));
        Instant todayRescheduleFrom = clock.instant().plus(1L, ChronoUnit.HOURS);
        Instant todayRescheduleTo = clock.instant().plus(3L, ChronoUnit.HOURS);
        testRescheduled(clock.instant(), todayRescheduleFrom, todayRescheduleTo, PushEvent.ORDER_RESCHEDULE_TODAY,
                "Заказ " + order.getExternalOrderId() + " перенесен на другой интервал: с 16 до 18");
    }

    @Test
    void processRescheduledForOtherDayEvent() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        Instant todayRescheduleFrom = clock.instant().plus(1L, ChronoUnit.DAYS);
        Instant todayRescheduleTo = clock.instant().plus(2L, ChronoUnit.DAYS);
        testRescheduled(clock.instant(), todayRescheduleFrom, todayRescheduleTo, PushEvent.ORDER_RESCHEDULE_OTHER_DAY,
                "Доставка заказа " + order.getExternalOrderId() + " перенесена на завтра");
    }

    @Test
    void processRescheduledForOtherDayByClientEvent() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        Instant todayRescheduleFrom = clock.instant().plus(1L, ChronoUnit.DAYS);
        Instant todayRescheduleTo = clock.instant().plus(2L, ChronoUnit.DAYS);
        DeliveryReschedule reschedule = DeliveryReschedule.fromClient(todayRescheduleFrom, todayRescheduleTo);
        testRescheduled(clock.instant(), PushEvent.ORDER_RESCHEDULE_OTHER_DAY,
                "Доставка заказа " + order.getExternalOrderId() + " перенесена клиентом на завтра", reschedule);
    }

    @Test
    void processEqueueInviteToLoadingEvent() {
        var user = testUserHelper.findOrCreateUser(2L, LocalDate.now(clock));
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        UserShift userShift = userShiftRepository.findById(userShiftId).get();

        eventPublisher.publishEvent(new EqueueInviteToLoadingEvent(userShift));

        List<PushNotification> notifications = pushNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        dbQueueTestUtil.assertQueueHasSingleEvent(
                QueueType.PUSH_NOTIFICATION_SEND, String.valueOf(notifications.get(0).getId())
        );
    }

    void testRescheduled(Instant now, Instant rescheduledFrom, Instant rescheduledTo,
                         PushEvent event, String expectedText) {
        Interval interval = new Interval(rescheduledFrom, rescheduledTo);
        DeliveryReschedule reschedule = DeliveryReschedule.fromDelivery(interval, rescheduledTo, CLIENT_REQUEST);
        testRescheduled(now, event, expectedText, reschedule);
    }

    void testRescheduled(Instant now, PushEvent event, String expectedText, DeliveryReschedule reschedule) {
        eventPublisher.publishEvent(new DeliveryTaskRescheduledEvent(
                task,
                reschedule,
                now,
                true,
                task.getOrderIds()
        ));

        List<PushNotification> notifications = pushNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        dbQueueTestUtil.assertQueueHasSingleEvent(QueueType.PUSH_NOTIFICATION_SEND,
                String.valueOf(notifications.get(0).getId()));
        dbQueueTestUtil.executeSingleQueueItem(QueueType.PUSH_NOTIFICATION_SEND);

        verify(xivaClient).send(argThat(arg -> {
            assertThat(arg).usingRecursiveComparison().isEqualTo(new PushSendRequest(
                            "1", event, PUSH_TITLE,
                            expectedText, 60 * 60,
                            new PushNotificationPayload(
                                    new PushRedirect(PushRedirectType.ROUTE_POINT_PAGE,
                                            task.getRoutePoint().getId(), task.getId(), null)
                            ), null
                    )
            );
            return true;
        }));
        assertThat(pushNotificationRepository.findById(notifications.get(0).getId()).orElseThrow().getTransitId())
                .isEqualTo(TRANSIT_ID);
    }

    private void assertNoPush() {
        assertThat(pushNotificationRepository.findAll()).isEmpty();
    }

}
