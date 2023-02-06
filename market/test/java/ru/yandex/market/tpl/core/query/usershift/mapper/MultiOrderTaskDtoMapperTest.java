package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.CollectDropshipTakePhotoDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryAddressDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.PhotoRequirementType;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.TransferType;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.GenericTaskDtoMapper;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class MultiOrderTaskDtoMapperTest {

    private final TaskDtoMapper taskDtoMapper;
    private final GenericTaskDtoMapper genericTaskDtoMapper;
    private final OrderDtoMapper orderDtoMapper;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;

    private OrderDeliveryTask deliveryTask1;
    private OrderDeliveryTask deliveryTask2;
    private LockerDeliveryTask lockerDeliveryTask;
    private OrderPickupTask pickupTask;
    private OrderReturnTask returnTask;
    private Order order1;
    private Order order2;
    private Order lockerOrder;
    private Order lockerReturnOrder;
    private CallToRecipientTask callTask;

    @BeforeEach
    void init() {
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());
        lockerOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L,
                        1L)))
                .build());
        lockerReturnOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 2L,
                        1L)))
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .build());

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();

        RoutePointAddress my_address = new RoutePointAddress("my_address", geoPoint);
        deliveryTask1 = (OrderDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(my_address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(order1, false, false)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        deliveryTask2 = (OrderDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(my_address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(order2, false, false)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        lockerDeliveryTask = (LockerDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(my_address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(lockerOrder, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        var userShift = deliveryTask1.getRoutePoint().getUserShift();
        callTask = userShift.streamCallTasks().findFirst().orElseThrow();

        pickupTask = userShift.streamPickupRoutePoints()
                .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        var pickupRoutePoint = pickupTask.getRoutePoint();
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), pickupRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
        ));
        commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId()
        ));
        String comment = "my_comment";

        assertThat(taskDtoMapper.mapTasks(pickupTask.getRoutePoint(), List.of(), user)).isEqualTo(expectedPickupTasks());
        commandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order1.getId(), order2.getId(), lockerOrder.getId()))
                        .comment(comment)
                        .finishedAt(clock.instant())
                        .build()
        ));
        commandService.finishLoading(
                userShift.getUser(),
                new UserShiftCommand.FinishLoading(
                        userShift.getId(),
                        pickupRoutePoint.getId(), pickupTask.getId()));
        testUserHelper.finishCallTasksAtRoutePoint(pickupRoutePoint);
        testUserHelper.finishDelivery(deliveryTask1.getRoutePoint(), true);

        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShiftId, lockerDeliveryTask.getRoutePoint().getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
        ));
        commandService.finishLoadingLocker(user, new UserShiftCommand.FinishLoadingLocker(userShiftId,
                lockerDeliveryTask.getRoutePoint().getId(), lockerDeliveryTask.getId(), null, ScanRequest.builder()
                .successfullyScannedOrders(List.of(lockerOrder.getId()))
                .build()));
        commandService.finishUnloadingLocker(user, new UserShiftCommand.FinishUnloadingLocker(
                userShiftId, lockerDeliveryTask.getRoutePoint().getId(), lockerDeliveryTask.getId(),
                Set.of(new UnloadedOrder(lockerReturnOrder.getExternalOrderId(), null, List.of()))
        ));

        returnTask = userShift.streamReturnRoutePoints()
                .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
    }

    @Test
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void mapTasks() {
        List<TaskDto> tasks = taskDtoMapper.mapTasks(deliveryTask1.getRoutePoint(), List.of(order1, order2), null);
        assertThat(tasks)
                .containsExactlyInAnyOrder(
                        expectedDeliveryTasks(deliveryTask1, order1),
                        expectedDeliveryTasks(deliveryTask2, order2));
        List<TaskDto> actualReturnTasks = taskDtoMapper.mapTasks(returnTask.getRoutePoint(), List.of(), null);
        assertThat(actualReturnTasks)
                .usingElementComparatorIgnoringFields("orders")
                .isEqualTo(expectedReturnTasks());
        OrderReturnTaskDto actualReturnTask = (OrderReturnTaskDto) actualReturnTasks.iterator().next();

        assertThat(actualReturnTask.getOrders()).containsExactlyInAnyOrderElementsOf(expectedOrdersForReturn());

    }

    List<TaskDto> expectedPickupTasks() {
        var pickupTaskDto = new OrderPickupTaskDto();
        expectedDeliveryDto(order1);
        String multiOrderId = String.valueOf(callTask.getId());
        var orders = List.of(
                new OrderScanTaskDto.OrderForScanDto(true, multiOrderId,
                        this.order1.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 10:00",
                        null,
                        null,
                        OrderType.CLIENT,
                        List.of(new PlaceForScanDto(this.order1.getExternalOrderId())),
                        OrderFlowStatus.SORTING_CENTER_PREPARED,
                        expectedDeliveryDto(order1),
                        1
                ),
                new OrderScanTaskDto.OrderForScanDto(true, multiOrderId, order2.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 10:00",
                        null,
                        null,
                        OrderType.CLIENT,
                        List.of(new PlaceForScanDto(order2.getExternalOrderId())),
                        OrderFlowStatus.SORTING_CENTER_PREPARED,
                        expectedDeliveryDto(order2),
                        1
                ),
                new OrderScanTaskDto.OrderForScanDto(false, "pickup" + lockerDeliveryTask.getId(),
                        lockerOrder.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 10:00",
                        null,
                        null,
                        OrderType.LOCKER,
                        List.of(new PlaceForScanDto(lockerOrder.getExternalOrderId())),
                        OrderFlowStatus.SORTING_CENTER_PREPARED,
                        expectedDeliveryDto(lockerOrder),
                        2
                )
        );
        genericTaskDtoMapper.mapGenericTask(pickupTaskDto, pickupTask);
        pickupTaskDto.setStatus(OrderPickupTaskStatus.IN_PROGRESS);
        pickupTaskDto.setOrders(orders);
        pickupTaskDto.setCompletedOrders(List.of());
        pickupTaskDto.setSkippedOrders(List.of());
        pickupTaskDto.setComment(null);
        pickupTaskDto.setDestinations(List.of(
                ScanTaskDestinationDto.builder()
                        .ordinalNumber(1)
                        .type(OrderType.CLIENT)
                        .batches(Set.of())
                        .outsideOrders(Set.of(
                                DestinationOrderDto.builder()
                                        .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                        .externalOrderId(order1.getExternalOrderId())
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode(order1.getExternalOrderId())
                                                        .build()
                                        ))
                                        .displayMode(OrderScanTaskDto.ScanOrderDisplayMode.OK)
                                        .text("Доставка в 10:00")
                                        .build(),
                                DestinationOrderDto.builder()
                                        .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                        .externalOrderId(order2.getExternalOrderId())
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode(order2.getExternalOrderId())
                                                        .build()
                                        ))
                                        .displayMode(OrderScanTaskDto.ScanOrderDisplayMode.OK)
                                        .text("Доставка в 10:00")
                                        .build()
                        ))
                        .delivery(expectedDeliveryDto(order1))
                        .build(),
                ScanTaskDestinationDto.builder()
                        .ordinalNumber(2)
                        .type(OrderType.LOCKER)
                        .batches(Set.of())
                        .outsideOrders(Set.of(
                                DestinationOrderDto.builder()
                                        .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                        .externalOrderId(lockerOrder.getExternalOrderId())
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode(lockerOrder.getExternalOrderId())
                                                        .build()
                                        ))
                                        .displayMode(OrderScanTaskDto.ScanOrderDisplayMode.OK)
                                        .text("Доставка в 10:00")
                                        .build()
                        ))
                        .delivery(expectedDeliveryDto(lockerOrder))
                        .build()
        ));
        pickupTaskDto.setBatches(Set.of());
        return List.of(pickupTaskDto);
    }

    private OrderDeliveryDto expectedDeliveryDto(Order order) {
        OrderDelivery delivery = order.getDelivery();
        DeliveryAddress deliveryAddress = delivery.getDeliveryAddress();
        var deliveryDto = new OrderDeliveryDto(null,
                delivery.getDeliveryIntervalFrom(),
                delivery.getDeliveryIntervalTo(),
                delivery.getRecipientNotes(),
                delivery.getRecipientFio(),
                delivery.getRecipientFioPersonalId(),
                delivery.getRecipientPhone(),
                delivery.getRecipientPhone(),
                delivery.getRecipientPhonePersonalId(),
                delivery.getRecipientEmail(),
                delivery.getRecipientEmailPersonalId(),
                deliveryAddress.getLongitude(),
                deliveryAddress.getLatitude(),
                deliveryAddress.getGpsPersonalId(),
                deliveryAddress.getAddress(),
                deliveryAddress.getAddressPersonalId(),
                new OrderDeliveryAddressDto(
                        deliveryAddress.getCity(),
                        deliveryAddress.getStreet(),
                        String.format("д. %s", deliveryAddress.getHouse()),
                        deliveryAddress.getEntrance(),
                        deliveryAddress.getApartment(),
                        deliveryAddress.getFloor(),
                        deliveryAddress.getEntryPhone(),
                        null,
                        "1234"
                ),
                delivery.getCourierNotes(),
                null
        );
        return deliveryDto;
    }

    TaskDto expectedDeliveryTasks(OrderDeliveryTask deliveryTask1, Order order) {
        var deliveryTaskDto1 = new OrderDeliveryTaskDto();
        genericTaskDtoMapper.mapGenericTask(deliveryTaskDto1, deliveryTask1);
        deliveryTaskDto1.setStatus(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        deliveryTaskDto1.setMultiOrderId(String.valueOf(callTask.getId()));
        deliveryTaskDto1.setMultiOrder(true);
        deliveryTaskDto1.setOrder(orderDtoMapper.mapOrderDto(
                order, deliveryTask1.getExpectedDeliveryTime(), deliveryTask1.getOrdinalNumber()));
        deliveryTaskDto1.setFailReason(new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, "my comment!", Source.COURIER));
        deliveryTaskDto1.setActions(List.of(new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.REOPEN)));
        deliveryTaskDto1.setPhotos(Collections.emptyList());
        deliveryTaskDto1.setTransferType(TransferType.HAND_TO_HAND);

        deliveryTaskDto1.setCallStatus(CallToRecipientTaskStatus.SUCCESS);
        deliveryTaskDto1.setCallAttemptCount(1);

        return deliveryTaskDto1;
    }

    List<TaskDto> expectedReturnTasks() {
        var returnTaskDto = new OrderReturnTaskDto();
        List<OrderScanTaskDto.OrderForScanDto> orders = expectedOrdersForReturn();
        genericTaskDtoMapper.mapGenericTask(returnTaskDto, returnTask);
        returnTaskDto.setStatus(OrderReturnTaskStatus.NOT_STARTED);
        returnTaskDto.setOrders(orders);
        returnTaskDto.setCompletedOrders(List.of());
        returnTaskDto.setSkippedOrders(List.of());
        returnTaskDto.setComment(null);
        returnTaskDto.setDestinations(List.of(
                ScanTaskDestinationDto.builder()
                        .delivery(expectedDeliveryDto(order1))
                        .ordinalNumber(1)
                        .type(OrderType.CLIENT)
                        .batches(Set.of())
                        .outsideOrders(Set.of(
                                        DestinationOrderDto.builder()
                                                .externalOrderId(order1.getExternalOrderId())
                                                .orderFlowStatus(OrderFlowStatus.READY_FOR_RETURN)
                                                .places(List.of(PlaceDto.builder()
                                                        .barcode(order1.getExternalOrderId())
                                                        .build()))
                                                .build(),
                                        DestinationOrderDto.builder()
                                                .externalOrderId(order2.getExternalOrderId())
                                                .orderFlowStatus(OrderFlowStatus.READY_FOR_RETURN)
                                                .places(List.of(PlaceDto.builder()
                                                        .barcode(order2.getExternalOrderId())
                                                        .build()))
                                                .build()
                                )
                        )
                        .build(),
                ScanTaskDestinationDto.builder()
                        .delivery(expectedDeliveryDto(lockerReturnOrder))
                        .ordinalNumber(2)
                        .type(OrderType.LOCKER)
                        .batches(Set.of())
                        .outsideOrders(Set.of(
                                        DestinationOrderDto.builder()
                                                .externalOrderId(lockerReturnOrder.getExternalOrderId())
                                                .orderFlowStatus(OrderFlowStatus.READY_FOR_RETURN)
                                                .places(List.of(PlaceDto.builder()
                                                        .barcode(lockerReturnOrder.getExternalOrderId())
                                                        .build()))
                                                .build()
                                )
                        )
                        .build()
        ));
        returnTaskDto.setBatches(Set.of());
        returnTaskDto.setOutsideOrders(Set.of(
                DestinationOrderDto.builder()
                        .externalOrderId(order1.getExternalOrderId())
                        .orderFlowStatus(OrderFlowStatus.READY_FOR_RETURN)
                        .places(List.of(
                                PlaceDto.builder()
                                        .barcode(order1.getExternalOrderId())
                                        .build()
                        ))
                        .build(),
                DestinationOrderDto.builder()
                        .externalOrderId(order2.getExternalOrderId())
                        .orderFlowStatus(OrderFlowStatus.READY_FOR_RETURN)
                        .places(List.of(
                                PlaceDto.builder()
                                        .barcode(order2.getExternalOrderId())
                                        .build()
                        ))
                        .build(),
                DestinationOrderDto.builder()
                        .externalOrderId(lockerReturnOrder.getExternalOrderId())
                        .orderFlowStatus(OrderFlowStatus.READY_FOR_RETURN)
                        .places(List.of(
                                PlaceDto.builder()
                                        .barcode(lockerReturnOrder.getExternalOrderId())
                                        .build()
                        ))
                        .build()
        ));
        returnTaskDto.setTakePhoto(new CollectDropshipTakePhotoDto(PhotoRequirementType.REQUIRED));
        return List.of(returnTaskDto);
    }

    private List<OrderScanTaskDto.OrderForScanDto> expectedOrdersForReturn() {
        String multiOrderId = String.valueOf(callTask.getId());
        return List.of(new OrderScanTaskDto.OrderForScanDto(
                        true, multiOrderId, order1.getExternalOrderId(), null, null,
                        order1.getDelivery().getDeliveryAddress().getAddress(),
                        order1.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.CLIENT,
                        List.of(new PlaceForScanDto(order1.getExternalOrderId())),
                        null,
                        expectedDeliveryDto(order1),
                        null
                ),
                new OrderScanTaskDto.OrderForScanDto(
                        true, multiOrderId, order2.getExternalOrderId(), null, null,
                        order2.getDelivery().getDeliveryAddress().getAddress(),
                        order2.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.CLIENT,
                        List.of(new PlaceForScanDto(order2.getExternalOrderId())),
                        null,
                        expectedDeliveryDto(order2),
                        null
                ),
                new OrderScanTaskDto.OrderForScanDto(
                        false, "pickup" + lockerDeliveryTask.getId(), lockerReturnOrder.getExternalOrderId(), null,
                        null,
                        lockerReturnOrder.getDelivery().getDeliveryAddress().getAddress(),
                        lockerOrder.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.LOCKER,
                        List.of(new PlaceForScanDto(lockerReturnOrder.getExternalOrderId())),
                        null,
                        expectedDeliveryDto(lockerReturnOrder),
                        null
                )
        );
    }
}
