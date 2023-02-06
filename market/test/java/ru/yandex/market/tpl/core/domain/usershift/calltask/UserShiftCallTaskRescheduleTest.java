package ru.yandex.market.tpl.core.domain.usershift.calltask;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingRescheduleDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftCallTaskRescheduleTest {

    private static final String COURIER_NOTES = "test";

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final TrackingService trackingService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final UserPropertyService userPropertyService;
    private final UserRepository userRepository;
    private final Clock clock;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
        user = userRepository.save(user);
    }

    @Test
    public void shouldCancelCallTaskAfterRescheduleFromTrackingByUser() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
        Order anotherOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());

        var deliveryTask = helper.taskUnpaid("addr1", 12, order.getId());
        var anotherDeliveryTask = helper.taskUnpaid("addr2", 16, anotherOrder.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryTask)
                .routePoint(anotherDeliveryTask)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(userShiftId).orElseThrow();

        userHelper.checkinAndFinishPickup(userShift);

        String trackingLink = trackingService.getTrackingLinkByOrder(anotherOrder.getExternalOrderId()).orElseThrow();

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(22, clock);

        // перенести заказ через экран Где курьер
        trackingService.rescheduleOrder(
                trackingLink,
                new TrackingRescheduleDto(from, to), null);

        OrderDeliveryTask orderDeliveryTask =
                userShift.streamOrderDeliveryTasks().filter(odt -> Objects.equals(odt.getOrderId(),
                                anotherOrder.getId()))
                        .findFirst()
                        .orElseThrow();

        assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        OrderDeliveryFailReason failReason = orderDeliveryTask.getFailReason();
        assertThat(failReason).isNotNull();
        assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
        assertThat(orderDeliveryTask.getRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        CallToRecipientTask callTask = orderDeliveryTask.getCallToRecipientTask();
        assertThat(callTask.getAttemptCount()).isEqualTo(0);

        assertThat(callTask.getStatus()).isEqualTo(CallToRecipientTaskStatus.CLOSED);
    }

    @Test
    public void shouldSuccessRescheduleToTomorrow() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();
        Order order3 = orderGenerateService.createOrder();

        var taskToMerge1 = helper.taskUnpaid("addr1", 9, order1.getId());
        var taskToMerge2 = helper.taskUnpaid("addr2", 9, order2.getId());
        var taskToMerge3 = helper.taskUnpaid("addr3", 13, order3.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(taskToMerge1)
                .routePoint(taskToMerge2)
                .routePoint(taskToMerge3)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        CallToRecipientTask callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        userShift.streamCallTasks()
                .limit(2)
                .forEach(callToRecipientTask -> {
                    Instant from = tomorrowAtHour(18, clock);
                    Instant to = tomorrowAtHour(22, clock);

                    UserShiftCommand.RescheduleOrderDeliveryTask rescheduleCommand =
                            new UserShiftCommand.RescheduleOrderDeliveryTask(
                                    userShift.getId(),
                                    pickupRoutePoint.getId(),
                                    callToRecipientTask.getId(),
                                    DeliveryReschedule.fromCourier(
                                            user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                                            COURIER_NOTES),
                                    DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                            LocalTime.of(18, 22))),
                                    userShift.getZoneId()
                            );

                    Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                            List.of(LocalTimeInterval.valueOf("18:00-22:00")));

                    commandService.rescheduleDeliveryAfterCall(user, rescheduleCommand, intervals);

                    OrderDeliveryTask orderDeliveryTask = callTask.getOrderDeliveryTasks().iterator().next();
                    RoutePoint routePoint = orderDeliveryTask.getRoutePoint();
                    assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
                    assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
                });
    }

    @Test
    public void shouldFinishPickupRoutePointAfterRescheduleToCurrentDay() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        var task1 = helper.taskUnpaid("addr1", 12, order1.getId());
        var task2 = helper.taskUnpaid("addr2", 12, order2.getId());
        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(12, clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(task1)
                .routePoint(task2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        List<CallToRecipientTask> unfinishedCallTasks = pickupRoutePoint
                .getUnfinishedCallTasks();

        // первый звонок успешный
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        unfinishedCallTasks.get(0).getId(),
                        COURIER_NOTES));
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        // после второго - перенос в рамках текущего дня
        Instant from = todayAtHour(18, clock);
        Instant to = todayAtHour(22, clock);

        UserShiftCommand.RescheduleOrderDeliveryTask rescheduleCommand =
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        unfinishedCallTasks.get(1).getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 22))),
                        userShift.getZoneId()
                );

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock),
                List.of(LocalTimeInterval.valueOf("18:00-22:00")));

        commandService.rescheduleDeliveryAfterCall(user, rescheduleCommand, intervals);

        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    public void recallAfterRescheduleToCurrentDay() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        var task1 = helper.taskPrepaid("addr1", order1.getId(), DateTimeUtil.todayAt(LocalTime.of(9, 0), clock),
                false);
        var task2 = helper.taskPrepaid("addr2", order2.getId(), DateTimeUtil.todayAt(LocalTime.of(9, 30), clock),
                false);
        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(9, clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(task1)
                .routePoint(task2)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        List<CallToRecipientTask> unfinishedCallTasks =
                pickupRoutePoint.getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(2);

        // перенос первой доставки в рамках дня
        Instant from = todayAtHour(18, clock);
        Instant to = todayAtHour(22, clock);

        CallToRecipientTask callToRecipientTask = unfinishedCallTasks.get(0);
        UserShiftCommand.RescheduleOrderDeliveryTask rescheduleCommand =
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        callToRecipientTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(11, 20))),
                        userShift.getZoneId());

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock),
                List.of(LocalTimeInterval.valueOf("18:00-22:00")));
        commandService.rescheduleDeliveryAfterCall(user, rescheduleCommand, intervals);

        // должен появиться звонок по второй точке в маршруте
        unfinishedCallTasks = pickupRoutePoint.getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(1);

        CallToRecipientTask callToRecipientTask2 = unfinishedCallTasks.get(0);
        assertThat(callToRecipientTask2.getOrderDeliveryTasks().iterator().next().getOrderId())
                .isEqualTo(order2.getId());
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        callToRecipientTask2.getId(),
                        COURIER_NOTES));

        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        // выдать заказ на следующей точке
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        OrderDeliveryTask orderDeliveryTask2 = currentRoutePoint.streamTasks(OrderDeliveryTask.class)
                .findFirst()
                .orElseThrow();
        assertThat(orderDeliveryTask2.getOrderId()).isEqualTo(order2.getId());

        userHelper.finishDelivery(currentRoutePoint, null, null, false);

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        // должен появиться звонок по перенесенному заказу
        unfinishedCallTasks = currentRoutePoint.getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(1);
        CallToRecipientTask callToRecipientTask1 = unfinishedCallTasks.iterator().next();

        assertThat(callToRecipientTask1.getOrderDeliveryTasks().iterator().next().getOrderId())
                .isEqualTo(order1.getId());
        assertThat(callToRecipientTask1.getStatus()).isEqualTo(CallToRecipientTaskStatus.RECALL_REQUIRED);

        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        callToRecipientTask1.getId(),
                        COURIER_NOTES));

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    void shouldReturnCallTaskAfterRescheduleToCurrentDayFromDeliveryTask() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        var task1 = helper.taskPrepaid("addr1", 12, order1.getId());
        var task2 = helper.taskPrepaid("addr2", 13, order2.getId());
        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(11, clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(task1)
                .routePoint(task2)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        List<CallToRecipientTask> unfinishedCallTasks = pickupRoutePoint.getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(1);

        // не дозвонились
        for (int i = 0; i < RoutePoint.MAX_ATTEMPT_CALL_COUNT_AT_ROUTE_POINT; i++) {
            commandService.incrementAttemptCallCount(user, new UserShiftCommand.AttemptCallToRecipient(
                    userShift.getId(),
                    pickupRoutePoint.getId(),
                    unfinishedCallTasks.iterator().next().getId(),
                    ""
            ));
        }
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        // но все равно едем по адресу
        // сделать перенос заказа в рамках дня из задания на доставку
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        OrderDeliveryTask orderDeliveryTask = currentRoutePoint.streamTasks(OrderDeliveryTask.class)
                .findFirst()
                .orElseThrow();
        assertThat(orderDeliveryTask.getOrderId()).isEqualTo(order1.getId());

        userHelper.arriveAtRoutePoint(currentRoutePoint);

        Instant from = todayAtHour(18, clock);
        Instant to = todayAtHour(22, clock);

        UserShiftCommand.RescheduleOrderDeliveryTask rescheduleCommand =
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        orderDeliveryTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 20))),
                        userShift.getZoneId());

        commandService.rescheduleDeliveryTask(user, rescheduleCommand);

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(currentRoutePoint.streamDeliveryTasks().count()).isEqualTo(0);

        // должен появиться звонок по второй точке в маршруте
        unfinishedCallTasks = currentRoutePoint.getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(1);


        CallToRecipientTask callToRecipientTask2 = unfinishedCallTasks.get(0);
        assertThat(callToRecipientTask2.getOrderDeliveryTasks().iterator().next().getOrderId())
                .isEqualTo(order2.getId());
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        callToRecipientTask2.getId(),
                        COURIER_NOTES));

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    void shouldFinishCurrentRoutePointInStatusInTransitAfterSuccessCall() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        var task1 = helper.taskPrepaid("addr1", 12, order1.getId());
        var task2 = helper.taskPrepaid("addr2", 13, order2.getId());
        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(11, clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(task1)
                .routePoint(task2)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        List<CallToRecipientTask> unfinishedCallTasks = pickupRoutePoint
                .getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(1);

        // не дозвонились
        for (int i = 0; i < RoutePoint.MAX_ATTEMPT_CALL_COUNT_AT_ROUTE_POINT; i++) {
            commandService.incrementAttemptCallCount(user, new UserShiftCommand.AttemptCallToRecipient(
                    userShift.getId(),
                    pickupRoutePoint.getId(),
                    unfinishedCallTasks.iterator().next().getId(),
                    ""
            ));
        }
        assertThat(pickupRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        // но все равно едем по адресу
        // сделать перенос заказа на следующий день
        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        OrderDeliveryTask orderDeliveryTask = currentRoutePoint.streamTasks(OrderDeliveryTask.class)
                .findFirst()
                .orElseThrow();
        assertThat(orderDeliveryTask.getOrderId()).isEqualTo(order1.getId());
        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(22, clock);

        UserShiftCommand.RescheduleOrderDeliveryTask rescheduleCommand =
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        orderDeliveryTask.getId(),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST, COURIER_NOTES),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 22))),
                        userShift.getZoneId());

        commandService.rescheduleDeliveryTask(user, rescheduleCommand);

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        // должен появиться звонок по второй точке в маршруте
        unfinishedCallTasks = currentRoutePoint
                .getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).hasSize(1);


        CallToRecipientTask callToRecipientTask2 = unfinishedCallTasks.get(0);
        assertThat(callToRecipientTask2.getOrderDeliveryTasks().iterator().next().getOrderId())
                .isEqualTo(order2.getId());
        commandService.successAttemptCall(
                user,
                new UserShiftCommand.AttemptCallToRecipient(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        callToRecipientTask2.getId(),
                        COURIER_NOTES));

        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
    }

    @Test
    void emptyCallTasksListForDeliveryBySellerUser() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        var task1 = helper.taskPrepaid("addr1", 12, order1.getId());
        var task2 = helper.taskPrepaid("addr2", 13, order2.getId());
        var pickupTask = helper.taskOrderPickup(DateTimeUtil.todayAtHour(11, clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(pickupTask)
                .routePoint(task1)
                .routePoint(task2)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();
        userPropertyService.addPropertyToUser(user, UserProperties.DBS_CALL_TO_RECIPIENT_DISABLED, true);

        userHelper.checkin(userShift);
        userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);

        List<CallToRecipientTask> unfinishedCallTasks = pickupRoutePoint.getUnfinishedCallTasks();
        assertThat(unfinishedCallTasks).isEmpty();
    }
}
