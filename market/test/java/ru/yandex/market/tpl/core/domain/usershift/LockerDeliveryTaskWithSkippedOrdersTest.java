package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointReturnReason;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrder;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LockerDeliveryTaskWithSkippedOrdersTest {

    public static final String UNKNOWN_ORDER_ID = "UNKNOWN_ORDER_ID";
    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final OrderManager orderManager;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;


    private User user;
    private Shift shift;
    private Long userShiftId;

    private Order order;
    private Order orderSkipped;
    private Order orderToReturn;

    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER,
                1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = getPickupOrder(pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED);
        orderSkipped = getPickupOrder(pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED);
        orderToReturn = getPickupOrder(pickupPoint, geoPoint, OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);

        userShiftReassignManager.assign(userShift, order);
        userShiftReassignManager.assign(userShift, orderSkipped);
        testUserHelper.checkinAndFinishPickup(userShift, List.of(order.getId()), List.of(orderSkipped.getId()),
                true, true);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(2);

        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_UPDATE_CANCEL_SC_130_160, true);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
    }

    private Order getPickupOrder(PickupPoint pickupPoint, GeoPoint geoPoint, OrderFlowStatus orderFlowStatus) {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(239L)
                        .pickupPoint(pickupPoint)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .flowStatus(orderFlowStatus)
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

    @Test
    void save() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        List<LockerSubtask> subtasks = lockerDeliveryTask.getSubtasks();
        assertThat(subtasks).hasSize(2);
        assertThat(subtasks)
                .extracting(LockerSubtask::getStatus)
                .contains(LockerDeliverySubtaskStatus.NOT_STARTED);
        assertThat(subtasks)
                .extracting(LockerSubtask::getStatus)
                .contains(LockerDeliverySubtaskStatus.FAILED);
        assertThat(subtasks)
                .extracting(LockerSubtask::getOrderId)
                .containsExactlyInAnyOrder(order.getId(), orderSkipped.getId());
    }

    @Test
    void shouldCancelTaskWhenCancelOrders() {
        orderManager.cancelOrder(order);
        assertThatFailSubtasksAndCancelTask();
    }

    @Test
    void shouldCancelTaskWhenRescheduleOrders() {
        for (Order o : List.of(order, orderSkipped)) {
            orderManager.rescheduleOrder(o, new Interval(tomorrowAtHour(18, clock), tomorrowAtHour(20, clock)),
                    Source.DELIVERY);
        }

        assertThatFailSubtasksAndCancelTask();
    }

    private void assertThatFailSubtasksAndCancelTask() {
        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
        List<LockerSubtask> subtasks = savedTask.getSubtasks();
        assertThat(subtasks).extracting(LockerSubtask::getStatus)
                .containsOnly(LockerDeliverySubtaskStatus.FAILED);
    }

    private LockerDeliveryTask fetchLockerDeliveryTask() {
        List<Task<?>> tasks = entityManager.find(RoutePoint.class, routePoint.getId()).getTasks();
        return (LockerDeliveryTask) tasks.get(0);
    }

    @Test
    void shouldThrowIfFinishLoadingWithoutComment() {
        assertThatThrownBy(() -> {
            Set<Long> loadedOrderIds = Set.of();
            lockerDeliveryTask.finishLoadingLocker(
                    Instant.now(),
                    null,
                    ScanRequest.builder()
                            .successfullyScannedOrders(List.of())
                            .build()
            );
        })
                .isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void shouldNotShowCancelledLockerOrder() {

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, routePoint.getId());
        assertThat(routePointInfo.getType()).isEqualTo(RoutePointType.LOCKER_DELIVERY);
        List<OrderDto> orders = ((LockerDeliveryTaskDto) routePointInfo.getTasks().get(0)).getOrders();
        assertThat(orders).hasSize(1);
        assertThat(orders).extracting(OrderDto::getExternalOrderId)
                .containsOnly(order.getExternalOrderId());
    }

    @Test
    void shouldNotReturnNotPickupOrder() {
        finishLoadingLocker();

        Order deliveryOrder =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(deliveryOrder.getExternalOrderId(), null, List.of()))
                ));
        assertThat(orderRepository.findByIdOrThrow(deliveryOrder.getId()).getOrderFlowStatus())
                .isEqualTo(deliveryOrder.getOrderFlowStatus());
    }

    @Test
    void shouldThrowIfFinishUnloadingDuplicateClientReturnBarcode() {
        ClientReturn existedClientReturn = clientReturnGenerator.generate();

        finishLoadingLocker();

        assertThatThrownBy(() -> userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(existedClientReturn.getBarcode(), null, List.of()))
                ))
        )
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }


    @Test
    void finishTask() {
        finishLoadingLocker();
        finishUnloadingLocker();
    }

    @Test
    void reopenAndCancelLockerDeliveryTask() {
        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order.getId(), orderSkipped.getId()))
                        .build()));
        checkAssignment(user, order);

        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(
                                        order.getExternalOrderId(),
                                        PickupPointReturnReason.CELL_DID_NOT_OPEN,
                                        List.of()),
                                new UnloadedOrder(orderToReturn.getExternalOrderId(), null, List.of())
                        )));

        checkAssignment(user, order, orderToReturn);

        userShiftCommandService.reopenDeliveryTask(
                user,
                new UserShiftCommand.ReopenOrderDeliveryTask(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Source.COURIER)
        );

        checkAssignment(user, order);

        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(orderSkipped.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(orderSkipped.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(orderToReturn.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
        assertThat(orderToReturn.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
    }


    private void finishUnloadingLocker() {
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(order.getExternalOrderId(), null, List.of()),
                                new UnloadedOrder(orderToReturn.getExternalOrderId(), null, List.of()),
                                new UnloadedOrder(UNKNOWN_ORDER_ID, null, null))
                ));

        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
        assertThat(savedTask.getExternalOrderIdsToReturn())
                .containsExactlyInAnyOrder(orderToReturn.getExternalOrderId(), UNKNOWN_ORDER_ID);

        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(orderRepository.findByIdOrThrow(orderToReturn.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        checkAssignment(user, orderToReturn);
    }

    private void finishLoadingLocker() {
        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order.getId()))
                        .build()));

        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        Map<Long, LockerSubtask> orderIdToSubtaskMap = savedTask.getSubtasks().stream()
                .collect(Collectors.toMap(LockerSubtask::getOrderId, st -> st));
        assertThat(orderIdToSubtaskMap.get(order.getId()).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.ORDERS_LOADED);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        checkAssignment(user, order);
    }

    private void checkAssignment(User user, Order... orders) {
        List<Order> currentUserOrders = orderRepository.findCurrentUserOrders(user.getId());
        assertThat(currentUserOrders).containsExactlyInAnyOrder(orders);
    }

}
