package ru.yandex.market.tpl.core.domain.dropoffcargo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.MovementStatusHistory;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.Status;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.model.delivery.response.GetMovementResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusHistoryResponse;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.order.locker.LockerDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.DeliverySummaryDto;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.ds.DsMovementManager;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementCommandService;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
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
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.order.movement.PartnerDropshipService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.movement.MovementStatus.TRANSPORTATION;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.CARGO_RECEIVED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.DROPOFF_CARGO_DELIVERY_TASK_FAILED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.DROPOFF_CARGO_DELIVERY_TASK_REOPEN;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_CANCELLED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_CONFIRMED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_CREATED;
import static ru.yandex.market.tpl.api.model.order.partner.MovementHistoryEventType.MOVEMENT_DROPOFF_RETURN_DELIVERY_TASK_CREATED;
import static ru.yandex.market.tpl.core.query.usershift.mapper.OrderSummaryDtoMapper.CARGO_SUMMARY_HINT;

@RequiredArgsConstructor
class DropOffReturnUserShiftTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final MovementGenerator movementGenerator;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final DropoffCargoRepository dropoffCargoRepository;
    private final MovementRepository movementRepository;
    private final Clock clock;

    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftQueryService userShiftQueryService;
    private final LockerDeliveryService lockerDeliveryService;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final DsMovementManager dsMovementManager;
    private final MovementCommandService movementCommandService;
    private final PartnerDropshipService partnerDropshipService;
    private final TplTestCargoFactory tplTestCargoFactory;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

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

        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movementReturn.getId())
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

            assertThat(lockerSubtask.getType()).isEqualTo(LockerSubtaskType.DROPOFF_RETURN);
            assertThat(lockerSubtask.getLockerSubtaskDropOff()).isNotNull();
            assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(movementReturn.getId());

            return null;
        });
    }

    @Test
    void addDropOffMovementToUserShift() {
        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(1);

            LockerSubtask lockerSubtask =
                    lockerDeliveryTask.streamSubtask().findFirst().orElseThrow();

            assertThat(lockerSubtask.getType()).isEqualTo(LockerSubtaskType.DROPOFF_RETURN);
            assertThat(lockerSubtask.getLockerSubtaskDropOff()).isNotNull();
            assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(movementReturn.getId());

            return null;
        });
    }

    @Test
    void cancelDropOffReturnTask() {
        String logisticPointIdTo = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo("BAG-001", logisticPointIdTo);
        DropoffCargo cargo2 = tplTestCargoFactory.createCargo("BAG-002", logisticPointIdTo);

        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), Set.of(cargo1.getId(), cargo2.getId()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, userShiftId)
        );

        checkMovementAndCargoStatuses(cargo1, cargo2);

        UserShiftCommand.FailOrderDeliveryTask command = getFailOrderDeliveryTaskCommand();
        userShiftCommandService.failDeliveryTask(user, command);

        checkMovementAndCargoStatuses(cargo1, cargo2);
        assertThatLockerSubtasksFailed();
        assertThat(movementHistoryEventRepository.findByMovementId(movementReturn.getId(), Pageable.unpaged()))
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        MOVEMENT_DROPOFF_RETURN_DELIVERY_TASK_CREATED, // TODO при приемке мешков кидать другой тип
                        // ивента
                        MOVEMENT_CONFIRMED,
                        MOVEMENT_DROPOFF_RETURN_DELIVERY_TASK_CREATED, // TODO при приемке мешков кидать другой тип
                        // ивента
                        CARGO_RECEIVED,
                        DROPOFF_CARGO_DELIVERY_TASK_FAILED,
                        DROPOFF_CARGO_DELIVERY_TASK_FAILED
                );

        lockerDeliveryService.reopen(command.getTaskId(), user);

        assertThatLockerSubtasksNotFailed();
        checkMovementAndCargoStatuses(cargo1, cargo2);
        assertThat(movementHistoryEventRepository.findByMovementId(movementReturn.getId(), Pageable.unpaged()))
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        MOVEMENT_DROPOFF_RETURN_DELIVERY_TASK_CREATED, // TODO при приемке мешков кидать другой тип
                        // ивента
                        MOVEMENT_CONFIRMED,
                        MOVEMENT_DROPOFF_RETURN_DELIVERY_TASK_CREATED, // TODO при приемке мешков кидать другой тип
                        // ивента
                        CARGO_RECEIVED,
                        DROPOFF_CARGO_DELIVERY_TASK_FAILED,
                        DROPOFF_CARGO_DELIVERY_TASK_FAILED,
                        DROPOFF_CARGO_DELIVERY_TASK_REOPEN,
                        DROPOFF_CARGO_DELIVERY_TASK_REOPEN
                );
    }

    @Test
    void cancelDropOffMovementFromPi() {
        movementCommandService.handleCommand(
                new MovementCommand.Cancel(
                        movementReturn.getId(),
                        Source.OPERATOR,
                        ""
                )
        );

        Movement updatedMovement =
                movementRepository.findByExternalId(this.movementReturn.getExternalId()).orElseThrow();
        assertThat(updatedMovement.getStatus()).isEqualTo(MovementStatus.CANCELLED);

        assertThat(movementHistoryEventRepository.findByMovementId(movementReturn.getId(), Pageable.unpaged()))
                .extracting(MovementHistoryEvent::getType)
                .containsExactly(
                        MOVEMENT_CREATED,
                        MOVEMENT_DROPOFF_RETURN_DELIVERY_TASK_CREATED,
                        MOVEMENT_CONFIRMED,
                        MOVEMENT_CANCELLED
                );

        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getStatus)
                    .containsExactly(LockerDeliverySubtaskStatus.FAILED);

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getFailReason)
                    .extracting(OrderDeliveryFailReason::getType)
                    .containsExactly(OrderDeliveryTaskFailReasonType.CANCEL_ORDER);

            return null;
        });


    }


    @Test
    void cancelAndReopenDropOffTaskFromPi() {
        partnerDropshipService.cancelTask(new PartnerkaCommand.CancelMovementExecutionTask(movementReturn.getId(),
                "other"));

        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getStatus)
                    .containsExactly(LockerDeliverySubtaskStatus.FAILED);

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getFailReason)
                    .extracting(OrderDeliveryFailReason::getType)
                    .containsExactly(OrderDeliveryTaskFailReasonType.OTHER);

            return null;
        });

        partnerDropshipService.reopenTask(new PartnerkaCommand.ReopenMovementExecutionTask(movementReturn.getId()));

        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getStatus)
                    .containsExactly(LockerDeliverySubtaskStatus.NOT_STARTED);

            return null;
        });
    }

    private void assertThatLockerSubtasksFailed() {
        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getStatus)
                    .containsExactly(LockerDeliverySubtaskStatus.FAILED, LockerDeliverySubtaskStatus.FAILED);

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getFailReason)
                    .extracting(OrderDeliveryFailReason::getType)
                    .containsExactly(OrderDeliveryTaskFailReasonType.PVZ_CLOSED,
                            OrderDeliveryTaskFailReasonType.PVZ_CLOSED);

            return null;
        });
    }

    private void assertThatLockerSubtasksNotFailed() {
        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

            assertThat(lockerDeliveryTask.getSubtasks())
                    .extracting(LockerSubtask::getStatus)
                    .containsExactly(LockerDeliverySubtaskStatus.NOT_STARTED, LockerDeliverySubtaskStatus.NOT_STARTED);

            return null;
        });

    }

    private void checkMovementAndCargoStatuses(DropoffCargo cargo1, DropoffCargo cargo2) {
        Movement updatedMovement =
                movementRepository.findByExternalId(this.movementReturn.getExternalId()).orElseThrow();
        assertThat(updatedMovement.getStatus()).isEqualTo(TRANSPORTATION);

        List<DropoffCargo> cargo = dropoffCargoRepository.findByIdIn(List.of(cargo1.getId(), cargo2.getId()));

        assertThat(cargo)
                .extracting(DropoffCargo::getStatus)
                .containsExactly(DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE,
                        DropoffCargoFlowStatus.ISSUED_FOR_CARRIAGE);
    }

    private UserShiftCommand.FailOrderDeliveryTask getFailOrderDeliveryTaskCommand() {
        return transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(2);

            LockerSubtask lockerSubtask =
                    lockerDeliveryTask.streamSubtask().findFirst().orElseThrow();

            assertThat(lockerSubtask.getLockerSubtaskDropOff()).isNotNull();
            assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(movementReturn.getId());

            return new UserShiftCommand.FailOrderDeliveryTask(
                    userShiftId,
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    new OrderDeliveryFailReason(
                            OrderDeliveryTaskFailReasonType.PVZ_CLOSED,
                            "Никого нет дома",
                            List.of(),
                            Source.COURIER
                    )
            );
        });
    }


    @Test
    void addDropOffReturnCargoDeliveryTask() {
        DropoffCargo cargo1 = addDropOffCargoDeliveryTask("first");

        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(1);

            LockerSubtask lockerSubtask =
                    lockerDeliveryTask.streamSubtask().findFirst().orElseThrow();

            assertThat(lockerSubtask.getLockerSubtaskDropOff()).isNotNull();
            assertThat(lockerSubtask.getLockerSubtaskDropOff().getMovementId()).isEqualTo(movementReturn.getId());
            assertThat(lockerSubtask.getLockerSubtaskDropOff().getDropoffCargoId()).isEqualTo(cargo1.getId());

            return null;
        });

        DropoffCargo cargo2 = addDropOffCargoDeliveryTask("second");

        transactionTemplate.execute(tt -> {
            RoutePoint routePoint = userShiftRepository.findByIdOrThrow(userShiftId)
                    .streamDeliveryRoutePoints().findFirst().orElseThrow();
            LockerDeliveryTask lockerDeliveryTask =
                    (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            assertThat(lockerDeliveryTask.getSubtasks())
                    .hasSize(2);

            assertThat(
                    StreamEx.of(lockerDeliveryTask.getSubtasks())
                            .filter(lockerSubtask -> lockerSubtask.getLockerSubtaskDropOff().getDropoffCargoId().equals(cargo1.getId()))
                            .findFirst()
            ).isPresent();

            assertThat(
                    StreamEx.of(lockerDeliveryTask.getSubtasks())
                            .filter(lockerSubtask -> lockerSubtask.getLockerSubtaskDropOff().getDropoffCargoId().equals(cargo2.getId()))
                            .findFirst()
            ).isPresent();

            return null;
        });
    }

    @Test
    void mapLockerDeliveryTask_OnlyDropOffCargo() {
        //given
        DropoffCargo cargo1 = addDropOffCargoDeliveryTask("first");
        DropoffCargo cargo2 = addDropOffCargoDeliveryTask("second");

        Long routePointId = transactionTemplate.execute(
                tt -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamDeliveryRoutePoints()
                        .findFirst()
                        .orElseThrow()
                        .getId());

        //when
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(
                user,
                routePointId
        );

        //then
        assertsRoutePointDto(routePointInfo, Set.of(cargo1, cargo2));
    }

    @Test
    void mapLockerDeliveryTask_OnlyDropOffCargoDirect_withTasks() {
        //given
        DropoffCargo cargo1 = addDropOffCargoDeliveryTaskDirect("first");
        DropoffCargo cargo2 = addDropOffCargoDeliveryTaskDirect("second");

        Long routePointId = transactionTemplate.execute(
                tt -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamDeliveryRoutePoints()
                        .findFirst()
                        .orElseThrow()
                        .getId());

        //when
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(
                user,
                routePointId
        );

        //then
        assertsRoutePointDto(routePointInfo, Set.of(cargo1, cargo2));
    }

    @Test
    void mapLockerDeliveryTask_OnlyDropOffCargoDirect_withOutTasks() {
        //given
        String logisticPointIdFrom = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo("first", "test", logisticPointIdFrom);
        DropoffCargo cargo2 = tplTestCargoFactory.createCargo("second", "test", logisticPointIdFrom);
        addDeliveryTask(null, false);

        Long routePointId = transactionTemplate.execute(
                tt -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamDeliveryRoutePoints()
                        .findFirst()
                        .orElseThrow()
                        .getId());

        //when
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(
                user,
                routePointId
        );

        //then
        assertsRoutePointDto(routePointInfo, Set.of());
    }

    @Test
    void mapLockerDeliveryTask_OnlyDropOffCargoDirect_withOutTasks_whenCancelled() {
        //given
        String logisticPointIdFrom = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo("first", "test", logisticPointIdFrom);
        DropoffCargo cargo2 = tplTestCargoFactory.createCargo("second", "test", logisticPointIdFrom);
        addDeliveryTask(null, false);

        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        UserShiftCommand.FailOrderDeliveryTask command = getFailOrderDeliveryTaskCommand();
        userShiftCommandService.failDeliveryTask(user, command);


        Long routePointId = transactionTemplate.execute(
                tt -> userShiftRepository.findByIdOrThrow(userShiftId)
                        .streamDeliveryRoutePoints()
                        .findFirst()
                        .orElseThrow()
                        .getId());

        //when
        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(
                user,
                routePointId
        );

        //then
        assertsRoutePointDto(routePointInfo, Set.of(cargo1, cargo2));
    }

    private void assertsRoutePointDto(RoutePointDto routePointInfo, Set<DropoffCargo> cargos) {
        assertThat(routePointInfo.getTasks()).hasSize(1);

        LockerDeliveryTaskDto lockerDeliveryTaskDto =
                (LockerDeliveryTaskDto) routePointInfo.getTasks().iterator().next();

        LockerDto locker = lockerDeliveryTaskDto.getLocker();
        assertThat(locker).isNotNull();
        assertThat(locker.getDescription()).isEqualTo(movementReturn.getWarehouseTo().getDescription());
        assertThat(locker.getPartnerSubType()).isEqualTo(PartnerSubType.PVZ);

        Set<ScanTaskDestinationDto> destinations = lockerDeliveryTaskDto.getDestinations();
        assertThat(destinations).hasSize(1);

        ScanTaskDestinationDto destination = destinations.iterator().next();

        assertThat(destination.getOutsideOrders()).hasSize(cargos.size());
        assertThat(lockerDeliveryTaskDto.getOrders()).hasSize(cargos.size());

        if (!cargos.isEmpty()) {
            List<String> barcodes = cargos.stream().map(DropoffCargo::getBarcode).collect(Collectors.toList());
            assertThat(destination.getOutsideOrders())
                    .extracting(DestinationOrderDto::getExternalOrderId)
                    .containsExactlyInAnyOrderElementsOf(barcodes);

            assertThat(lockerDeliveryTaskDto.getOutsideOrders()).isNotEmpty();
            assertThat(lockerDeliveryTaskDto.getOutsideOrders())
                    .extracting(DestinationOrderDto::getExternalOrderId)
                    .containsExactlyInAnyOrderElementsOf(barcodes);

            assertThat(lockerDeliveryTaskDto.getOrders()).isNotNull();
            assertThat(lockerDeliveryTaskDto.getOrders())
                    .extracting(OrderDto::getExternalOrderId)
                    .containsExactlyInAnyOrderElementsOf(barcodes);
        }
    }

    @Test
    void mapLockerDeliveryTasks_getRemainingTasks() {

        DropoffCargo cargo1 = addDropOffCargoDeliveryTask("first");
        DropoffCargo cargo2 = addDropOffCargoDeliveryTask("second");

        RemainingOrderDeliveryTasksDto remainingTasksInfo = userShiftQueryService.getRemainingTasksInfo(user);

        DeliverySummaryDto summary = remainingTasksInfo.getSummary();
        assertThat(summary.getUnfinishedOrderCount()).isEqualTo(1);
        assertThat(summary.getTotalOrderCount()).isEqualTo(1);

        List<OrderSummaryDto> orders = remainingTasksInfo.getOrders();
        assertThat(orders).hasSize(2);

        assertThat(orders)
                .extracting(OrderSummaryDto::getExternalOrderId)
                .containsExactlyInAnyOrder(cargo1.getBarcode(), cargo2.getBarcode());

        assertThat(orders)
                .extracting(OrderSummaryDto::isCanBeGrouped)
                .containsExactly(true, true);

        assertThat(orders)
                .extracting(OrderSummaryDto::getRecipientFio)
                .containsExactly(PartnerSubType.PVZ.getDescription(), PartnerSubType.PVZ.getDescription());
    }

    @Test
    void mapLockerDeliveryTasks_getRemainingTasks_whenPickup() {

        RemainingOrderDeliveryTasksDto remainingTasksInfo = userShiftQueryService.getRemainingTasksInfo(user);

        DeliverySummaryDto summary = remainingTasksInfo.getSummary();
        assertThat(summary.getUnfinishedOrderCount()).isEqualTo(1);
        assertThat(summary.getTotalOrderCount()).isEqualTo(1);

        List<OrderSummaryDto> orders = remainingTasksInfo.getOrders();
        assertThat(orders).hasSize(1);

        assertThat(orders)
                .extracting(OrderSummaryDto::getExternalOrderId)
                .containsExactlyInAnyOrder(CARGO_SUMMARY_HINT);

        assertThat(orders)
                .extracting(OrderSummaryDto::isCanBeGrouped)
                .containsExactly(true);

        assertThat(orders)
                .extracting(OrderSummaryDto::getRecipientFio)
                .containsExactly(PartnerSubType.PVZ.getDescription());
    }

    @Test
    void mapLockerDeliveryTasks_getAllTasks() {
        DropoffCargo cargo1 = addDropOffCargoDeliveryTask("first");
        DropoffCargo cargo2 = addDropOffCargoDeliveryTask("second");

        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, false);


        List<OrderSummaryDto> tasks = tasksInfo.getTasks();
        assertThat(tasks).hasSize(2);

        assertThat(tasks)
                .extracting(OrderSummaryDto::getExternalOrderId)
                .containsExactlyInAnyOrder(cargo1.getBarcode(), cargo2.getBarcode());

        assertThat(tasks)
                .extracting(OrderSummaryDto::isCanBeGrouped)
                .containsExactly(true, true);

        assertThat(tasks)
                .extracting(OrderSummaryDto::getRecipientFio)
                .containsExactly(PartnerSubType.PVZ.getDescription(), PartnerSubType.PVZ.getDescription());
    }


    @Test
    void getMovement_DropOffReturnMovement_ReturnAssignedCourier() {
        GetMovementResponse getResponse = dsMovementManager.getMovement(new ResourceId(movementReturn.getExternalId()
                , null));
        assertThat(getResponse).isNotNull();
        Courier courier = getResponse.getCourier();
        assertThat(courier).isNotNull();
        Person courierPerson = courier.getPersons().iterator().next();
        assertThat(courierPerson.getName()).isEqualTo(user.getFirstName());
        assertThat(courierPerson.getSurname()).isEqualTo(user.getLastName());
    }

    @Test
    void getMovementStatus_DropOffReturnMovement_ReturnNeededStatuses() {
        GetMovementStatusHistoryResponse response =
                dsMovementManager.getMovementStatusHistory(List.of(new ResourceId(movementReturn.getExternalId(),
                        null)));

        assertThat(response.getMovementStatusHistories()).hasSize(1);
        MovementStatusHistory statusHistory = response.getMovementStatusHistories().iterator().next();

        assertThat(statusHistory.getHistory())
                .extracting(Status::getStatusCode)
                .containsExactlyInAnyOrder(StatusCode.CREATED, StatusCode.COURIER_FOUND, StatusCode.MOVEMENT_CONFIRMED);
    }

    private DropoffCargo addDropOffCargoDeliveryTask(String barcode) {
        String logisticPointIdTo = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo(barcode, logisticPointIdTo);
        addDeliveryTask(cargo1.getId(), true);

        return cargo1;
    }

    private DropoffCargo addDropOffCargoDeliveryTaskDirect(String barcode) {
        String logisticPointIdFrom = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo(barcode, "test", logisticPointIdFrom);
        addDeliveryTask(cargo1.getId(), false);

        return cargo1;
    }

    private void addDeliveryTask(Long cargoId, boolean isReturn) {
        userShiftCommandService.addDeliveryTask(
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
