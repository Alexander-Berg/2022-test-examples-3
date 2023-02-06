package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.usershift.ArriveAtRoutePointService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CASH;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.PREPAID;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class OrderReturnTaskTest {
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final ArriveAtRoutePointService arriveAtRoutePointService;

    private User user;
    private User user2;
    private User user3;
    private User user4;
    private UserShift userShiftEmpty;
    private UserShift userShiftWithCash;
    private UserShift userShiftWithOrderForReturn;
    private UserShift userShiftWithOrderForReturnAndCash;
    private Order orderPaidByCard;
    private Order orderPaidByCash;
    private Order orderPaidByCash3;
    private Order orderPaidByCash4;
    private Order orderPaidByCash5;

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        user = userHelper.findOrCreateUser(356L);
        user2 = userHelper.findOrCreateUser(3523L);
        user3 = userHelper.findOrCreateUser(352L);
        user4 = userHelper.findOrCreateUser(35L);

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        orderPaidByCard = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(PREPAID)
                .build());
        userShiftEmpty = initUserShiftMethod(shift, user, orderPaidByCard, null);

        orderPaidByCash = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(CASH)
                .build());

        userShiftWithCash = initUserShiftMethod(shift, user2, orderPaidByCash, null);

        orderPaidByCash3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(CASH)
                .build());
        userShiftWithOrderForReturn = initUserShiftMethod(shift, user3, orderPaidByCash3, null);

        orderPaidByCash4 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(CASH)
                .build());
        orderPaidByCash5 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(CASH)
                .build());
        userShiftWithOrderForReturnAndCash = initUserShiftMethod(shift, user4, orderPaidByCash4, orderPaidByCash5);
    }


    @Test
    void completeReturnEmptyTask() {
        var userShift = userShiftEmpty;
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), false);
        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        arriveAtRoutePointService.arrivedAtRoutePoint(returnRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId()), user);
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);

    }

    @Test
    void completeReturnOrderTaskWithoutOrdersForReturnWithCash() {
        var userShift = userShiftWithCash;
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userHelper.finishDelivery(Objects.requireNonNull(userShift.getCurrentRoutePoint()), false);
        orderPaidByCash.getCheques().iterator().next().setTotal(orderPaidByCash.getTotalPrice());
        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        arriveAtRoutePointService.arrivedAtRoutePoint(returnRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId()),
                user2);

        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.AWAIT_CASH_RETURN);
        commandService.finishReturnCash(user2, new UserShiftCommand.ReturnCash(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);
        commandService.finishReturnTask(user2, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    void completeReturnOrderTaskWithOrdersForReturnWithoutCash() {
        var userShift = userShiftWithOrderForReturn;
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        RoutePoint rp = userShift.getCurrentRoutePoint();
        OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();
        commandService.failDeliveryTask(user3, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "")
        ));
        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        arriveAtRoutePointService.arrivedAtRoutePoint(returnRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId()), user3);
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);
        commandService.finishReturnCash(user3, new UserShiftCommand.ReturnCash(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);
        commandService.finishReturnTask(user3, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }

    @Test
    void completeReturnOrderTaskWithOrdersForReturnWithCash() {
        var userShift = userShiftWithOrderForReturnAndCash;
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        userHelper.finishPickupAtStartOfTheDay(userShift);
        assertThat(userShift.streamReturnRoutePoints().count()).isEqualTo(1);
        var rp = userShift.getCurrentRoutePoint();
        userHelper.finishDelivery(Objects.requireNonNull(rp), false);
        orderPaidByCash4.getCheques().iterator().next().setTotal(orderPaidByCash4.getTotalPrice());

        rp = userShift.getCurrentRoutePoint();
        var task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        commandService.failDeliveryTask(user4, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "")
        ));
        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        arriveAtRoutePointService.arrivedAtRoutePoint(returnRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId()),
                user4);
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutArriving(userShift, true);

        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.AWAIT_CASH_RETURN);
        commandService.finishReturnCash(user4, new UserShiftCommand.ReturnCash(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);
        commandService.finishReturnTask(user4, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);

    }

    @Test
    void testFinishReturnCashIsIdempotent() {
        var userShift = userShiftWithOrderForReturnAndCash;
        userHelper.finishPickupAtStartOfTheDay(userShift);
        var rp = userShift.getCurrentRoutePoint();
        userHelper.finishDelivery(Objects.requireNonNull(rp), false);
        orderPaidByCash4.getCheques().iterator().next().setTotal(orderPaidByCash4.getTotalPrice());

        rp = userShift.getCurrentRoutePoint();
        var task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();

        commandService.failDeliveryTask(user4, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "")
        ));
        var returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        arriveAtRoutePointService.arrivedAtRoutePoint(returnRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId()),
                user4);
        var taskForReturn = returnRoutePoint.streamReturnTasks().findFirst().orElseThrow();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutArriving(userShift, true);

        commandService.finishReturnCash(user4, new UserShiftCommand.ReturnCash(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishReturnCash(user4, new UserShiftCommand.ReturnCash(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));

        commandService.finishReturnTask(user4, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));
        commandService.finishReturnCash(user4, new UserShiftCommand.ReturnCash(userShift.getId(),
                returnRoutePoint.getId(), taskForReturn.getId()));

        assertThat(returnRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(taskForReturn.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
    }


    private UserShift initUserShiftMethod(Shift shift, User user, Order order, Order order1) {
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        if (order.isPrepaid()) {
            builder.routePoint(helper.taskPrepaid("addr ", 12, order.getId()));
        } else {
            builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));

        }
        if (order1 != null) {
            builder.routePoint(helper.taskUnpaid("addrNexr", 13, order1.getId()));
        }

        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        var userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        return userShift;
    }
}
