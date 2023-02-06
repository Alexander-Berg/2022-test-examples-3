package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftRescheduleMergedTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;

    private final Clock clock;

    private UserShift userShift;
    private User user;

    @BeforeEach
    void createShiftAndPassOneRoutePoint() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        Order unpaidOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .build());

        Order unpaidOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .build());

        Order unpaidOrder3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .build());

        Order unpaidOrder4 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .build());

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        NewDeliveryRoutePointData task = helper.taskUnpaid("addrPaid", 13, unpaidOrder3.getId());
        NewDeliveryRoutePointData taskMerged = helper.cloneTask(task, task.getExpectedDeliveryTime(),
                unpaidOrder4.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, unpaidOrder1.getId()))
                .routePoint(helper.taskPrepaid("addr3", 14, orderGenerateService.createOrder().getId()))
                .routePoint(task)
                .routePoint(helper.taskUnpaid("addrPaid", 16, unpaidOrder2.getId()))
                .routePoint(taskMerged)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        assertThat(userShift.getRoutePoints()).hasSize(6);

        userHelper.checkin(userShift);

        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, true);

        arriveAtCurrentRoutePoint();
        finishCurrentRoutePoint();
        arriveAtCurrentRoutePoint();

        assertThat(currentRoutePoint().getTasks()).hasSize(2);
    }

    @Test
    void shouldFinishRoutePointOnlyWhenAllTasksAreDone() {
        RoutePoint rp = currentRoutePoint();
        OrderDeliveryTask task2 = (OrderDeliveryTask) rp.getTasks().get(1);

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        failTask(task2);

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        finishCurrentRoutePoint();

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    void shouldSplitRoutePointOnReschedule() {
        RoutePoint rp = currentRoutePoint();
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);
        OrderDeliveryTask task2 = (OrderDeliveryTask) rp.getTasks().get(1);

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(task2.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        assertThat(rp.getExpectedDateTime()).isEqualTo(todayAtHour(13, clock));
        assertThat(userShift.getRoutePoints()).hasSize(6);

        DeliveryTask updatedTask = commandService.rescheduleDeliveryTask(user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(), rp.getId(), task.getId(),
                        DeliveryReschedule.fromCourier(user, todayAtHour(18, clock),
                            todayAtHour(20, clock), OrderDeliveryRescheduleReasonType.DELIVERY_DELAY),
                        todayAtHour(11, clock),
                        userShift.getZoneId()
                ));
        assertThat(updatedTask.getRoutePoint().getId()).isNotEqualTo(rp.getId());

        assertThat(userShift.getRoutePoints()).hasSize(7);
        assertThat(task.getRoutePoint()).isNotEqualTo(task2.getRoutePoint());

        // other task is unchanged
        assertThat(task2.getRoutePoint()).isEqualTo(rp);
        assertThat(rp.getExpectedDateTime()).isEqualTo(todayAtHour(13, clock));

        RoutePoint newRp = task.getRoutePoint();

        assertThat(newRp.getExpectedDateTime()).isAfter(todayAtHour(19, clock).plus(45, ChronoUnit.MINUTES));
        assertThat(newRp.getExpectedDateTime()).isBefore(todayAtHour(20, clock));
        assertThat(newRp.getExpectedDateTime()).isEqualTo(task.getExpectedDeliveryTime());

        assertThat(newRp.getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
    }

    private void failTask(OrderDeliveryTask task) {

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), task.getRoutePoint().getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT, "Недозвон")
        ));

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(task.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.NO_CONTACT);
        assertThat(task.getFailReason().getComment()).isEqualTo("Недозвон");
        assertThat(task.getFailReason().getSource()).isEqualTo(Source.COURIER);
    }

    private void finishCurrentRoutePoint() {
        RoutePoint rp = currentRoutePoint();

        rp.streamTasks(DeliveryTask.class)
                .remove(DeliveryTask::isInTerminalStatus)
                .forEach(task -> {
                            commandService.payOrder(user, new UserShiftCommand.PayOrder(
                                    userShift.getId(), rp.getId(), task.getId(), OrderPaymentType.CASH, null
                            ));
                            commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                                    userShift.getId(), rp.getId(), task.getId(),
                                    helper.getChequeDto(OrderPaymentType.CASH), Instant.now(clock), false,
                                    null, Optional.empty()
                            ));
                        }
                );
        userHelper.finishCallTasksAtRoutePoint(rp);
    }

    private void arriveAtCurrentRoutePoint() {
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), currentRoutePoint().getId(),
                helper.getLocationDto(userShift.getId())
        ));
    }

    private RoutePoint currentRoutePoint() {
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint).isNotNull();
        return Objects.requireNonNull(currentRoutePoint);
    }

}
