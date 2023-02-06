package ru.yandex.market.tpl.core.domain.usershift.calltask;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.call.CallTaskDto;
import ru.yandex.market.tpl.api.model.task.call.CallTasksDto;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.RouteTaskTimes;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.query.usershift.mapper.RoutePointDtoMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAt;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class UserShiftQueryServiceCallTaskTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserPropertyService userPropertyService;
    private final UserShiftRepository repository;
    private final UserShiftQueryService userShiftQueryService;
    private final RoutePointDtoMapper routePointDtoMapper;
    private final Clock clock;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
    }

    @Test
    public void shouldReturnCallTasksForPickupRoutePoint() {
        LocalDate now = LocalDate.now(clock);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var deliveryTask = helper.taskUnpaid("addr1", 9, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(1);

        CallTaskDto callTaskDto = routePointInfo.getCallTasks().iterator().next();
        assertThat(callTaskDto.getTaskStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);
    }


    @Test
    public void shouldReturnCallTasksForPickupRoutePointIfExpectedCallTimeAfterPickupTime() {
        LocalDate now = LocalDate.now(clock);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var deliveryTask = helper.taskUnpaid("addr1", 10, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(1);

        CallTaskDto callTaskDto = routePointInfo.getCallTasks().iterator().next();
        assertThat(callTaskDto.getTaskStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);
    }

    @Test
    public void shouldNotReturnCallTasksForPickupRoutePointIfExpectedCallTimeAfterPickupTimeAfterTreeAttempts() {
        LocalDate now = LocalDate.now(clock);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(now)
                        .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var deliveryTask = helper.taskUnpaid("addr1", 10, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        userHelper.checkin(userShift);

        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(1);

        CallTaskDto callTaskDto = routePointInfo.getCallTasks().iterator().next();
        assertThat(callTaskDto.getTaskStatus()).isEqualTo(CallToRecipientTaskStatus.NOT_CALLED);

        UserShiftCommand.AttemptCallToRecipient attemptCallCommand = new UserShiftCommand.AttemptCallToRecipient(
                userShift.getId(),
                routePointInfo.getId(),
                callTaskDto.getId(),
                "");
        for (int i = 0; i < RoutePoint.MAX_ATTEMPT_CALL_COUNT_AT_ROUTE_POINT; i++) {
            commandService.incrementAttemptCallCount(user, attemptCallCommand);
        }

        routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(0);
    }

    @Test
    @Disabled("пока ограничиваемся одной попыткой недозвона на первой точке")
    public void shouldSortCallTasksByAttemptCallCountAndExpectedCallTime() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .recipientPhone("79998765432")
                        .build());
        Order thirdOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .recipientPhone("79998765431")
                        .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var firstDeliveryTask = helper.taskUnpaid("addr1", 9, firstOrder.getId());
        var secondDeliveryTask = helper.cloneTask(firstDeliveryTask,
                firstDeliveryTask.getExpectedDeliveryTime().plus(1, ChronoUnit.MINUTES), secondOrder.getId());
        var thirdDeliveryTask = helper.cloneTask(secondDeliveryTask,
                secondDeliveryTask.getExpectedDeliveryTime().plus(1, ChronoUnit.MINUTES), thirdOrder.getId());

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
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkinAndFinishPickup(userShift);

        List<CallTaskDto> callTasks;
        callTasks = getCallTasksAfterAction(pickupRoutePoint);
        assertThat(callTasks).isNotNull();
        assertThat(callTasks).hasSize(3);

        long firstCallTaskId = callTasks.get(0).getId();
        long secondCallTaskId = callTasks.get(1).getId();
        long thirdCallTaskId = callTasks.get(2).getId();

        assertThat(callTasks)
                .extracting(ct -> ct.getOrders().iterator().next().getExternalOrderId())
                .containsExactly(
                        firstOrder.getExternalOrderId(),
                        secondOrder.getExternalOrderId(),
                        thirdOrder.getExternalOrderId());

        // после неудачной попытки карточка перемещается в конец списка задач
        commandService.incrementAttemptCallCount(
                user,
                new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                        firstCallTaskId, ""));

        callTasks = getCallTasksAfterAction(pickupRoutePoint);

        assertThat(callTasks)
                .extracting(ct -> ct.getOrders().iterator().next().getExternalOrderId())
                .containsExactly(
                        secondOrder.getExternalOrderId(),
                        thirdOrder.getExternalOrderId(),
                        firstOrder.getExternalOrderId());

        // после успешной попытки карточка должна исчезать из списка
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                        secondCallTaskId, ""));

        callTasks = getCallTasksAfterAction(pickupRoutePoint);

        assertThat(callTasks)
                .extracting(ct -> ct.getOrders().iterator().next().getExternalOrderId())
                .containsExactly(
                        thirdOrder.getExternalOrderId(),
                        firstOrder.getExternalOrderId());

        // после 3-х неудачных попыток карточка должна исчезать из списка
        for (int i = 0; i < 3; i++) {
            commandService.incrementAttemptCallCount(
                    user,
                    new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                            thirdCallTaskId, ""));
        }

        callTasks = getCallTasksAfterAction(pickupRoutePoint);

        assertThat(callTasks)
                .extracting(ct -> ct.getOrders().iterator().next().getExternalOrderId())
                .containsExactly(firstOrder.getExternalOrderId());
    }

    @Test
    void oneAttemptOnDontCallOnNextRoutePoint() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        Order thirdOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var firstDeliveryTask = helper.taskPrepaid(
                "addr1", firstOrder.getId(), todayAt(LocalTime.of(8, 30), clock), false);
        var secondDeliveryTask = helper.taskPrepaid(
                "addr2", secondOrder.getId(), todayAt(LocalTime.of(9, 0), clock), false);
        var thirdDeliveryTask = helper.taskPrepaid(
                "addr3", thirdOrder.getId(), todayAt(LocalTime.of(9, 15), clock), false);

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
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        assertThat(userShift.streamDeliveryTasks().count()).isEqualTo(3);

        List<CallTaskDto> callTasks;
        callTasks = getCallTasksAfterAction(pickupRoutePoint);
        assertThat(callTasks).isNotNull();
        assertThat(callTasks).hasSize(3);

        // дозваниваемся по первым двум заданиям на доставку
        for (int i = 0; i < 2; i++) {
            commandService.successAttemptCall(
                    user,
                    new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                            callTasks.get(i).getId(), ""));
        }
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        // не дозваниваемся по последнему заданию на доставку
        for (int i = 0; i < RoutePoint.MAX_ATTEMPT_CALL_COUNT_AT_ROUTE_POINT; i++) {
            commandService.incrementAttemptCallCount(
                    user,
                    new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                            callTasks.get(2).getId(), ""));
        }
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        // выдать заказ на первой точке
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        userHelper.finishDelivery(currentRoutePoint, null, null, false);

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        callTasks = getCallTasksAfterAction(currentRoutePoint);
        assertThat(callTasks).hasSize(1);

        // не дозвониться по последнему заданию на доставку один раз
        commandService.incrementAttemptCallCount(
                user,
                new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), currentRoutePoint.getId(),
                        callTasks.iterator().next().getId(), ""));
        // карточка не должна больше предлагаться к прозвону на текущей точке

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    void shouldCallTaskAfterNoContact() {
        Order firstOrder = orderGenerateService.createOrder();
        Order secondOrder = orderGenerateService.createOrder();
        Order thirdOrder = orderGenerateService.createOrder();

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var firstDeliveryTask = helper.taskPrepaid(
                "addr1", firstOrder.getId(), todayAt(LocalTime.of(10, 0), clock), false);
        var secondDeliveryTask = helper.taskPrepaid(
                "addr2", secondOrder.getId(), todayAt(LocalTime.of(11, 10), clock), false);
        var thirdDeliveryTask = helper.taskPrepaid(
                "addr3", thirdOrder.getId(), todayAt(LocalTime.of(12, 0), clock), false);

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
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        assertThat(userShift.streamDeliveryTasks().count()).isEqualTo(3);

        List<CallTaskDto> callTasks;
        callTasks = getCallTasksAfterAction(pickupRoutePoint);
        assertThat(callTasks).isNotNull();
        assertThat(callTasks).hasSize(1);

        // не дозвониться по первой точке, но все равно туда приехать и выдать заказ
        for (int i = 0; i < RoutePoint.MAX_ATTEMPT_CALL_COUNT_AT_ROUTE_POINT; i++) {
            commandService.incrementAttemptCallCount(
                    user,
                    new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                            callTasks.iterator().next().getId(), ""));
        }
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        // выдать заказ на первой точке
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        userHelper.finishDelivery(currentRoutePoint, null, OrderPaymentType.CASH, false);

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        // должно отобразиться задание на звонок по следующей точке
        callTasks = getCallTasksAfterAction(currentRoutePoint);
        assertThat(callTasks).hasSize(1);

        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), currentRoutePoint.getId(),
                        callTasks.iterator().next().getId(), ""));

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }


    @Test
    public void shouldReturnLateContactCallTask() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(14, clock));
        var deliveryTask = helper.taskUnpaid("addr1", 15, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        OrderDeliveryTask orderDeliveryTask = userShift.streamOrderDeliveryTasks()
                .findFirst()
                .orElseThrow();

        List<CallTaskDto> callTasks = getCallTasksAfterAction(pickupRoutePoint);
        assertThat(callTasks).isNotNull();
        assertThat(callTasks).hasSize(1);

        CallTaskDto callTaskDto = callTasks.iterator().next();

        assertThat(callTaskDto.getLateContact()).isTrue();
        long expectedCountdown = ChronoUnit.MINUTES.between(orderDeliveryTask.getRoutePoint().getExpectedDateTime(),
                order.getDelivery().getDeliveryIntervalTo());
        assertThat(callTaskDto.getCountdownInMinute()).isEqualTo(expectedCountdown);
    }

    @Disabled("MARKETTPL-1026")
    @Test
    public void shouldReopenCallTaskAfterReroutingWithLateContact() {
        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("09:00-14:00"))
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("09:00-14:00"))
                .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var deliveryTask1 = helper.taskUnpaid("addr1", 9, order1.getId());
        var deliveryTask2 = helper.taskUnpaid("addr2", 9, order2.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask1)
                .routePoint(deliveryTask2)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkinAndFinishPickup(userShift);

        List<CallTaskDto> callTasks = getCallTasksAfterAction(pickupRoutePoint);
        assertThat(callTasks).isNotNull();
        assertThat(callTasks).hasSize(2);

        // успешно дозвониться по ближайшим доставкам
        callTasks.forEach(t -> {
            commandService.successAttemptCall(
                    user,
                    new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), pickupRoutePoint.getId(),
                            t.getId(), ""));
        });

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(),
                currentRoutePoint.getId(),
                helper.getLocationDto(userShift.getId())));

        long countDeliveryTasksOnFirstDeliveryRoutePoint = currentRoutePoint.streamDeliveryTasks().count();
        assertThat(countDeliveryTasksOnFirstDeliveryRoutePoint).isEqualTo(1);
        OrderDeliveryTask orderDeliveryTask = currentRoutePoint.streamTasks(OrderDeliveryTask.class)
                .findFirst()
                .orElseThrow();
        assertThat(orderDeliveryTask.getOrderId()).isEqualTo(order1.getId());

        // случается ремаршрутизация - второй заказ не успеваем доставить в выбранный интервал
        Map<Long, RouteTaskTimes> taskTimes = userShift.streamOrderDeliveryTasks()
                .mapToEntry(BaseJpaEntity::getId,
                        t -> t.getRoutePoint().getAddressString().equals("addr1")
                                ? t.getRoutePoint().getExpectedDateTime().plusSeconds(120) :
                                t.getRoutePoint().getExpectedDateTime().plus(6, ChronoUnit.HOURS))
                .mapValues(r -> new RouteTaskTimes(r, r.plusSeconds(900)))
                .toMap();

        var updateCommand = new UserShiftCommand.UpdateRoute(
                userShift.getId(),
                userShift.getProcessingId(),
                SimpleStrategies.BY_DATE_INTERVAL_MERGE,
                taskTimes);
        commandService.updateRoute(updateCommand);

        List<CallTaskDto> callTasksAfterAction = getCallTasksAfterAction(currentRoutePoint);
        assertThat(callTasksAfterAction).hasSize(1);
        CallTaskDto callTaskLateContact = callTasksAfterAction.stream().findFirst().orElseThrow();
        assertThat(callTaskLateContact.getTaskStatus()).isEqualTo(CallToRecipientTaskStatus.RECALL_REQUIRED);

        // позвонить второй раз
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(userShift.getId(), currentRoutePoint.getId(),
                        callTaskLateContact.getId(), ""));

        // еще раз эмулируем получение результатов маршрутизации
        commandService.updateRoute(updateCommand);

        CallToRecipientTask callToRecipientTaskLateContactSuccessCalled =
                userShift.streamCallTasks().filter(t -> t.getId() == callTaskLateContact.getId()).findFirst().orElseThrow();

        assertThat(callToRecipientTaskLateContactSuccessCalled.getStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);
    }

    private List<CallTaskDto> getCallTasksAfterAction(RoutePoint pickupRoutePoint) {
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, pickupRoutePoint.getId());
        return routePointInfo.getCallTasks();
    }

    @Test
    public void shouldReturnCallTasksForDeliveryRoutePoint() {
        Order order = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();


        var pickupTask = helper.taskOrderPickup(todayAtHour(9, clock));
        var deliveryTask = helper.taskPrepaid("addr1", 10, order.getId());
        var deliveryTask2 = helper.taskPrepaid("addr2", 11, order2.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask)
                .routePoint(deliveryTask2)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);
        successAttemptCallOnCurrentRoutePoint(userShift);

        RoutePoint routePoint = userShift.getCurrentRoutePoint();
        assertThat(routePoint.getType()).isEqualTo(RoutePointType.DELIVERY);
        userHelper.finishDelivery(routePoint, null, OrderPaymentType.CASH, false);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, routePoint.getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(1);
    }

    @Test
    public void shouldReturnCallTaskForOneOrderForMultiOrder() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .flowStatus(OrderFlowStatus.CREATED)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress my_address = new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat());

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(my_address)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(my_address)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order2, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .routePoint(delivery2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(userShift.streamCallTasks().count()).isEqualTo(1);

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, List.of(order1.getId()), List.of(), true, false);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user,
                userShift.getCurrentRoutePoint().getId());

        assertThat(routePointInfo.getCallTasks()).isNotNull();
        assertThat(routePointInfo.getCallTasks()).hasSize(1);
        CallTaskDto callTask = routePointInfo.getCallTasks().iterator().next();

        assertThat(callTask.getOrders()).hasSize(1);
        assertThat(callTask.getOrders().iterator().next())
                .extracting(OrderDto::getExternalOrderId)
                .isEqualTo(order1.getExternalOrderId());
        assertThat(callTask.getMultiOrderId()).isEqualTo(String.valueOf(callTask.getId()));
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

    @Test
    void shouldReturnCallTaskDto() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());

        var deliveryTask = helper.taskUnpaid("addr1", 12, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        commandService.switchActiveUserShift(user, userShift.getId());

        List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
        assertThat(callToRecipientTasks).hasSize(1);

        CallToRecipientTask callTask = callToRecipientTasks.iterator().next();

        CallTaskDto callTaskDto = userShiftQueryService.getCallTask(user, callTask.getId());

        assertThat(callTaskDto.getId()).isEqualTo(callTask.getId());
    }

    @Test
    public void shouldReturnListCallTasks() {
        Order order = orderGenerateService.createOrder();
        Order anotherOrder = orderGenerateService.createOrder();

        var deliveryTask1 = helper.taskUnpaid("addr1", 12, order.getId());
        var deliveryTask2 = helper.taskUnpaid("addr2", 14, anotherOrder.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryTask1)
                .routePoint(deliveryTask2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        commandService.switchActiveUserShift(user, userShift.getId());

        List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
        assertThat(callToRecipientTasks).hasSize(2);

        CallTasksDto callTasks = userShiftQueryService.getCallTasks(user, Boolean.FALSE);

        assertThat(callTasks.getTasks())
                .extracting(t -> t.getExternalOrderIds().iterator().next())
                .containsExactly(
                        anotherOrder.getExternalOrderId(),
                        order.getExternalOrderId()
                );
    }

    @Test
    public void shouldNotReturnItemCountNotStartedAfterPickup() {
        LocalDate now = LocalDate.now(clock);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .places(
                                List.of(
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "P001"))
                                                .build(),
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", "P002"))
                                                .build()
                                )
                        )
                        .deliveryDate(now)
                        .build());

        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var deliveryTask = helper.taskUnpaid("addr1", 9, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(deliveryTask)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        RoutePointSummaryDto.ItemCount itemCount = routePointDtoMapper.itemCountRoutePointScan(pickupRoutePoint);
        assertThat(itemCount.getNotStarted()).isEqualTo(2);
        assertThat(itemCount.getSuccess()).isEqualTo(0);
        assertThat(itemCount.getFail()).isEqualTo(0);

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true);

        itemCount = routePointDtoMapper.itemCountRoutePointScan(pickupRoutePoint);
        assertThat(itemCount.getNotStarted()).isNull();
        assertThat(itemCount.getSuccess()).isEqualTo(2);
        assertThat(itemCount.getFail()).isEqualTo(0);

        RoutePoint returnRoutePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();

        itemCount = routePointDtoMapper.itemCountRoutePointScan(returnRoutePoint);
        assertThat(itemCount.getNotStarted()).isEqualTo(2);
        assertThat(itemCount.getSuccess()).isEqualTo(0);
        assertThat(itemCount.getFail()).isEqualTo(0);

    }
}
