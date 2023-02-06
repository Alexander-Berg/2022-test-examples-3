package ru.yandex.market.tpl.core.domain.order;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.DeliveryRescheduleDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.CLIENT_REQUEST;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.NO_PASSPORT;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule.fromCourier;

/**
 * @author kukabara
 */
@RequiredArgsConstructor
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderFlowStatusTest extends TplAbstractTest {

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final TestUserHelper userHelper;

    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService userShiftCommandService;
    private final TestDataFactory testDataFactory;
    private final Clock clock;

    private final OrderCommandService orderCommandService;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final OrderRepository orderRepository;
    private final ScManager scManager;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;

    private Order order;
    private Order lockerOrder;
    private User user;
    private UserShift userShift;


    @BeforeEach
    void setUp() {
        transactionTemplate.execute(
                ts -> {
                    LocalDate now = LocalDate.now(clock);
                    user = userHelper.findOrCreateUser(35237L, now);

                    order = orderGenerateService.createOrder(
                            OrderGenerateService.OrderGenerateParam.builder()
                                    .deliveryServiceId(DELIVERY_SERVICE_ID)
                                    .deliveryDate(now)
                                    .flowStatus(OrderFlowStatus.CREATED)
                                    .build()
                    );
                    lockerOrder = orderGenerateService.createOrder(
                            OrderGenerateService.OrderGenerateParam.builder()
                                    .deliveryServiceId(DELIVERY_SERVICE_ID)
                                    .deliveryDate(now)
                                    .flowStatus(OrderFlowStatus.CREATED)
                                    .pickupPoint(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L))
                                    .build()
                    );
                    assertThat(getStatusCodes(order)).containsExactly(1);

                    Shift shift = userHelper.findOrCreateOpenShiftForSc(now, SORTING_CENTER_ID);
                    userShift = userHelper.createEmptyShift(user, shift);
                    userHelper.addDeliveryTaskToShift(user, userShift, order);
                    userHelper.addLockerDeliveryTaskToShift(user, userShift, lockerOrder);

                    jdbcTemplate.update("UPDATE orders SET created_at = :createdAt WHERE id in (:ids)",
                            new MapSqlParameterSource()
                                    .addValue("createdAt", Date.from(Instant.now(clock).minusSeconds(1)))
                                    .addValue("ids", List.of(order.getId(), lockerOrder.getId()))
                    );

                    Instant instant = Instant.now(clock);
                    scManager.createOrders(instant);
                    scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                            List.of(
                                    new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), instant),
                                    new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                            instant),
                                    new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                                            instant)
                            )
                    );
                    scManager.updateOrderStatuses(lockerOrder.getExternalOrderId(), SORTING_CENTER_ID,
                            List.of(
                                    new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), instant),
                                    new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                            instant),
                                    new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                                            instant)
                            )
                    );
                    assertThat(getStatusCodes(order)).containsExactly(1, 10, 20);
                    assertThat(getCheckpoints(order)).containsExactly(1, 10, 20);
                    assertThat(getCheckpoints(lockerOrder)).containsExactly(1, 10);


                    userHelper.checkinAndFinishPickup(userShift);
                    assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48);
                    assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48);
                    assertThat(getCheckpoints(lockerOrder)).containsExactly(1, 10, 30);
                    return null;
                }
        );

    }

    @Test
    void shouldDeliver() {
        userShift.streamDeliveryRoutePoints()
                .forEach(rp -> userHelper.finishDelivery(rp, false));
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 49);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 49);
        transactionTemplate.execute(ts -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            RoutePoint returnRoutePoint = userShift.getCurrentRoutePoint();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
            userShiftCommandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                    returnRoutePoint.getId(), returnRoutePoint.streamReturnTasks().findFirst().orElseThrow().getId()));
            userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
            return null;
        });
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 49, 50);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 49, 50);
    }

    @Test
    void shouldSet90WhenNoContact() {
        OrderDeliveryTask task = userShift.getCurrentRoutePoint().streamTasks(OrderDeliveryTask.class)
                .findFirst()
                .orElseThrow();
        userShiftCommandService.failDeliveryTask(user,
                new UserShiftCommand.FailOrderDeliveryTask(
                        task.getRoutePoint().getUserShift().getId(),
                        task.getRoutePoint().getId(),
                        task.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT, null, null,
                                Source.COURIER)
                ));
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 90, 48);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 90, 48);
    }

    @Test
    void shouldSet46WhenRescheduleByClient() {
        orderCommandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                DeliveryReschedule.fromClient(tomorrowAtHour(10, clock), tomorrowAtHour(12, clock))));

        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 46, 48);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 46, 48);
    }

    @Test
    void shouldSet46WhenRescheduleByNoPassport() {
        orderCommandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                fromCourier(user, new DeliveryRescheduleDto(tomorrowAtHour(12, clock), tomorrowAtHour(14, clock),
                        NO_PASSPORT, ""))));

        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 46, 48);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 46, 48);
    }

    @Test
    void shouldSet46WhenRescheduleByCourierIfClientRequest() {
        orderCommandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                DeliveryReschedule.fromCourier(user, tomorrowAtHour(10, clock), tomorrowAtHour(12, clock),
                        CLIENT_REQUEST)));
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 46, 48);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 46, 48);
    }

    @Test
    void shouldSet47WhenRescheduleByCourier() {
        Instant from = Instant.now(clock);
        orderCommandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                fromCourier(user, tomorrowAtHour(10, clock), tomorrowAtHour(12, clock),
                        OrderDeliveryRescheduleReasonType.DELIVERY_DELAY)));
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 47, 48);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 47, 48);

        assertThat(orderRepository.findByExternalOrderId(order.getExternalOrderId()).get().getOrderFlowStatusUpdatedAt())
                .isAfter(from);
    }

    @Test
    void shouldSet10And60WhenCancelledOrderArriveToScForceCancel() {
        orderCommandService.forceUpdateFlowStatus(new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.CANCELLED
        ));
        Instant instant = Instant.now(clock);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(), instant)
                )
        );
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 60, 10, 60);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 60, 10, 60);
    }

    @Test
    void shouldSet10And60WhenCancelledOrderArriveToSc() {
        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryDate(LocalDate.now(clock))
                        .flowStatus(OrderFlowStatus.CREATED)
                        .build()
        );


        jdbcTemplate.update("UPDATE orders SET created_at = :createdAt WHERE id IN (:ids)",
                new MapSqlParameterSource()
                        .addValue("createdAt", Date.from(Instant.now(clock).minusSeconds(1)))
                        .addValue("ids", List.of(order.getId(), lockerOrder.getId()))
        );

        Instant instant = Instant.now(clock);
        scManager.createOrders(instant);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED_FF.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), instant)
                )
        );

        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.CANCELLED
        ), Source.DELIVERY);

        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(), instant)
                )
        );

        assertThat(getStatusCodes(order)).containsExactly(1, 60, 10, 60);
        assertThat(getCheckpoints(order)).containsExactly(1, 60, 10, 60);
    }

    @Test
    void shouldSet70OnSc170IfReadyForReturn() {
        orderCommandService.forceUpdateFlowStatus(new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.READY_FOR_RETURN
        ));
        Instant instant = Instant.now(clock);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE.getCode(), instant)
                )
        );
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 60, 70);
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 60, 70);
    }

    @Test
    void shouldNotSet70OnSc170IfNotReadyForReturn() {
        Instant instant = Instant.now(clock);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE.getCode(), instant)
                )
        );
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48);
        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48);
    }

    @Test
    void shouldSet70IfScOrderStatus170() {
        userShift.streamDeliveryTasks()
                .forEach(task -> userShiftCommandService.failDeliveryTask(user,
                        new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(), task.getRoutePoint().getId(),
                                task.getId(),
                                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, ""))));
        Instant instant = Instant.now(clock);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE.getCode(), instant)
                )
        );
        transactionTemplate.execute(ts -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            RoutePoint returnRoutePoint = userShift.getCurrentRoutePoint();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
            userShiftCommandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                    returnRoutePoint.getId(), returnRoutePoint.streamReturnTasks().findFirst().orElseThrow().getId()));
            userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
            return null;
        });
        assertThat(getStatusCodes(order)).contains(1, 10, 20, 48, 20, 70);
        assertThat(getCheckpoints(order)).contains(1, 10, 20, 48, 20, 70);
    }

    @Test
    void shouldSet403() {
        userShift.streamDeliveryTasks()
                .forEach(task -> userShiftCommandService.failDeliveryTask(user,
                        new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(), task.getRoutePoint().getId(),
                                task.getId(),
                                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, ""))));
        orderCommandService.forceUpdateFlowStatus(new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.LOST
        ));
        transactionTemplate.execute(ts -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            RoutePoint returnRoutePoint = userShift.getCurrentRoutePoint();
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
            userShiftCommandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                    returnRoutePoint.getId(), returnRoutePoint.streamReturnTasks().findFirst().orElseThrow().getId()));
            userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
            return null;
        });

        assertThat(getStatusCodes(order)).containsExactly(1, 10, 20, 48, 60, 403);
        assertThat(getCheckpoints(order)).containsExactly(1, 10, 20, 48, 60, 403);
    }


    private List<Integer> getStatusCodes(Order order) {
        List<OrderFlowStatusHistory> statuses =
                orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        return StreamEx.of(statuses)
                .map(OrderFlowStatusHistory::getOrderFlowStatusAfter)
                .map(OrderFlowStatus::getCode)
                .collapse(Integer::equals, (c1, c2) -> c1)
                .collect(Collectors.toList());
    }

    private List<Integer> getCheckpoints(Order order) {
        List<OrderFlowStatusHistory> statuses =
                orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        return StreamEx.of(statuses)
                .map(OrderFlowStatusHistory::getDsApiCheckpoint)
                .collapse(Integer::equals, (c1, c2) -> c1)
                .collect(Collectors.toList());
    }

}
