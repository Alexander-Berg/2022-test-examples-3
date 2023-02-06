package ru.yandex.market.tpl.core.domain.dropoffcargo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.scanner.processor.TplScanDropoffCargoProcessor;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.UserShiftUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.TaskType.LOCKER_DELIVERY;

@RequiredArgsConstructor
public class DropOfUserShiftTest extends TplAbstractTest {
    private final TransactionTemplate transactionTemplate;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final MovementGenerator movementGenerator;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final DropoffCargoRepository dropoffCargoRepository;
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final Clock clock;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftQueryService userShiftQueryService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TplScanDropoffCargoProcessor cargoProcessor;

    private PickupPoint pickupPoint;
    private Movement movement;
    private Long userShiftId;

    private User user;

    @BeforeEach
    void init() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );
        clearAfterTest(pickupPoint);

        user = userHelper.findOrCreateUser(824567125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouse(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        userShiftId = userShiftCommandService.createUserShift(createCommand);

        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movement.getId())
                                                .isReturn(false)
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                                .name(movement.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }

    @Test
    void addDropOffReturnMovementToUserShift() {
        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(1);

            LockerSubtask lockerSubtask =
                    lockerDeliveryTask.streamSubtask().findFirst().orElseThrow();

            assertThat(lockerSubtask.getType()).isEqualTo(LockerSubtaskType.DROPOFF);
            assertThat(lockerSubtask.getLockerSubtaskDropOff()).isNotNull();
            assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(movement.getId());

            var tasks = userShiftQueryService.getRemainingTasksInfo(user);
            var movementsIds = tasks.getOrders().stream()
                    .filter(summary -> summary.getType() == LOCKER_DELIVERY)
                    .map(summary -> summary.getTaskId())
                    .collect(Collectors.toSet());
            assertThat(movementsIds).contains(lockerDeliveryTask.getId());

            return null;

        });
    }

    @Test
    void shouldExistDropoffDirectTasks() {
        transactionTemplate.execute(tt -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            for (int i = 0; i < 5; i++) {
                Long cargoBarcodeSuffix = dropoffCargoRepository.getNextSequenceValue();
                dropoffCargoCommandService.createOrGet(
                        DropoffCargoCommand.Create.builder()
                                .barcode("BAG-" + cargoBarcodeSuffix)
                                .logisticPointIdFrom("" + pickupPoint.getLogisticPointId())
                                .logisticPointIdTo("" + userShift.getSortingCenterId())
                                .referenceId("REF-" + userShiftId)
                                .build()
                );
            }

            var totalDropoffTasks = UserShiftUtils.getTotalDropoffTasks(Optional.of(userShift));

            var dropoffTasksInProgress = UserShiftUtils.getDropoffTasksInProgress(totalDropoffTasks);
            var result = userShiftQueryService.getDropoffTaskDtos(dropoffTasksInProgress);
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.iterator().next().getPlaces().size()).isEqualTo(5);
            return null;
        });
    }

    @Test
    void shouldExistDropoffDirectTasks_whenEmpty() {
        transactionTemplate.execute(tt -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            var totalDropoffTasks = UserShiftUtils.getTotalDropoffTasks(Optional.of(userShift));

            var dropoffTasksInProgress = UserShiftUtils.getDropoffTasksInProgress(totalDropoffTasks);
            var result = userShiftQueryService.getDropoffTaskDtos(dropoffTasksInProgress);
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.iterator().next().getPlaces()).isNull();
            return null;
        });
    }


}
