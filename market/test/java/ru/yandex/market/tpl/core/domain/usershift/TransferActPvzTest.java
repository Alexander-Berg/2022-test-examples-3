package ru.yandex.market.tpl.core.domain.usershift;


import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrder;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.service.demo.ManualService;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.transferact.TransferActPushCouriersService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.LOADING_PENDING_SIGN;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.LOADING_WAITING_SIGN;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.NOT_STARTED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.ORDERS_LOADED;
import static ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus.UNLOADING_WAITING_SIGN;

@RequiredArgsConstructor
public class TransferActPvzTest extends TplAbstractTest {
    private static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    private static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String RETURN_EXTERNAL_ORDER_ID_1 = "RETURN_EXTERNAL_ORDER_ID_1";
    public static final String UNKNOWN_ORDER_ID = "UNKNOWN_ORDER_ID";


    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final OrderGenerateService orderGenerateService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService commandService;
    private final TransactionTemplate tt;
    private final ManualService manualService;
    private final LockerDeliveryService lockerDeliveryService;
    private final OrderManager orderManager;
    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TplTestCargoFactory tplTestCargoFactory;
    private final TransferActPushCouriersService transferActPushCouriersService;
    private final OrderRepository orderRepository;


    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order1;
    private Order order2;
    private Order orderToReturn;
    private LockerDeliveryTask lockerDeliveryTask;
    private RoutePoint routePoint;
    private PickupPoint pickupPoint;

    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L));
        pickupPoint.setTransferActEnabled(true);
    }

    void initDeliveryTask() {
        init();
        var userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order1 = getPickupOrder(EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        order2 = getPickupOrder(EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint,
                OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

        orderToReturn = getPickupOrder(RETURN_EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                OrderFlowStatus.DELIVERED_TO_PICKUP_POINT, 1);

        userShiftReassignManager.assign(userShift, order1);
        userShiftReassignManager.assign(userShift, order2);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();

        testUserHelper.checkinAndFinishPickup(userShift);
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
    }


    @Test
    void shouldChangeTaskStatusTo_ORDERS_LOADED() {
        tt.execute(a -> {
            initDeliveryTask();
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId(), order1.getId()))
                            .build()));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_WAITING_SIGN);


            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_1));


            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_PENDING_SIGN);

            var dest = lockerDeliveryService.getDestinationTasks(lockerDeliveryTask.getId(), user);

            assertThat(dest.getTaskId()).isEqualTo(lockerDeliveryTask.getId());
            assertThat(dest.getStatus()).isEqualTo(lockerDeliveryTask.getStatus());
            assertThat(dest.getType()).isEqualTo(lockerDeliveryTask.getType());
            assertThat(dest.getCompletedDestination().getOutsideOrders().stream()
                    .map(DestinationOrderDto::getExternalOrderId)
                    .collect(Collectors.toSet()).contains(EXTERNAL_ORDER_ID_1)).isTrue();

            assertThat(dest.getSkippedDestination().getOutsideOrders().stream()
                    .map(DestinationOrderDto::getExternalOrderId)
                    .collect(Collectors.toSet()).contains(EXTERNAL_ORDER_ID_2)).isTrue();


            commandService.signLoading(new UserShiftCommand.SignLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    Map.of(),
                    user
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(ORDERS_LOADED);
            return 0;
        });

    }

    @Test
    void shouldCompletedAndSkippedBeNull() {
        tt.execute(a -> {
            initCargo();
            String logisticPointIdTo = pickupPoint.getLogisticPointId().toString();
            DropoffCargo cargo1 = tplTestCargoFactory.createCargo("BAG-001", logisticPointIdTo);
            DropoffCargo cargo2 = tplTestCargoFactory.createCargo("BAG-002", logisticPointIdTo);

            tplTestCargoFactory.initPickupCargoFlow(
                    TplTestCargoFactory.CargoPickupContext.of(List.of(), Set.of(cargo1.getId(), cargo2.getId()),
                            Set.of()),
                    TplTestCargoFactory.ShiftContext.of(user, userShift.getId())
            );
            routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
            lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of())
                            .successfullyScannedDropoffCargos(Set.of(cargo1.getId(), cargo2.getId()))
                            .build()));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(ORDERS_LOADED);

            var dest = lockerDeliveryService.getDestinationTasks(lockerDeliveryTask.getId(), user);

            assertThat(dest.getTaskId()).isEqualTo(lockerDeliveryTask.getId());
            assertThat(dest.getStatus()).isEqualTo(lockerDeliveryTask.getStatus());
            assertThat(dest.getType()).isEqualTo(lockerDeliveryTask.getType());
            assertThat(dest.getCompletedDestination()).isNull();
            assertThat(dest.getSkippedDestination()).isNull();
            return 0;
        });

    }

    void initCargo() {
        init();
        var movementReturn = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouseTo(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build());


        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        var userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        commandService.addDeliveryTask(
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
    void skipTransferActTestWhen_LOADING_WAITING_SIGN() {
        tt.executeWithoutResult(s -> {
            initDeliveryTask();
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId(), order1.getId()))
                            .build())
            );

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_WAITING_SIGN);
        });

        transferActPushCouriersService.processCourier(user.getId());

        tt.executeWithoutResult(s -> {
            var task = userShiftRepository.findByIdOrThrow(userShift.getId())
                    .streamTasks(LockerDeliveryTask.class)
                    .filter(t -> Objects.equals(lockerDeliveryTask.getId(), t.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(ORDERS_LOADED);
            var orders = task.streamSubtask()
                    .filter(st -> Objects.equals(st.getOrderId(), order1.getId()) || Objects.equals(st.getOrderId(), order2.getId()))
                    .peek(st -> assertThat(st.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED))
                    .map(st -> orderRepository.findByIdOrThrow(st.getOrderId()))
                    .toList();

            orders.forEach(order -> assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT));

            // после завершения задания заказы должны быть доставлены
            commandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            Set.of()
                    ));
            orders.forEach(order -> assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT));
        });
    }

    @Test
    void skipTransferActTestWhen_LOADING_PENDING_SIGN() {
        tt.executeWithoutResult(s -> {
            initDeliveryTask();
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId(), order1.getId()))
                            .build())
            );
            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_1, EXTERNAL_ORDER_ID_2));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_PENDING_SIGN);
        });

        transferActPushCouriersService.processCourier(user.getId());

        tt.executeWithoutResult(s -> {
            var task = userShiftRepository.findByIdOrThrow(userShift.getId())
                    .streamTasks(LockerDeliveryTask.class)
                    .filter(t -> Objects.equals(lockerDeliveryTask.getId(), t.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(ORDERS_LOADED);
            var orders = task.streamSubtask()
                    .filter(st -> Objects.equals(st.getOrderId(), order1.getId())
                            || Objects.equals(st.getOrderId(), order2.getId()))
                    .peek(st -> assertThat(st.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED))
                    .map(st -> orderRepository.findByIdOrThrow(st.getOrderId()))
                    .toList();
            orders.forEach(order -> assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT));

            // после завершения задания заказы должны быть доставлены
            commandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            Set.of()
                    ));
            orders.forEach(order -> assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT));
        });
    }

    @Test
    void skipTransferActTestWhen_UNLOADING_WAITING_SIGN() {
        tt.executeWithoutResult(s -> {
            initDeliveryTask();
            orderManager.cancelOrder(order1);
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId()))
                            .build()));


            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_2));
            commandService.signLoading(new UserShiftCommand.SignLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    Map.of(),
                    user
            ));

            commandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            Set.of(
                                    new UnloadedOrder(order1.getExternalOrderId(), null,
                                            getPlaceBarcodes(order1)),
                                    new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                            getPlaceBarcodes(orderToReturn))
                            )
                    ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(UNLOADING_WAITING_SIGN);
        });

        transferActPushCouriersService.processCourier(user.getId());

        tt.executeWithoutResult(s -> {
            var task = userShiftRepository.findByIdOrThrow(userShift.getId())
                    .streamTasks(LockerDeliveryTask.class)
                    .filter(t -> Objects.equals(lockerDeliveryTask.getId(), t.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(task.getStatus()).isEqualTo(FINISHED);
            task.streamSubtask()
                    .filter(st -> Objects.equals(st.getOrderId(), orderToReturn.getId()))
                    .forEach(st -> {
                        assertThat(st.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
                        var order = orderRepository.findByIdOrThrow(st.getOrderId());
                        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
                    });
        });
    }

    @Test
    void shouldChangeTaskStatusTo_NOT_STARTED() {
        tt.execute(a -> {
            initDeliveryTask();
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId(), order1.getId()))
                            .build()));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_WAITING_SIGN);


            commandService.cancelLoading(new UserShiftCommand.CancelLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId()
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(NOT_STARTED);
            return 0;
        });

    }

    @Test
    void shouldChangeTaskStatusTo_NOT_STARTED_when_LOADING_PENDING_SIGN() {
        tt.execute(a -> {
            initDeliveryTask();
            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId(), order1.getId()))
                            .build()));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_WAITING_SIGN);

            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_1, EXTERNAL_ORDER_ID_2));


            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LOADING_PENDING_SIGN);

            commandService.cancelLoading(new UserShiftCommand.CancelLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId()
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(NOT_STARTED);
            return 0;
        });

    }


    @Test
    void shouldSwitchTransferActFlag() {
        tt.execute(a -> {
            initDeliveryTask();
            assertThat(lockerDeliveryTask.getTransferActEnabled()).isTrue();
            lockerDeliveryService.switchTransferAct(lockerDeliveryTask.getId(), false, user);
            assertThat(lockerDeliveryTask.getTransferActEnabled()).isFalse();
            lockerDeliveryService.switchTransferAct(lockerDeliveryTask.getId(), true, user);
            assertThat(lockerDeliveryTask.getTransferActEnabled()).isTrue();
            return 0;
        });

    }

    @Test
    void shouldChangeStatusTo_UNLOADING_PENDING_SIGN() {
        tt.execute(a -> {
            initDeliveryTask();
            orderManager.cancelOrder(order1);

            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());

            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId()))
                            .build()));


            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_2));

            commandService.signLoading(new UserShiftCommand.SignLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    Map.of(),
                    user
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(ORDERS_LOADED);

            commandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            Set.of(
                                    new UnloadedOrder(order1.getExternalOrderId(), null, List.of()),
                                    new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                            getPlaceBarcodes(orderToReturn)),
                                    new UnloadedOrder(UNKNOWN_ORDER_ID, null, null))
                    ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(UNLOADING_WAITING_SIGN);
            commandService.continueUnloading(user,
                    new UserShiftCommand.ContinueUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId()
                    ));


            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(FINISHED);


            return 0;
        });

    }

    @Test
    void shouldCancelUnloading() {
        tt.execute(a -> {
            initDeliveryTask();

            testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());

            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                            lockerDeliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(List.of(order2.getId()))
                            .build()));


            manualService.signedByPvz(userShift.getId(), lockerDeliveryTask.getId(),
                    List.of(EXTERNAL_ORDER_ID_2));

            commandService.signLoading(new UserShiftCommand.SignLoading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId(),
                    Map.of(),
                    user
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(ORDERS_LOADED);

            commandService.finishUnloadingLocker(user,
                    new UserShiftCommand.FinishUnloadingLocker(
                            routePoint.getUserShift().getId(),
                            routePoint.getId(),
                            lockerDeliveryTask.getId(),
                            Set.of(
                                    new UnloadedOrder(order1.getExternalOrderId(), null, List.of()),
                                    new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                            getPlaceBarcodes(orderToReturn)),
                                    new UnloadedOrder(UNKNOWN_ORDER_ID, null, null))
                    ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(UNLOADING_WAITING_SIGN);
            assertThat(lockerDeliveryTask.getTransferActs().size()).isEqualTo(2);
            commandService.cancelUnloading(new UserShiftCommand.CancelUnloading(
                    userShift.getId(),
                    routePoint.getId(),
                    lockerDeliveryTask.getId()
            ));

            assertThat(lockerDeliveryTask.getStatus()).isEqualTo(ORDERS_LOADED);

            return 0;
        });
    }

    private List<String> getPlaceBarcodes(Order order) {
        return StreamEx.of(order.getPlaces())
                .map(OrderPlace::getBarcode)
                .filter(Objects::nonNull)
                .map(OrderPlaceBarcode::getBarcode)
                .toList();
    }


    private Order getPickupOrder(
            String externalOrderId,
            PickupPoint pickupPoint,
            GeoPoint geoPoint,
            OrderFlowStatus orderFlowStatus,
            int placeCount
    ) {
        OrderGenerateService.OrderGenerateParam.OrderGenerateParamBuilder orderGenerateParamBuilder =
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(externalOrderId)
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(239L)
                        .pickupPoint(pickupPoint)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .flowStatus(orderFlowStatus);

        if (placeCount > 1) {
            List<OrderPlaceDto> places = new ArrayList<>();
            for (int i = 0; i < placeCount; i++) {
                places.add(OrderPlaceDto.builder()
                        .barcode(new OrderPlaceBarcode("145", externalOrderId + "-" + (i + 1)))
                        .build());
            }

            orderGenerateParamBuilder.places(places);
        }

        Order order = orderGenerateService.createOrder(
                orderGenerateParamBuilder
                        .build());


        scManager.createOrders();
        ScOrder scOrder = scOrderRepository.findByYandexIdAndPartnerId(order.getExternalOrderId(),
                        shift.getSortingCenter().getId())
                .orElseThrow();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), "SC-" + scOrder.getId(), scOrder.getPartnerId());
        scManager.updateOrderStatuses(order.getExternalOrderId(), scOrder.getPartnerId(), List.of(
                new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24)),
                new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24))
        ));
        return order;
    }
}
