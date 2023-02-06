package ru.yandex.market.tpl.api.controller.api.task;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseSchedule;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static ru.yandex.market.tpl.core.domain.movement.Movement.TAG_DROPOFF_CARGO_RETURN;

@Service
@RequiredArgsConstructor
public class TestDropOffCourierFlowFactory {

    public static final String LOGISTICPOINT_ID_FOR_RETURN_DROPOFF = "1234567";
    public static final String LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2 = "994567";
    public static final String LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF = "7654321";
    public static final String LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2 = "9874321";
    public static final String LOGISTICPOINT_ID_SC = "5555555";
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService commandService;
    private final MovementGenerator movementGenerator;
    private final UserShiftCommandDataHelper helper;
    private final AddressGenerator addressGenerator;
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final OrderGenerateService orderGenerateService;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;

    private final Clock clock;

    public DropoffCargo addDropoffCargoReturn(String barcode) {
        return addDropoffCargo(barcode, LOGISTICPOINT_ID_SC, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);
    }

    public DropoffCargo addDropoffCargoDirect(String barcode) {
        return addDropoffCargo(barcode, LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF, LOGISTICPOINT_ID_SC);
    }

    public DropoffCargo addDropoffCargo(String barcode, String logisticPointFrom, String logisticPointTo) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom(logisticPointFrom)
                        .logisticPointIdTo(logisticPointTo)
                        .build());
    }

    /**
     * Команда для создания флоу UserShift (3 рутпоинта с тасками):
     * Смешаный флоу с успешной приемкой "Возарвтов" в СЦ
     * OrderPickupTask - LockerDeliveryTask (с сабтаской на DropOff и на DropoffReturn ) - OrderReturnTask
     * Init в статусе - на загрузке в СЦ, OrderPickupTask
     */
    public CreatedEntityDto initDirectFlowLockerTaskMixed() {

        var movementDropoffDirect = movementGenerator.generate(buildCreateMovmentDirectCommand());
        var movementDropoffReturn = movementGenerator.generate(buildCreateMovmentReturnCommand());
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF),
                        1L)
        );

        CreatedEntityDto initFlowDto = CreatedEntityDto.builder()
                .movementDropoffReturn(movementDropoffReturn)
                .movementDropoffDirect(movementDropoffDirect)
                .pickupPointId(pickupPoint.getId())
                .build();

        CreatedEntityDto createdFlow = initUserShift(initFlowDto);

        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(createdFlow.getUserShift().getId());

            List<String> barcodes = List.of("barcode1Return", "barcode2Return");
            var dropoffReturnsCargos = barcodes
                    .stream().map(this::addDropoffCargoReturn)
                    .collect(Collectors.toList());

            createdFlow.setSucceedScannedReturnCargos(dropoffReturnsCargos);


            if (expectedPickupCreation(createdFlow)) {
                finishPickupProcess(createdFlow);
                testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            }

            var lockerTask = userShift.streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();

            createdFlow.setLockerTask(lockerTask);
            return null;
        });

        List<String> barcodes = List.of("barcode1Direct", "barcode2Direct");
        var dropoffDirectCargos = barcodes
                .stream().map(this::addDropoffCargoDirect)
                .collect(Collectors.toList());

        createdFlow.setSucceedScannedDirectCargos(dropoffDirectCargos);
        return createdFlow;
    }

    /**
     * Команда для создания флоу UserShift (2 рутпоинта с тасками):
     * LockerDeliveryTask (с одной сабтаской на DropOff) - OrderReturnTask
     * Init в статусе - на загрузке в СЦ, OrderPickupTask
     */
    public CreatedEntityDto initDirectFlowLockerTaskState() {

        var movementDropoffDirect = movementGenerator.generate(buildCreateMovmentDirectCommand());
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2),
                        1L)
        );

        CreatedEntityDto initFlowDto = CreatedEntityDto.builder()
                .movementDropoffDirect(movementDropoffDirect)
                .pickupPointId(pickupPoint.getId())
                .build();

        CreatedEntityDto createdFlow = initUserShift(initFlowDto);

        transactionTemplate.execute(status -> {

            var userShiftId = createdFlow.getUserShift().getId();
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            if (expectedPickupCreation(createdFlow)) {
                finishPickupProcess(createdFlow);
            }
            testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());

            var lockerTask = userShift.streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();

            createdFlow.setLockerTask(lockerTask);
            createdFlow.setUserShift(userShift);

            return null;
        });

        List<String> barcodes = List.of("barcode1", "barcode2");
        var dropoffDirectCargos = barcodes
                .stream().map(this::addDropoffCargoDirect)
                .collect(Collectors.toList());
        createdFlow.setSucceedScannedDirectCargos(dropoffDirectCargos);
        return createdFlow;
    }

    /**
     * Команда для создания флоу UserShift (3 рутпоинта с тасками - ):
     * LockerDeliveryTask #1 - LockerDeliveryTask #2 (с одной сабтаской на DropOff) - OrderReturnTask
     * Init в статусе - на загрузке в СЦ, OrderPickupTask
     */
    public CreatedEntityDto initDirectSeveralFlowLockerTaskState() {

        var movementDropoffDirect = movementGenerator.generate(buildCreateMovmentDirectCommand());
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF),
                        1L)
        );

        var movementDropoffDirect2 = movementGenerator.generate(buildCreateMovmentCommand(
                LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2, LOGISTICPOINT_ID_SC, List.of()
        ));
        var pickupPoint2 = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF2),
                        1L)
        );

        CreatedEntityDto initFlowDto = CreatedEntityDto.builder()
                .movementDropoffDirect(movementDropoffDirect)
                .movementDropoffDirect2(movementDropoffDirect2)
                .pickupPointId(pickupPoint.getId())
                .pickupPointId2(pickupPoint2.getId())
                .build();

        CreatedEntityDto createdFlow = initUserShift(initFlowDto);

        transactionTemplate.execute(status -> {

            var userShiftId = createdFlow.getUserShift().getId();
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            if (expectedPickupCreation(createdFlow)) {
                finishPickupProcess(createdFlow);
                testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            }
            var lockerTask = userShift.streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();

            createdFlow.setLockerTask(lockerTask);
            createdFlow.setUserShift(userShift);

            return null;
        });

        List<String> barcodes = List.of("barcode1", "barcode2");
        var dropoffDirectCargos = barcodes
                .stream().map(this::addDropoffCargoDirect)
                .collect(Collectors.toList());
        createdFlow.setSucceedScannedDirectCargos(dropoffDirectCargos);
        return createdFlow;
    }

    /**
     * Команда для создания флоу UserShift (3 рутпоинта с тасками):
     * OrderPickupTask - LockerDeliveryTask (с одной сабтаской на DropOffReturn) - OrderReturnTask
     * Init в статусе - на загрузке в СЦ, OrderPickupTask
     */
    public CreatedEntityDto initReturnFlowPickupTaskState() {

        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        CreatedEntityDto.CreatedEntityDtoBuilder resultBuilder = CreatedEntityDto.builder();

        var movementDropoffReturn = movementGenerator.generate(buildCreateMovmentReturnCommand());

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF),
                        1L)
        );

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var createCommand = buildCreateUserShiftCommandReturn(shift, user, movementDropoffReturn,
                order.getId(), pickupPoint.getId());

        Long userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));

        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            testUserHelper.openShift(user, userShift.getId());
            testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            var pickupTask = userShift.streamPickupRoutePoints()
                    .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
            commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                    userShift.getId(), userShift.getCurrentRoutePoint().getId(), pickupTask.getId()
            ));

            resultBuilder
                    .userShift(userShift)
                    .pickupTask(pickupTask);

            return null;
        });

        List<String> barcodes = List.of("barcode1", "barcode2");
        var dropoffCargos = barcodes
                .stream().map(this::addDropoffCargoReturn)
                .collect(Collectors.toList());


        return resultBuilder
                .user(user)
                .movementDropoffReturn(movementDropoffReturn)
                .lockerOrder(order)
                .succeedScannedReturnCargos(dropoffCargos)
                .build();
    }

    /**
     * Команда для создания флоу UserShift (4 рутпоинта с тасками):
     * - OrderPickupTask
     * - LockerDeliveryTask (с одной сабтаской на DropOffReturn)
     * - LockerDeliveryTask (с одной сабтаской на DropOffReturn)
     * - OrderReturnTask
     * Init в статусе - на загрузке в СЦ, OrderPickupTask
     */
    public CreatedEntityDto initReturnSeveralFlowPickupTaskState() {

        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        CreatedEntityDto.CreatedEntityDtoBuilder resultBuilder = CreatedEntityDto.builder();

        var movementDropoffReturn = movementGenerator.generate(buildCreateMovmentReturnCommand());

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF),
                        1L)
        );

        var movementDropoffReturn2 = movementGenerator.generate(
                buildCreateMovmentCommand(LOGISTICPOINT_ID_SC, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2,
                        List.of(TAG_DROPOFF_CARGO_RETURN))
        );
        var pickupPoint2 = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF2),
                        1L)
        );

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId(), pickupPoint.getId()))
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn2.getId(), pickupPoint2.getId()))
                .build();

        Long userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));

        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            testUserHelper.openShift(user, userShift.getId());
            testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            var pickupTask = userShift.streamPickupRoutePoints()
                    .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
            commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                    userShift.getId(), userShift.getCurrentRoutePoint().getId(), pickupTask.getId()
            ));

            resultBuilder
                    .userShift(userShift)
                    .pickupTask(pickupTask);

            return null;
        });

        List<String> barcodes = List.of("barcode1", "barcode2");
        var dropoffCargos = barcodes
                .stream().map(this::addDropoffCargoReturn)
                .collect(Collectors.toList());


        return resultBuilder
                .user(user)
                .pickupPointId2(pickupPoint2.getId())
                .pickupPointId(pickupPoint.getId())
                .movementDropoffReturn(movementDropoffReturn)
                .movementDropoffReturn2(movementDropoffReturn2)
                .lockerOrder(order)
                .succeedScannedReturnCargos(dropoffCargos)
                .build();
    }

    private CreatedEntityDto initUserShift(CreatedEntityDto initFlowDto) {
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = buildCreateUserShiftCommandReturn(shift, user, initFlowDto);

        Long userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));

        CreatedEntityDto.CreatedEntityDtoBuilder resultBuilder = CreatedEntityDto.builder()
                .pickupPointId(initFlowDto.getPickupPointId())
                .pickupPointId2(initFlowDto.getPickupPointId2())
                .movementDropoffReturn(initFlowDto.getMovementDropoffReturn())
                .movementDropoffDirect(initFlowDto.getMovementDropoffDirect())
                .movementDropoffDirect2(initFlowDto.getMovementDropoffDirect2())
                .lockerOrder(initFlowDto.getLockerOrder());


        transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            testUserHelper.openShift(user, userShift.getId());

            resultBuilder
                    .userShift(userShift);

            if (expectedPickupCreation(initFlowDto)) {
                var pickupTask = userShift.streamPickupRoutePoints()
                        .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
                testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
                commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                        userShift.getId(), userShift.getCurrentRoutePoint().getId(), pickupTask.getId()
                ));
                resultBuilder
                        .pickupTask(pickupTask);
            }

            return null;
        });


        return resultBuilder
                .user(user)
                .build();
    }

    private boolean expectedPickupCreation(CreatedEntityDto initFlowDto) {
        return initFlowDto.getMovementDropoffReturn() != null ||
                initFlowDto.getLockerOrder() != null;
    }

    /**
     * Команда для создания флоу UserShift (3 рутпоинта с тасками):
     * OrderPickupTask - LockerDeliveryTask (с одной сабтаской на DropOffReturn) - OrderReturnTask
     * Init в статусе - на загрузке в ПВЗ, LockerDeliveryTask
     */
    public CreatedEntityDto initReturnFlowLockerTaskState() {

        CreatedEntityDto createdEntityDto = initReturnFlowPickupTaskState();
        CreatedEntityDto.CreatedEntityDtoBuilder resultBuilder = CreatedEntityDto.builder();

        var dropoffCargos = createdEntityDto.getSucceedScannedReturnCargos();


        transactionTemplate.execute(status -> {
            var userShiftId = createdEntityDto.getUserShift().getId();
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            var lockerTask = userShift.streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();

            if (expectedPickupCreation(createdEntityDto)) {
                finishPickupProcess(createdEntityDto);
                testUserHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            }

            resultBuilder
                    .succeedScannedReturnCargos(dropoffCargos)
                    .lockerTask(lockerTask);
            return null;
        });

        return resultBuilder
                .user(createdEntityDto.getUser())
                .movementDropoffReturn(createdEntityDto.getMovementDropoffReturn())
                .userShift(createdEntityDto.getUserShift())
                .pickupTask(createdEntityDto.getPickupTask())
                .lockerOrder(createdEntityDto.getLockerOrder())
                .build();

    }

    public void finishPickupProcess(CreatedEntityDto createdEntityDto) {
        transactionTemplate.execute(status -> {
            var userShiftId = createdEntityDto.getUserShift().getId();
            var pickupTask = createdEntityDto.getPickupTask();
            var dropoffCargos = createdEntityDto.getSucceedScannedReturnCargos();

            commandService.pickupOrders(createdEntityDto.getUser(),
                    new UserShiftCommand.FinishScan(userShiftId,
                            pickupTask.getRoutePoint().getId(),
                            pickupTask.getId(),
                            ScanRequest.builder()
                                    .successfullyScannedDropoffCargos(
                                            Optional.ofNullable(dropoffCargos)
                                                    .orElseGet(List::of)
                                                    .stream().map(DropoffCargo::getId).collect(Collectors.toSet()))
                                    .skippedDropoffCargos(Set.of())
                                    .build()
                    )
            );

            commandService.finishLoading(
                    createdEntityDto.getUser(),
                    new UserShiftCommand.FinishLoading(
                            userShiftId,
                            pickupTask.getRoutePoint().getId(),
                            pickupTask.getId()
                    )
            );
            return null;
        });
    }


    private UserShiftCommand.Create buildCreateUserShiftCommandReturn(Shift shift, User user,
                                                                      Movement movementDropoffReturn,
                                                                      Long orderId,
                                                                      Long pickupPointId) {

        return UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskLockerDelivery(orderId, pickupPointId))
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId(), pickupPointId))
                .build();
    }

    private UserShiftCommand.Create buildCreateUserShiftCommandReturn(Shift shift, User user,
                                                                      CreatedEntityDto createdEntityDto) {

        UserShiftCommand.Create.CreateBuilder createBuilder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true);

        Optional.ofNullable(createdEntityDto.getLockerOrder())
                .ifPresent(lockerOrder -> createBuilder
                        .routePoint(helper.taskLockerDelivery(lockerOrder.getId(),
                                createdEntityDto.getPickupPointId())));

        Optional.ofNullable(createdEntityDto.getMovementDropoffReturn())
                .ifPresent(movementDropoffReturn -> createBuilder
                        .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId(),
                                createdEntityDto.getPickupPointId())));

        Optional.ofNullable(createdEntityDto.getMovementDropoffDirect())
                .ifPresent(movementDropoffDirect -> createBuilder
                        .routePoint(helper.taskDropOff(movementDropoffDirect.getId(),
                                createdEntityDto.getPickupPointId(), false)));

        Optional.ofNullable(createdEntityDto.getMovementDropoffDirect2())
                .ifPresent(movementDropoffDirect -> createBuilder
                        .routePoint(helper.taskDropOff(movementDropoffDirect.getId(),
                                createdEntityDto.getPickupPointId2(), false)));

        return createBuilder.build();
    }

    public MovementCommand.Create buildCreateMovmentReturnCommand() {
        return buildCreateMovmentCommand(LOGISTICPOINT_ID_SC, LOGISTICPOINT_ID_FOR_RETURN_DROPOFF,
                List.of(TAG_DROPOFF_CARGO_RETURN));
    }

    public MovementCommand.Create buildCreateMovmentDirectCommand() {
        return buildCreateMovmentCommand(LOGISTICPOINT_ID_FOR_DIRECT_DROPOFF, LOGISTICPOINT_ID_SC,
                null);
    }

    public MovementCommand.Create buildCreateMovmentCommand(String logisticPointFrom, String logisticPointTo,
                                                            List<String> tags) {
        return MovementCommand.Create.builder()
                .orderWarehouseTo(orderWarehouseRepository.saveAndFlush(new OrderWarehouse(
                        logisticPointTo,
                        "corp",
                        addressGenerator.generateWarehouseAddress(
                                AddressGenerator.AddressGenerateParam.builder()
                                        .street("Пушкина")
                                        .house("Колотушкина")
                                        .apartment("10")
                                        .floor(1)
                                        .build()
                        ),
                        Arrays.stream(DayOfWeek.values())
                                .collect(Collectors.toMap(
                                        d -> d,
                                        d -> new OrderWarehouseSchedule(
                                                d,
                                                OffsetTime.of(LocalTime.of(9, 27), DateTimeUtil.DEFAULT_ZONE_ID),
                                                OffsetTime.of(LocalTime.of(10, 27), DateTimeUtil.DEFAULT_ZONE_ID)
                                        )
                                ))
                        ,
                        List.of("223322223322"),
                        "Спросить старшего",
                        "Иван Дропшипов")))
                .orderWarehouse(orderWarehouseRepository.saveAndFlush(new OrderWarehouse(
                        logisticPointFrom,
                        "corp",
                        new OrderWarehouseAddress("abc", "cde", "efg", "ads", "asd", "12", "2", "1",
                                1, BigDecimal.ONE, BigDecimal.ONE),
                        Map.of(),
                        List.of("777777"),
                        "Спросить старшего",
                        "Иван Дропшипов")))
                .tags(tags)
                .build();
    }

    @Data
    @Builder
    public static class CreatedEntityDto {
        private UserShift userShift;
        private OrderPickupTask pickupTask;
        private LockerDeliveryTask lockerTask;
        private Movement movementDropoffReturn;
        private Movement movementDropoffReturn2;
        private Movement movementDropoffDirect;
        private Movement movementDropoffDirect2;
        private User user;
        private Order lockerOrder;
        private Long pickupPointId;
        private Long pickupPointId2;
        private List<DropoffCargo> succeedScannedReturnCargos;
        private List<DropoffCargo> succeedScannedDirectCargos;
    }
}
