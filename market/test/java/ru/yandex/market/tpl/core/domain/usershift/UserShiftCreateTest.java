package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.transaction.TestTransaction;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftCreateTest {

    @Value("${tpl.callInMinutesBeforeDelivery:60}")
    private int callInMinutesBeforeDelivery;

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final UserPropertyService userPropertyService;

    @MockBean
    private Clock clock;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.of(2017, 1, 1, 0, 0, 0));
        LocalDate now = LocalDate.now(clock);
        user = userHelper.findOrCreateUser(824125L, now);
        shift = userHelper.findOrCreateOpenShift(now);
    }

    @Test
    void shouldCreateShift() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(userShift.getTransportTypeId()).isEqualTo(0);
        assertThat(userShift.getRoutingVehicleType()).isEqualTo(RoutingVehicleType.COMMON);
        assertThat(userShift.getCurrentRoutePoint()).isNull();
        assertThat(userShift.getRoutePoints()).isNotNull().isEmpty();
    }

    @Test
    void shouldCreateShiftWithRoutePoints() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, orderGenerateService.createOrder().getId()))
                .routePoint(helper.taskPrepaid("addrPaid", 14, orderGenerateService.createOrder().getId()))
                .routePoint(helper.taskUnpaid("addr3", 13, orderGenerateService.createOrder().getId()))
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(userShift.getCurrentRoutePoint()).isNull();
        assertThat(userShift.getRoutePoints()).isNotNull().hasSize(5);

        assertThat(userShift.getRoutePoints().get(0).getType()).isEqualTo(RoutePointType.ORDER_PICKUP);

        assertThat(userShift.getRoutePoints())
                .extracting(RoutePoint::getStatus)
                .containsOnly(RoutePointStatus.NOT_STARTED);

        var pickupTasks = userShift.streamPickupRoutePoints()
                .flatMap(RoutePoint::streamPickupTasks)
                .toList();

        assertThat(pickupTasks).hasSize(1);
        assertThat(pickupTasks.get(0).getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);

        var tasks = userShift.streamOrderDeliveryTasks().toList();

        assertThat(tasks).hasSize(3);
        assertThat(tasks).extracting(OrderDeliveryTask::getStatus)
                .containsExactly(
                        OrderDeliveryTaskStatus.NOT_DELIVERED,
                        OrderDeliveryTaskStatus.NOT_DELIVERED,
                        OrderDeliveryTaskStatus.NOT_DELIVERED);

        assertThat(userShift.streamDeliveryRoutePoints().toList())
                .extracting(r -> r.getAddressString())
                .containsExactly("addr1", "addr3", "addrPaid");

        List<CallToRecipientTask> callToRecipientTasks = userShift.getCallToRecipientTasks();
        assertThat(callToRecipientTasks).isNotNull();
        assertThat(callToRecipientTasks).hasSize(3);

        tasks.forEach(orderDeliveryTask -> {
            CallToRecipientTask callToRecipientTask = orderDeliveryTask.getCallToRecipientTask();
            assertThat(callToRecipientTask).isNotNull();
            Instant rpExpectedDateTime = orderDeliveryTask.getRoutePoint().getExpectedDateTime();

            Instant expectedCallTime = rpExpectedDateTime.minus(callInMinutesBeforeDelivery, ChronoUnit.MINUTES);
            assertThat(callToRecipientTask.getExpectedCallTime()).isEqualTo(expectedCallTime);
        });
    }

    @ParameterizedTest
    @EnumSource(value = SimpleStrategies.class, mode = EnumSource.Mode.EXCLUDE, names = "NO_MERGE")
    void shouldCreateShiftAndMergeExactRoutePoints(SimpleStrategies strategy) {
        var taskToMerge1 = helper.taskUnpaid("addr1", 12, orderGenerateService.createOrder().getId());
        var taskToMerge2 = helper.cloneTask(taskToMerge1, taskToMerge1.getExpectedArrivalTime(),
                orderGenerateService.createOrder().getId());

        var taskNotToMerge = helper.cloneTask(taskToMerge1,
                taskToMerge1.getExpectedArrivalTime().plus(3, ChronoUnit.HOURS),
                orderGenerateService.createOrder().getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(taskToMerge1)
                .routePoint(helper.taskPrepaid("addrPaid", 14, orderGenerateService.createOrder().getId()))
                .routePoint(taskNotToMerge)
                .routePoint(helper.taskUnpaid("addr3", 13, orderGenerateService.createOrder().getId()))
                .routePoint(taskToMerge2) // will be merged
                .mergeStrategy(strategy)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(deliveryRoutePoints(userShift)).isNotNull().hasSize(4);
        assertThat(deliveryRoutePoints(userShift).get(0).getTasks()).hasSize(2);
    }

    private List<RoutePoint> deliveryRoutePoints(UserShift userShift) {
        return userShift.streamDeliveryRoutePoints()
                .collect(Collectors.toList());
    }

    @Test
    void shouldCreateShiftAndMergeSimilarRoutePoints() {
        var taskToMerge1 = helper.taskUnpaid("addr1", 12, orderGenerateService.createOrder().getId());
        var taskToMerge2 = helper.cloneTask(taskToMerge1,
                taskToMerge1.getExpectedDeliveryTime().plus(4, ChronoUnit.MINUTES),
                orderGenerateService.createOrder().getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(taskToMerge1)
                .routePoint(helper.taskPrepaid("addrPaid", 14, orderGenerateService.createOrder().getId()))
                .routePoint(helper.taskUnpaid("addr3", 13, orderGenerateService.createOrder().getId()))
                .routePoint(taskToMerge2) // will be merged
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(deliveryRoutePoints(userShift)).isNotNull().hasSize(3);
        assertThat(deliveryRoutePoints(userShift).get(0).getTasks()).hasSize(2);
    }

    @Test
    @Commit
    @Disabled
    void shouldAddTaskAfterCreate() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(userShift.getRoutePoints()).isNotNull().isEmpty();

        TestTransaction.end();
        TestTransaction.start();

        addTwoRoutePoints(id, userShift);

        TestTransaction.end();
        TestTransaction.start();

        userShift = repository.findById(id).orElseThrow();
        assertThat(deliveryRoutePoints(userShift)).hasSize(2);

        assertThat(userShift.getCallToRecipientTasks()).hasSize(2);
    }

    @Test
    @Commit
    @Disabled
    void shouldAddTaskToClosedShift() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(userShift.getRoutePoints()).isNotNull().isEmpty();

        commandService.checkin(user, new UserShiftCommand.CheckIn(id));
        if (!userPropertyService.findPropertyForUser(UserProperties.SEQUENTIAL_DELIVERY_ENABLED, user)) {
            commandService.startShift(user, new UserShiftCommand.Start(id));
        }

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);

        TestTransaction.end();
        TestTransaction.start();

        addTwoRoutePoints(id, userShift);

        TestTransaction.end();
        TestTransaction.start();

        userShift = repository.findById(id).orElseThrow();
        assertThat(deliveryRoutePoints(userShift)).hasSize(2);
    }

    @Test
    void createCollectDropshipsTask() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, orderGenerateService.createOrder().getId()))
                .routePoint(helper.taskCollectDropship(
                        LocalDate.now(clock),
                        movementGenerator.generate(MovementCommand.Create.builder().build()))
                )
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = repository.findById(id).orElseThrow();
        List<CollectDropshipTask> collectDropshipTasks = userShift.streamCollectDropshipTasks()
                .collect(Collectors.toList());
        assertThat(collectDropshipTasks.size()).isEqualTo(1);
        assertThat(collectDropshipTasks.get(0).getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
    }

    @Test
    void shouldMergeRoutePointsOnNewTask() {
        var taskToMerge1 = helper.taskUnpaid("addr1", 12, orderGenerateService.createOrder().getId());
        var taskToMerge2 = helper.cloneTask(taskToMerge1,
                taskToMerge1.getExpectedDeliveryTime().plus(4, ChronoUnit.MINUTES),
                orderGenerateService.createOrder().getId());
        var taskToMerge3 = helper.cloneTask(taskToMerge1, taskToMerge1.getExpectedDeliveryTime(),
                orderGenerateService.createOrder().getId());
        var taskToMerge4 = helper.cloneTask(taskToMerge1, taskToMerge1.getExpectedDeliveryTime(),
                orderGenerateService.createOrder().getId());

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
        UserShift userShift = repository.findById(id).orElseThrow();

        assertThat(deliveryRoutePoints(userShift)).isNotNull().hasSize(6);

        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                helper.taskPrepaid("addNew", 15, orderGenerateService.createOrder().getId()),
                SimpleStrategies.BY_DATE_INTERVAL_MERGE
        ));

        userShift = repository.findById(id).orElseThrow();

        assertThat(deliveryRoutePoints(userShift)).isNotNull().hasSize(4);
        assertThat(deliveryRoutePoints(userShift).get(0).getTasks()).hasSize(4);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private void addTwoRoutePoints(long id, UserShift userShift) {
        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                helper.taskPrepaid("125", 7, orderGenerateService.createOrder().getId()),
                SimpleStrategies.NO_MERGE
        ));

        userShift = repository.findById(id).orElseThrow();
        assertThat(deliveryRoutePoints(userShift)).hasSize(1);

        commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(
                        userShift.getId(),
                        helper.taskUnpaid("444", 6, orderGenerateService.createOrder().getId()),
                        SimpleStrategies.NO_MERGE
                )
        );
    }

}
