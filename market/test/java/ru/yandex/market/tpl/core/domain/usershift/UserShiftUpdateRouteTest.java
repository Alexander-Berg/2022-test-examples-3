package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCollectDropshipRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCommonRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.RouteTaskTimes;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
@RequiredArgsConstructor
class UserShiftUpdateRouteTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;

    private final MovementGenerator movementGenerator;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final TestDataFactory testDataFactory;

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;

    private User user;
    private UserShift userShift;

    long orderToSplit1;
    long orderToSplit2;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.of(1993, 1, 1, 0, 0, 0));
        user = userHelper.findOrCreateUser(35239L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var taskToMerge1 = helper.taskUnpaid("addr1", 12, orderGenerateService.createOrder().getId());
        var taskToMerge2 = helper.cloneTask(taskToMerge1,
                taskToMerge1.getExpectedDeliveryTime().plus(4, ChronoUnit.MINUTES),
                orderGenerateService.createOrder().getId());
        orderToSplit1 = orderGenerateService.createOrder().getId();
        orderToSplit2 = orderGenerateService.createOrder().getId();
        var taskToMerge3 = helper.cloneTask(taskToMerge1, taskToMerge1.getExpectedDeliveryTime(), orderToSplit1);
        var taskToMerge4 = helper.cloneTask(taskToMerge1, taskToMerge1.getExpectedDeliveryTime(), orderToSplit2);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(taskToMerge1)
                .routePoint(taskToMerge3)
                .routePoint(helper.taskPrepaid("addrPaid", 14, orderGenerateService.createOrder().getId()))
                .routePoint(taskToMerge4)
                .routePoint(helper.taskUnpaid("addr3", 13, orderGenerateService.createOrder().getId()))
                .routePoint(taskToMerge2) // will be merged
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);
        userShift = repository.findById(id).orElseThrow();
    }

    @Test
    void shouldSplitRoutePointsIfNeeded() {
        commandService.updateRoute(new UserShiftCommand.UpdateRoute(userShift.getId(), userShift.getProcessingId(),
                SimpleStrategies.BY_DATE_INTERVAL_MERGE, Map.of()));

        transactionTemplate.execute(tt -> {
            UserShift us = repository.findByIdOrThrow(userShift.getId());
            assertThat(us.getRoutePoints()).isNotNull().hasSize(5);
            return null;
        });

        Map<Long, RouteTaskTimes> taskTimes = buildTaskTimesForOrders(Map.of(
                orderToSplit1, Duration.ofHours(4),
                orderToSplit2, Duration.ofHours(4).plus(5, ChronoUnit.MINUTES))
        );

        var updateCommand = new UserShiftCommand.UpdateRoute(userShift.getId(), userShift.getProcessingId(),
                SimpleStrategies.NO_MERGE, taskTimes);
        commandService.updateRoute(updateCommand);

        transactionTemplate.execute(tt -> {
            UserShift us = repository.findByIdOrThrow(userShift.getId());

            assertThat(us.getRoutePoints()).isNotNull().hasSize(7);
            assertThat(us.getRoutePoints().get(1).getTasks()).hasSize(2);
            assertThat(us.getRoutePoints().get(4).getTasks()).hasSize(1);
            assertThat(us.getRoutePoints().get(5).getTasks()).hasSize(1);
            return null;
        });
    }

    @Test
    void shouldNotSplitRoutePointWithFlowTask() {
        var pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 12345L);
        var date = LocalDate.now();
        var specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .arriveIntervalFrom(date.atTime(9, 0))
                        .arriveIntervalTo(date.atTime(15, 0))
                        .build()
        );
        var lockerOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );

        var arrivalTime = date.atTime(11, 0).toInstant(ZoneOffset.ofHours(3));
        var address = RoutePointAddress.builder()
                .addressString("address")
                .build();


        var orderTask = commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(userShift.getId(),
                helper.taskLockerDelivery(lockerOrder.getId(), pickupPoint.getId(), 10),
                SimpleStrategies.NO_MERGE
        ));

        var flowTask = commandService.addFlowTask(user, new UserShiftCommand.AddFlowTask(
                userShift.getId(), TaskFlowType.LOCKER_INVENTORY,
                NewCommonRoutePointData.builder()
                        .withLogisticRequests(List.of(specialRequest))
                        .name(TaskFlowType.LOCKER_INVENTORY.name())
                        .address(address)
                        .expectedArrivalTime(arrivalTime)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .build()
        ));

        assertThat(orderTask.getRoutePoint().getId()).isEqualTo(flowTask.getRoutePoint().getId());

        var taskTimes = buildTaskTimesForOrders(Map.of(
                lockerOrder.getId(), Duration.ofHours(2)
        ));
        taskTimes.put(flowTask.getId(),
                new RouteTaskTimes(
                        flowTask.getRoutePoint().getExpectedDateTime().plus(Duration.ofHours(2)),
                        flowTask.getRoutePoint().getExpectedDateTime().plus(Duration.ofHours(2))
                )
        );

        commandService.updateRoute(new UserShiftCommand.UpdateRoute(userShift.getId(), "test_processing_id",
                SimpleStrategies.BY_DATE_INTERVAL_MERGE, taskTimes));

        transactionTemplate.execute(s -> {
            var us = repository.findByIdOrThrow(userShift.getId());
            var lockerTask = us.streamLockerDeliveryTasks()
                    .flatMap(LockerDeliveryTask::streamLockerDeliverySubtasks)
                    .findFirst(st -> Objects.equals(st.getOrderId(), lockerOrder.getId()))
                    .map(LockerSubtask::getTask)
                    .orElseThrow();
            var inventTask = us.streamFlowTasks().findFirst().orElseThrow();

            // инвентаризация и доставка в постамат остались на одном роут поинте после рероута
            assertThat(lockerTask.getRoutePoint().getId()).isEqualTo(inventTask.getRoutePoint().getId());

            return null;
        });
    }

    @Test
    void shouldUpdateRouteProcessingId() {
        Map<Long, RouteTaskTimes> taskTimes = buildTaskTimesForOrders(Map.of(
                orderToSplit1, Duration.ofHours(4),
                orderToSplit2, Duration.ofHours(4).plus(5, ChronoUnit.MINUTES))
        );

        commandService.updateRoute(new UserShiftCommand.UpdateRoute(userShift.getId(), "test_processing_id",
                SimpleStrategies.BY_DATE_INTERVAL_MERGE, taskTimes));

        userShift = repository.findById(userShift.getId()).orElseThrow();
        assertThat(userShift.getProcessingId()).isEqualTo("test_processing_id");
    }

    @Test
    void shouldSplitAndMergeRoutePoints() {
        commandService.updateRoute(new UserShiftCommand.UpdateRoute(userShift.getId(), userShift.getProcessingId(),
                SimpleStrategies.BY_DATE_INTERVAL_MERGE, Map.of()));

        transactionTemplate.execute(tt -> {
            UserShift us = repository.findByIdOrThrow(userShift.getId());
            assertThat(us.getRoutePoints()).isNotNull().hasSize(5);
            return null;
        });

        Map<Long, RouteTaskTimes> taskTimes = buildTaskTimesForOrders(Map.of(
                orderToSplit1, Duration.ofHours(4),
                orderToSplit2, Duration.ofHours(4).plus(5, ChronoUnit.MINUTES))
        );

        var updateCommand = new UserShiftCommand.UpdateRoute(userShift.getId(), userShift.getProcessingId(),
                SimpleStrategies.BY_DATE_INTERVAL_MERGE, taskTimes);
        commandService.updateRoute(updateCommand);

        transactionTemplate.execute(tt -> {
            UserShift us = repository.findByIdOrThrow(userShift.getId());

            assertThat(us.getRoutePoints()).isNotNull().hasSize(6);
            assertThat(us.getRoutePoints().get(1).getTasks()).hasSize(2);
            assertThat(us.getRoutePoints().get(4).getTasks()).hasSize(2);
            return null;
        });
    }

    @Test
    void shouldUpdateExpectedTimeForCollectDropshipTasks() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder().build());

        User user = userHelper.findOrCreateUser(1L);
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        CollectDropshipTask collectDropshipTask = commandService.addCollectDropshipTask(user,
                new UserShiftCommand.AddCollectDropshipTask(
                        userShift.getId(),
                        NewCollectDropshipRoutePointData.builder()
                                .movement(movement)
                                .expectedArrivalTime(clock.instant())
                                .name("test")
                                .address(new RoutePointAddress(
                                        movement.getWarehouse().getAddress().getAddress(),
                                        movement.getWarehouse().getAddress().getGeoPoint()))
                                .build()
                ));

        Instant expectedTime = collectDropshipTask.getExpectedTime().plus(1, ChronoUnit.HOURS);
        Map<Long, RouteTaskTimes> taskTimes = Map.of(
                collectDropshipTask.getId(),
                new RouteTaskTimes(expectedTime, expectedTime)
        );

        commandService.updateRoute(
                new UserShiftCommand.UpdateRoute(
                        userShift.getId(),
                        "test_processing_id",
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE,
                        taskTimes
                )
        );

        transactionTemplate.execute(tt -> {
            UserShift us = repository.findById(userShift.getId()).orElseThrow();
            assertThat(us.getProcessingId()).isEqualTo("test_processing_id");

            CollectDropshipTask task = us.streamCollectDropshipTasks()
                    .findFirst()
                    .orElseThrow();

            assertThat(task.getExpectedTime()).isEqualTo(expectedTime);

            return null;
        });
    }

    private Map<Long, RouteTaskTimes> buildTaskTimesForOrders(Map<Long, Duration> timeshiftByOrder) {
        return transactionTemplate.execute(
                tt -> {
                    UserShift us = repository.findByIdOrThrow(userShift.getId());
                    return EntryStream.of(timeshiftByOrder)
                            .mapKeys(orderId -> us.streamDeliveryTasks()
                                    .findFirst(t -> t.getOrderIds().contains(orderId))
                                    .map(t -> (Task) t)
                                    .orElseThrow())
                            .mapToValue((task, dur) -> new RouteTaskTimes(
                                    task.getRoutePoint().getExpectedDateTime().plus(dur),
                                    task.getRoutePoint().getExpectedDateTime().plus(dur)
                            ))
                            .mapKeys(Task::getId)
                            .toMap();
                }
        );

    }

}
