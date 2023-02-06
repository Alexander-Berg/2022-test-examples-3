package ru.yandex.market.tpl.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.ApiIntTest;
import ru.yandex.market.tpl.api.controller.api.LockerDeliveryTaskController;
import ru.yandex.market.tpl.api.controller.api.OrderPickupTaskController;
import ru.yandex.market.tpl.api.model.locker.FinishMessage;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatch;
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
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ApiIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LockerDeliveryTaskWithBatchTest {

    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final UserPropertyService userPropertyService;
    private final OrderPickupTaskController orderPickupTaskController;
    private final LockerDeliveryTaskController lockerDeliveryTaskController;

    private Shift shift;

    @Test
    void test() {
        User user = testUserHelper.findOrCreateUser(1L);

        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        Long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        Order order = getPickupOrder(pickupPoint, geoPoint);
        String batchBarcode = StreamEx.of(order.getPlaces())
                .map(OrderPlace::getCurrentBatch)
                .flatMap(Optional::stream)
                .findFirst()
                .map(OrderBatch::getBarcode)
                .orElseThrow();

        userShiftReassignManager.assign(userShift, order);

        userShiftCommandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        UserShift userShift1 = userShift;
        userShiftCommandService.arriveAtRoutePoint(userShift1.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift1.getId(),
                        userShift1.streamPickupRoutePoints().findFirst().orElseThrow().getId(),
                        helper.getLocationDto(userShift1.getId())
                ));
        var routePoint1 = userShift1.streamPickupRoutePoints().findFirst().orElseThrow();
        var pickupTask = routePoint1.streamPickupTasks().findFirst().orElseThrow();

        orderPickupTaskController.startTask(pickupTask.getId(), user);
        orderPickupTaskController.pickupOrders(
                pickupTask.getId(),
                OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of())
                        .skippedOrders(List.of())
                        .scannedOutsidePlaces(Set.of())
                        .scannedBatches(Set.of(batchBarcode))
                        .build(),
                user
        );
        orderPickupTaskController.loadingFinished(pickupTask.getId(), user);


        var lockerDeliveryTask = userShift1.streamLockerDeliveryTasks().findFirst().orElseThrow();

        userShiftCommandService.arriveAtRoutePoint(userShift1.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift1.getId(),
                        lockerDeliveryTask.getRoutePoint().getId(),
                        helper.getLocationDto(userShift1.getId())
                ));

        lockerDeliveryTaskController.finishLoadingLocker(
                lockerDeliveryTask.getRoutePoint().getId(),
                lockerDeliveryTask.getId(),
                OrderScanTaskRequestDto.builder()
                        .completedOrders(List.of())
                        .skippedOrders(List.of())
                        .scannedOutsidePlaces(Set.of())
                        .scannedBatches(Set.of(batchBarcode))
                        .build(),
                user
        );
        lockerDeliveryTaskController.finishTask(lockerDeliveryTask.getId(), new FinishMessage(), user);
        assertThat(lockerDeliveryTask.getSubtasks().get(0).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);

    }

    private Order getPickupOrder(PickupPoint pickupPoint, GeoPoint geoPoint) {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(LockerDeliveryTaskWithBatchTest.EXTERNAL_ORDER_ID_1)
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(239L)
                        .pickupPoint(pickupPoint)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                        .places(List.of(
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("123", "asdf"))
                                        .build()
                        ))
                        .withBatch(true)
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
