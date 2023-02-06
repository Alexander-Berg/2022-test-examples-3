package ru.yandex.market.tpl.core.domain.order.delayed_order;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerOrderDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.events.DeliveryTaskFailedEvent;
import ru.yandex.market.tpl.core.domain.usershift.events.locker.LockerDeliverySubtaskFailedEvent;
import ru.yandex.market.tpl.core.domain.usershift.events.locker.LockerDeliveryTaskFailedEvent;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointType.DELIVERY;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointType.LOCKER_DELIVERY;
import static ru.yandex.market.tpl.core.domain.order.delayed_order.DelayedOrderStatusListener.DEFAULT_DELAY_BEFORE_CLOSE_PICKUP_MINUTES;
import static ru.yandex.market.tpl.core.domain.order.delayed_order.DelayedOrderStatusListener.DEFAULT_DELAY_ORDER_STATUS_MINUTES;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor
class DelayedOrderStatusListenerTest extends TplAbstractTest {
    private static final int LOCKER_DELIVERY_INTERVAL = 120;
    private final DelayedOrderStatusListener delayedOrderStatusListener;
    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final TestDataFactory testDataFactory;
    private final UserShiftReassignManager userShiftReassignManager;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final PickupPointRepository pickupPointRepository;
    private final TransactionTemplate tt;
    private final SortingCenterService sortingCenterService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final JdbcTemplate jdbcTemplate;
    private final UserShiftManager userShiftManager;

    private UserShift userShift;
    private User user;
    private Shift shift;
    private Order order;
    private Order pickupOrder;

    @BeforeEach
    void init() {
        tt.execute(a -> {
            ClockUtil.initFixed(clock);
            user = testUserHelper.findOrCreateUser(35236L);
            shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(239).getId());
            order = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryServiceId(DELIVERY_SERVICE_ID)
                            .build());

            var userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
            userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            userShiftReassignManager.assign(userShift, order);

            var now = Instant.now(clock);
            var localDate = LocalDate.from(now.atZone(shift.getZoneId()));

            PickupPoint pickupPoint = pickupPointRepository.save(
                    testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, DELIVERY_SERVICE_ID));

            pickupPoint.putScheduleRecord(
                    localDate.getDayOfWeek(),
                    new ru.yandex.market.tpl.core.domain.pickup.LocalTimeInterval(
                            LocalTime.from(now.atZone(shift.getZoneId())),
                            LocalTime.from(now.plus(LOCKER_DELIVERY_INTERVAL, ChronoUnit.MINUTES)
                                    .atZone(shift.getZoneId()))
                    )
            );

            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

            pickupOrder = lockerOrderDataHelper.getPickupOrder(
                    shift, "EXTERNAL_ORDER_ID_1", pickupPoint, geoPoint,
                    OrderFlowStatus.SORTING_CENTER_PREPARED, 2,
                    new LocalTimeInterval(LocalTime.now(clock), LocalTime.now(clock).plusHours(2))
            );

            userShiftReassignManager.assign(userShift, pickupOrder);
            testUserHelper.checkinAndFinishPickup(userShift);
            return 0;
        });
    }

    @Test
    void calcExecutedTimeForClientDeliveryTest() {
        var deliveryTask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get();

        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        DeliveryTaskFailedEvent event = new DeliveryTaskFailedEvent(deliveryTask, failReason, false);

        var executedTime = delayedOrderStatusListener.calcExecutedTime();
        var delay = Duration.between(Instant.now(clock), executedTime);
        assertThat(delay.toMinutes()).isEqualTo(DEFAULT_DELAY_ORDER_STATUS_MINUTES);

    }

    @Test
    void calcExecutedTimeForLockerDeliveryTest() {

        var lockerDeliveryTask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == LOCKER_DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get();

        var subtask = (LockerSubtask) lockerDeliveryTask.streamDeliverySubtasks()
                .findFirst()
                .get();

        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        var event = new LockerDeliverySubtaskFailedEvent(
                userShift,
                failReason,
                subtask
        );

        tt.execute(a -> {
            var executedTime = delayedOrderStatusListener.calcExecutedTime(event);

            var delay = Duration.between(Instant.now(clock), executedTime);
            assertThat(delay.toMinutes())
                    .isEqualTo(LOCKER_DELIVERY_INTERVAL - DEFAULT_DELAY_BEFORE_CLOSE_PICKUP_MINUTES);
            return 0;
        });
    }

    @Test
    void failTaskListenerForPickUpTask() {

        var lockerDeliveryTask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == LOCKER_DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get();


        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        DeliveryTaskFailedEvent event = new DeliveryTaskFailedEvent(
                lockerDeliveryTask,
                failReason,
                false
        );
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DELAYED_CHECKPOINT_ENABLED, Boolean.TRUE
        );
        delayedOrderStatusListener.failTaskListener(event);
        jdbcTemplate.query("SELECT queue_name, task FROM queue_task where queue_name='DELAYED_ORDER_STATUS'", rs -> {
                    Assertions.assertThat(rs.getString(1)).isEqualTo("DELAYED_ORDER_STATUS");
                    Assertions.assertThat(rs.getString(2)).isEqualTo(Long.toString(lockerDeliveryTask.getId()));
                }
        );

    }

    @Test
    void isNeedPrepareShouldReturnTrueForClientDelivery() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DELAYED_CHECKPOINT_ENABLED, Boolean.TRUE
        );
        var deliveryTask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get();

        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        var event = new DeliveryTaskFailedEvent(deliveryTask, failReason, false);

        assertThat(delayedOrderStatusListener.isNeedPrepare(event)).isTrue();
    }

    @Test
    void isNeedPrepareShouldReturnFalseForLockerDelivery() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DELAYED_CHECKPOINT_ENABLED, Boolean.TRUE
        );
        var lockerDeliveryTask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == LOCKER_DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get();


        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        var event = new LockerDeliveryTaskFailedEvent(userShift, failReason, (LockerDeliveryTask) lockerDeliveryTask);

        assertThat(delayedOrderStatusListener.isNeedPrepare(event)).isFalse();
    }

    @Test
    void isNeedPrepareShouldReturnTrueForLockerDelivery() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DELAYED_CHECKPOINT_ENABLED, Boolean.TRUE
        );
        var lockerSubtask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == LOCKER_DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get()
                .streamDeliverySubtasks()
                .findFirst().get();


        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                "Source.COURIER"
        );

        var event = new LockerDeliverySubtaskFailedEvent(userShift, failReason, (LockerSubtask) lockerSubtask);

        assertThat(delayedOrderStatusListener.isNeedPrepare(event)).isTrue();
    }

    @Test
    void isNeedPrepareShouldReturnFalseWhenReasonIsNotPrepared() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DELAYED_CHECKPOINT_ENABLED, Boolean.TRUE
        );
        var deliveryTask = userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getType() == DELIVERY)
                .findFirst().get()
                .streamDeliveryTasks()
                .findFirst().get();

        var failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED,
                "Source.COURIER"
        );

        var event = new DeliveryTaskFailedEvent(deliveryTask, failReason, false);

        assertThat(delayedOrderStatusListener.isNeedPrepare(event)).isFalse();
    }

}
