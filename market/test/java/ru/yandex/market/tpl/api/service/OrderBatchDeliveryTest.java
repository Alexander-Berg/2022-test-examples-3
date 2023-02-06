package ru.yandex.market.tpl.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.controller.api.OrderPickupTaskController;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatchCommand;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatchCommandService;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatchPlaceDto;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
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
import ru.yandex.market.tpl.core.domain.user.UserPropertyRepository;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.Task;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderBatchDeliveryTest extends BaseApiTest {

    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandDataHelper helper;
    private final OrderBatchCommandService orderBatchCommandService;
    private final UserPropertyRepository userPropertyRepository;
    private final OrderPickupTaskController orderPickupTaskController;

    private User user;
    private Shift shift;
    private Long userShiftId;

    private Order order;
    private Order order2;

    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    @Test
    void test() {
        user = testUserHelper.findOrCreateUser(1L);

        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_UPDATE_CANCEL_SC_130_160, true);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);

        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = getPickupOrder("1", pickupPoint, geoPoint);
        order2 = getPickupOrder("2", pickupPoint, geoPoint);

        userShiftReassignManager.assign(userShift, order);
        userShiftReassignManager.assign(userShift, order2);
        putInABatch("batch_barcode", order, order2);

        userShiftCommandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        RoutePoint pickupRoutePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();
        var pickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();
        LockerSubtask order2Subtask = userShift.streamLockerDeliveryTasks()
                .map(LockerDeliveryTask::getSubtasks)
                .flatMap(Collection::stream)
                .filter(lds -> Objects.equals(lds.getOrderId(), order2.getId()))
                .findFirst()
                .orElseThrow();
        userShiftCommandService.arriveAtRoutePoint(userShift.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        userShiftCommandService.failDeliverySubtask(null, new UserShiftCommand.FailOrderDeliverySubtask(
                userShiftId,
                order2Subtask.getTask().getRoutePoint().getId(),
                order2Subtask,
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.BIG_PLACE_FOR_FREE_CELLS, "")
        ));
        userShiftCommandService.startOrderPickup(userShift.getUser(),
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        pickupTask.getId()
                ));

        orderPickupTaskController.pickupOrders(
                pickupTask.getId(),
                OrderScanTaskRequestDto.builder()
                        .scannedBatches(Set.of("batch_barcode"))
                        .scannedOutsidePlaces(Set.of())
                        .completedOrders(List.of())
                        .skippedOrders(List.of())
                        .build(),
                user
        );
        userShiftCommandService.finishLoading(
                userShift.getUser(),
                new UserShiftCommand.FinishLoading(
                        userShift.getId(),
                        pickupRoutePoint.getId(),
                        pickupTask.getId()));

        this.routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) this.routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(2);

        testUserHelper.arriveAtRoutePoint(this.routePoint);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(this.routePoint.getUserShift().getId(),
                        this.routePoint.getId(), lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));

        //then
        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        Map<Long, LockerSubtask> orderIdToSubtaskMap = savedTask.getSubtasks().stream()
                .collect(Collectors.toMap(LockerSubtask::getOrderId, st -> st));
        assertThat(orderIdToSubtaskMap.get(order.getId()).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.ORDERS_LOADED);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
    }

    private void putInABatch(String batchBarcode, Order... orders) {
        var places = Arrays.stream(orders)
                .map(Order::getPlaces)
                .flatMap(Collection::stream)
                .map(p -> new OrderBatchPlaceDto(p.getBarcode().getBarcode(), p.getOrder().getExternalOrderId()))
                .collect(Collectors.toList());
        orderBatchCommandService.put(new OrderBatchCommand.Put(batchBarcode, places));
    }

    private Order getPickupOrder(
            String externalOrderId,
            PickupPoint pickupPoint,
            GeoPoint geoPoint
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
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                        .places(List.of(
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place1")).build(),
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place2")).build()
                        ));

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

    private LockerDeliveryTask fetchLockerDeliveryTask() {
        Optional<Task<?>> tasks = entityManager.find(RoutePoint.class, routePoint.getId()).streamTasks().findFirst();
        return (LockerDeliveryTask) tasks.orElseThrow();
    }

}
