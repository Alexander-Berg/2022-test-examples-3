package ru.yandex.market.tpl.core.domain.usershift.calltask;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.call.CallTaskDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAt;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class UserShiftQueryServiceCallTaskOnPickupRoutePointTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final UserShiftQueryService userShiftQueryService;
    private final UserPropertyService userPropertyService;
    private final Clock clock;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
    }

    @DisplayName("При раннем заборе заказов, отдаем на прозвон все задачи от начала первого клиентского интервала + 1" +
            " час")
    @Test
    public void shouldReturnCallTasksForEarlyPickupRoutePoint() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        Order thirdOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var firstDeliveryTask = helper.taskPrepaid(
                "addr1", firstOrder.getId(), todayAt(LocalTime.of(9, 5), clock), false);
        var secondDeliveryTask = helper.taskPrepaid(
                "addr2", secondOrder.getId(), todayAt(LocalTime.of(9, 20), clock), false);
        var thirdDeliveryTask = helper.taskPrepaid(
                "addr3", thirdOrder.getId(), todayAt(LocalTime.of(10, 40), clock), false);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .routePoint(thirdDeliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(2);
    }

    @DisplayName("При заборе заказов, отображать все задания на звонки которые необходимо совершить " +
            "в течение часа после прибытия на первую точку")
    @Test
    public void shouldReturnCallTasksForPickupRoutePoint() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        Order thirdOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(todayAtHour(9, clock));
        var firstDeliveryTask = helper.taskPrepaid(
                "addr1", firstOrder.getId(), todayAt(LocalTime.of(9, 58), clock), false);
        var secondDeliveryTask = helper.taskPrepaid(
                "addr2", secondOrder.getId(), todayAt(LocalTime.of(10, 20), clock), false);
        var thirdDeliveryTask = helper.taskPrepaid(
                "addr3", thirdOrder.getId(), todayAt(LocalTime.of(12, 40), clock), false);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .routePoint(thirdDeliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        RoutePoint pickupRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(pickupRoutePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());

        List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks()
                .limit(2)
                .toList();

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(2);
        assertThat(routePointInfo.getCallTasks())
                .extracting(CallTaskDto::getId)
                .containsExactlyInAnyOrder(callToRecipientTasks.get(0).getId(), callToRecipientTasks.get(1).getId());
    }


    @DisplayName("При выдаче заказов отображаем задачи на звонок доставка по которым должна быть в течение часа")
    @Test
    public void callTaskOnDeliveryRoutePoint() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        Order thirdOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(todayAtHour(9, clock));
        var firstDeliveryTask = helper.taskPrepaid(
                "addr1", firstOrder.getId(), todayAt(LocalTime.of(10, 5), clock), false);
        var secondDeliveryTask = helper.taskPrepaid(
                "addr2", secondOrder.getId(), todayAt(LocalTime.of(11, 5), clock), false);
        var thirdDeliveryTask = helper.taskPrepaid(
                "addr3", thirdOrder.getId(), todayAt(LocalTime.of(11, 10), clock), false);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .routePoint(thirdDeliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        successAttemptCallOnCurrentRoutePoint(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint.getType()).isEqualTo(RoutePointType.DELIVERY);
        userHelper.finishDelivery(currentRoutePoint, null, null, false);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, currentRoutePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(1);
    }


    @DisplayName("При выдаче заказов, если в течение часа нет доставок - показывать задачу на звонок по следующей " +
            "точке и только по ней")
    @Test
    public void callTaskOnDeliveryRoutePointLargeIntervalBetweenRoutePoints() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        Order thirdOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(todayAtHour(9, clock));
        var firstDeliveryTask = helper.taskPrepaid(
                "addr1", firstOrder.getId(), todayAt(LocalTime.of(10, 5), clock), false);
        var secondDeliveryTask = helper.taskPrepaid(
                "addr2", secondOrder.getId(), todayAt(LocalTime.of(12, 30), clock), false);
        var thirdDeliveryTask = helper.taskPrepaid(
                "addr3", thirdOrder.getId(), todayAt(LocalTime.of(12, 40), clock), false);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(firstDeliveryTask)
                .routePoint(secondDeliveryTask)
                .routePoint(thirdDeliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        successAttemptCallOnCurrentRoutePoint(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        assertThat(currentRoutePoint.getType()).isEqualTo(RoutePointType.DELIVERY);

        userHelper.finishDelivery(currentRoutePoint, null, null, false);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, currentRoutePoint.getId());

        List<CallTaskDto> callTasks = routePointInfo.getCallTasks();
        assertThat(callTasks).isNotNull();
        assertThat(callTasks).hasSize(1);
        CallTaskDto callTask = callTasks.iterator().next();
        OrderDto order = callTask.getOrders().iterator().next();
        assertThat(order.getExternalOrderId()).isEqualTo(secondOrder.getExternalOrderId());
    }


    private void successAttemptCallOnCurrentRoutePoint(UserShift userShift) {
        RoutePointDto routePointInfo =
                userShiftQueryService.getRoutePointInfo(user, userShift.getCurrentRoutePoint().getId());

        routePointInfo.getCallTasks()
                .forEach(callTaskDto -> commandService.successAttemptCall(
                        user,
                        new UserShiftCommand.AttemptCallToRecipient(
                                userShift.getId(),
                                routePointInfo.getId(),
                                callTaskDto.getId(),
                                "")));
    }
}
