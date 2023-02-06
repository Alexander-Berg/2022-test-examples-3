package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class UserShiftCancelDeliveryTaskOnShiftFinishedTest {

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final OrderManager orderManager;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;

    private User user;
    private Order order;
    private Shift shift;
    private UserShift userShift;
    private OrderDeliveryTask orderDeliveryTask;
    private OrderDeliveryTask orderDeliveryTaskNextDay;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    /**
     * Имитируем ситуацию, когда маршрутизация была выполнена до завершения всех заданий текущей смены.
     */
    @BeforeEach
    void createShifts() {
        user = userHelper.findOrCreateUser(824128L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .build());

        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Instant arrivalTime = Instant.now(clock);
        userShift = repository.findById(
                commandService.createUserShift(
                        UserShiftCommand.Create.builder()
                                .userId(user.getId())
                                .shiftId(shift.getId())
                                .routePoint(helper.taskPrepaid("addr1", order.getId(), arrivalTime, false))
                                .build()
                )).orElseThrow();
        orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();

        var shiftNextDay = userHelper.findOrCreateOpenShift(LocalDate.now(clock).plusDays(1));
        UserShift userShiftNextDay = repository.findById(
                commandService.createUserShift(
                        UserShiftCommand.Create.builder()
                                .userId(user.getId())
                                .shiftId(shiftNextDay.getId())
                                .routePoint(helper.taskPrepaid("addr1", order.getId(), arrivalTime.plus(1,
                                        ChronoUnit.DAYS), false))
                                .build()
                )).orElseThrow();
        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDayAndSelectNext(userShift);
        RoutePoint rp = userShift.getCurrentRoutePoint();
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), rp.getId(),
                helper.getLocationDto(userShift.getId())
        ));

        orderDeliveryTaskNextDay = userShiftNextDay.streamOrderDeliveryTasks().findFirst().orElseThrow();
        assertThat(orderDeliveryTaskNextDay.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
    }

    @Test
    void shouldCancelNextDayTaskIfOrderDelivered() {
        // выдать заказ и завершить смену чтобы заказ получил 50 чекпоинт
        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                userShift.getId(), orderDeliveryTask.getRoutePoint().getId(), orderDeliveryTask.getId(),
                helper.getChequeDto(OrderPaymentType.PREPAID), Instant.now(clock), false, null, Optional.empty()
        ));
        returnOrders();
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        executeCancelDeliveryTaskQueue();

        assertThatNextDayTaskCancelled();
    }

    @Test
    void shouldCancelNextDayTaskIfOrderCancelledByDelivery() {
        orderManager.cancelOrder(order);

        returnOrders();

        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        assertThat(orderDeliveryTaskNextDay.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = orderDeliveryTaskNextDay.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.CANCEL_ORDER);
        assertThat(failReason.getSource()).isEqualTo(Source.DELIVERY);
    }

    @DisplayName("Отмена потерянного заказа без заданий")
    @Test
    void shouldCancelOrderWithTaskReasonOrderWasLost() {
        orderManager.cancelOrder(order);
        returnOrders();
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        orderManager.cancelOrder(order, new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST,
                null, null, Source.OPERATOR));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.LOST);
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
    }

    @DisplayName("Отмена заказа с причиной другое без заданий")
    @Test
    void shouldCancelOrderWithTaskReasonOther() {
        orderManager.cancelOrder(order);
        returnOrders();
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        orderManager.cancelOrder(order, new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.OTHER, null,
                null, Source.OPERATOR));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.CANCELLED);
    }

    @Test
    void shouldCancelNextDayTaskIfOrderCancelledByCourier() {
        commandService.failDeliveryTask(null,
                new UserShiftCommand.FailOrderDeliveryTask(
                        orderDeliveryTask.getRoutePoint().getUserShift().getId(),
                        orderDeliveryTask.getRoutePoint().getId(),
                        orderDeliveryTask.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, null, null, Source.COURIER)
                ));

        returnOrders();

        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        executeCancelDeliveryTaskQueue();

        assertThatNextDayTaskCancelled();
    }

    @Test
    void shouldCancelNextDayTaskIfOrderRescheduledToDayAfterTomorrowByDelivery() {
        LocalTimeInterval localTimeInterval = new LocalTimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
        Interval deliveryInterval = localTimeInterval.toInterval(
                LocalDate.now(clock).plusDays(2), DateTimeUtil.DEFAULT_ZONE_ID);

        orderManager.rescheduleOrder(
                order,
                deliveryInterval,
                Source.DELIVERY);

        returnOrders();

        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        assertThat(orderDeliveryTaskNextDay.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = orderDeliveryTaskNextDay.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
        assertThat(failReason.getSource()).isEqualTo(Source.DELIVERY);
    }

    @Test
    void shouldCancelNextDayTaskIfOrderRescheduledToDayAfterTomorrowByClient() {
        LocalTimeInterval localTimeInterval = new LocalTimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
        Interval deliveryInterval = localTimeInterval.toInterval(
                LocalDate.now(clock).plusDays(2), DateTimeUtil.DEFAULT_ZONE_ID);

        DeliveryReschedule reschedule = new DeliveryReschedule(
                deliveryInterval,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                null,
                Source.CLIENT,
                deliveryInterval.getStart());

        commandService.rescheduleDeliveryTask(
                null,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        orderDeliveryTask.getRoutePoint().getUserShift().getId(),
                        orderDeliveryTask.getRoutePoint().getId(),
                        orderDeliveryTask.getId(),
                        reschedule,
                        Instant.now(clock),
                        userShift.getZoneId()
                ));
        orderManager.rescheduleOrder(
                order,
                deliveryInterval,
                Source.CLIENT);

        returnOrders();

        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        executeCancelDeliveryTaskQueue();

        assertThatNextDayTaskCancelled();
    }

    /**
     * Ситуация.
     * у курьера есть задание 1 активное.
     * Началась маршрутизация, и создала задание 2 на завтра.
     * Потом заказ в этот же день перенесли другому курьеру - задание 3.
     * При закрытии смены первого курьера, заказ созданный маршрутизацией не должен закрыться, потому что вдруг он ещё
     * пригодится.
     */
    @Test
    void shouldNotCancelTaskAfterReassignIfOtherExists() {
        var user2 = userHelper.findOrCreateUser(8233326L);
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        returnOrders();
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));

        assertThat(orderDeliveryTaskNextDay.getStatus().isTerminal()).isFalse();
    }

    private void returnOrders() {
        RoutePoint returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        returnRoutePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        var returnTask = returnRoutePoint.getTasks().get(0);

        if (returnTask.getStatus() != OrderReturnTaskStatus.READY_TO_FINISH) {
            var returnOrdersCommand = new UserShiftCommand.FinishScan(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId(),
                    ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order.getId()))
                            .finishedAt(Instant.now(clock))
                            .build()
            );

            var returnStartCommand = new UserShiftCommand.StartScan(
                    userShift.getId(),
                    returnRoutePoint.getId(),
                    returnTask.getId()
            );

            commandService.startOrderReturn(user, returnStartCommand);
            commandService.finishReturnOrders(user, returnOrdersCommand);
        }
        var returnFinishTaskCommand = new UserShiftCommand.FinishReturnTask(
                userShift.getId(),
                returnRoutePoint.getId(),
                returnTask.getId()
        );
        commandService.finishReturnTask(user, returnFinishTaskCommand);

    }

    private void assertThatNextDayTaskCancelled() {
        assertThat(orderDeliveryTaskNextDay.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = orderDeliveryTaskNextDay.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.TASK_CANCELLED);
        assertThat(failReason.getSource()).isEqualTo(Source.SYSTEM);
    }

    private void executeCancelDeliveryTaskQueue() {
        dbQueueTestUtil.assertQueueHasSingleEvent(
                QueueType.CANCEL_DELIVERY_TASK,
                String.format("UserShift[%s]", userShift.getId()));
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CANCEL_DELIVERY_TASK);
    }

}
