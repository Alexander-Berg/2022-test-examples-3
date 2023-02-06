package ru.yandex.market.tpl.core.domain.movement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus.FAILED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus.FINISHED;

@RequiredArgsConstructor
class MovementManagerTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final MovementGenerator movementGenerator;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;

    private final UserShiftCommandService userShiftCommandService;
    private final TplTestCargoFactory tplTestCargoFactory;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final MovementManager movementManager;
    private final CollectDropshipTaskFactory collectDropshipTaskFactory;

    private PickupPoint pickupPoint;
    private Movement movementReturn;
    private Movement movementDirect;
    private Long userShiftId;

    private User user;


    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        clearAfterTest(pickupPoint);

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        movementReturn = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouseTo(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build());

        movementDirect = movementGenerator.generate(
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
    }

    @Test
    void shouldCorrectUnassign_whenCollectDropshipTask() {
        //given
        var addCollectDropshipTaskCommand = collectDropshipTaskFactory.createAddCollectDrophipTask(
                userShiftRepository.findByIdOrThrow(userShiftId),
                movementDirect,
                Instant.now(clock)
        );
        userShiftCommandService.addCollectDropshipTask(user, addCollectDropshipTaskCommand);
        //when
        movementManager.unassign(List.of(movementDirect));

        //then
        List<CollectDropshipTask> dropshipTasks = transactionTemplate.execute(st -> userShiftRepository
                .findByIdOrThrow(userShiftId)
                .streamCollectDropshipTasks()
                .filter(cdt -> cdt.getMovementId().equals(movementDirect.getId()))
                .collect(Collectors.toList()));

        assertThat(dropshipTasks).hasSize(1);
        assertThat(dropshipTasks.get(0).getStatus()).isEqualTo(CollectDropshipTaskStatus.CANCELLED);
    }

    @Test
    void shouldCorrectUnassignReturn_whenLockerTaskFinished_skippCancel() {
        //given
        String logisticPointIdTo = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo("first", logisticPointIdTo, "test");
        DropoffCargo cargo2 = tplTestCargoFactory.createCargo("second", logisticPointIdTo, "test");
        addDeliveryTask(null, true);

        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), Set.of(cargo1.getId(), cargo2.getId()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, userShiftId)
        );

        transactionTemplate.execute(st -> {
            var task = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamLockerDeliveryTasks().findFirst().orElseThrow();
            task.finishLoadingLocker(
                    Instant.now(),
                    null,
                    ScanRequest.builder()
                            .successfullyScannedDropoffCargos(Set.of(cargo1.getId(), cargo2.getId()))
                            .build()
            );

            tplTestCargoFactory.finishUpload(
                    List.of(),
                    TplTestCargoFactory.ShiftContext.of(user, userShiftId)
            );

            return null;
        });

        //when
        movementManager.unassign(List.of(movementReturn));

        //then
        List<LockerSubtask> subTasks = transactionTemplate.execute(st -> userShiftRepository
                .findByIdOrThrow(userShiftId)
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList()));

        assertThat(subTasks.stream().anyMatch(st -> st.getStatus() != FINISHED)).isFalse();
        assertThat(subTasks.get(0).getTask().getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
    }

    @Test
    void shouldCorrectUnassignReturn_whenLockerTaskNotFinished_Cancel() {
        //given
        addDeliveryTask(null, true);

        //when
        movementManager.unassign(List.of(movementReturn));

        //then
        List<LockerSubtask> subTasks = transactionTemplate.execute(st -> userShiftRepository
                .findByIdOrThrow(userShiftId)
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList()));

        assertThat(subTasks.stream().anyMatch(st -> st.getStatus() != FAILED)).isFalse();
        assertThat(subTasks.get(0).getTask().getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
    }

    @Test
    void shouldCorrectUnassignReturn_whenLockerTaskNotFinished_CancelReturnSubtasksOnly() {
        //given
        addDeliveryTask(null, true);
        addDeliveryTask(null, false);

        //when
        movementManager.unassign(List.of(movementReturn));

        //then
        List<LockerSubtask> subTasks = transactionTemplate.execute(st -> userShiftRepository
                .findByIdOrThrow(userShiftId)
                .streamLockerDeliveryTasks()
                .flatMap(ldt -> StreamEx.of(ldt.streamDropOffReturnSubtasks())
                        .append(ldt.streamDropoffCargoSubtasks()))
                .collect(Collectors.toList()));

        assertThat(subTasks.stream()
                .filter(st -> st.getType() == LockerSubtaskType.DROPOFF_RETURN)
                .anyMatch(st -> st.getStatus() != FAILED)).isFalse();
        assertThat(subTasks.stream()
                .filter(st -> st.getType() == LockerSubtaskType.DROPOFF)
                .anyMatch(st -> st.getStatus() != FAILED)).isTrue();
        assertThat(subTasks.get(0).getTask().getStatus()).isNotEqualTo(LockerDeliveryTaskStatus.CANCELLED);
    }

    @Test
    void shouldCorrectUnassignReturn_whenLockerTaskNotFinished_CancelDirectSubtasksOnly() {
        //given
        addDeliveryTask(null, true);
        addDeliveryTask(null, false);

        //when
        movementManager.unassign(List.of(movementDirect));

        //then
        List<LockerSubtask> subTasks = transactionTemplate.execute(st -> userShiftRepository
                .findByIdOrThrow(userShiftId)
                .streamLockerDeliveryTasks()
                .flatMap(ldt -> StreamEx.of(ldt.streamDropOffReturnSubtasks())
                        .append(ldt.streamDropoffCargoSubtasks()))
                .collect(Collectors.toList()));

        assertThat(subTasks.stream()
                .filter(st -> st.getType() == LockerSubtaskType.DROPOFF_RETURN)
                .anyMatch(st -> st.getStatus() != FAILED)).isTrue();
        assertThat(subTasks.stream()
                .filter(st -> st.getType() == LockerSubtaskType.DROPOFF)
                .anyMatch(st -> st.getStatus() != FAILED)).isFalse();
        assertThat(subTasks.get(0).getTask().getStatus()).isNotEqualTo(LockerDeliveryTaskStatus.CANCELLED);
    }

    @Test
    void shouldCorrectUnassignAll_whenLockerTaskNotFinished_CancelAllSubtasks() {
        //given
        addDeliveryTask(null, true);
        addDeliveryTask(null, false);

        //when
        movementManager.unassign(List.of(movementReturn, movementDirect));

        //then
        List<LockerSubtask> subTasks = transactionTemplate.execute(st -> userShiftRepository
                .findByIdOrThrow(userShiftId)
                .streamLockerDeliveryTasks()
                .flatMap(ldt -> StreamEx.of(ldt.streamDropOffReturnSubtasks())
                        .append(ldt.streamDropoffCargoSubtasks()))
                .collect(Collectors.toList()));

        assertThat(subTasks.stream().anyMatch(st -> st.getStatus() != FAILED)).isFalse();
        assertThat(subTasks.get(0).getTask().getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
    }

    private DeliveryTask addDeliveryTask(Long cargoId, boolean isReturn) {
        return userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(isReturn ? movementReturn.getId() : movementDirect.getId())
                                                .dropOffCargoId(cargoId)
                                                .isReturn(isReturn)
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movementReturn.getWarehouseTo().getAddress()))
                                .name(movementReturn.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }
}
