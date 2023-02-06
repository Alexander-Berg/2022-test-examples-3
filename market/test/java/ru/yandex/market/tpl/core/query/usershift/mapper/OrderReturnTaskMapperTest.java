package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.CollectDropshipTakePhotoDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.PhotoRequirementType;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.DeliveryTaskAddressToDeliveryAddressMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.GenericTaskDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.ReturnTaskDtoMapper;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.util.TplTaskUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.test.TestDataFactory.PICKUP_POINT_DEFAULT_PHONE;
import static ru.yandex.market.tpl.core.util.TplTaskUtils.PICKUP_PREFIX;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class OrderReturnTaskMapperTest {

    private final ReturnTaskDtoMapper returnTaskDtoMapper;
    private final GenericTaskDtoMapper genericTaskDtoMapper;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final ClientReturnCommandService clientReturnCommandService;
    private final Clock clock;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;
    private final ConfigurationServiceAdapter configuration;
    private final DeliveryTaskAddressToDeliveryAddressMapper addressMapper;

    private RoutePointAddress address;
    private OrderReturnTask returnTask;
    private String clientReturnBarcodeExternalCreated1;

    private String expectedMultiOrderId;
    private Order order;

    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        clientReturnCommandService.create(ClientReturnCommand.Create.builder()
                .barcode(clientReturnBarcodeExternalCreated1)
                .returnId(clientReturnBarcodeExternalCreated1)
                .pickupPoint(pickupPoint)
                .createdSource(CreatedSource.EXTERNAL)
                .source(Source.SYSTEM)
                .build());

        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        address = new RoutePointAddress("my_address", geoPoint);
        LockerDeliveryTask deliveryTask = (LockerDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .deliveryTaskAddress(addressMapper.map(order.getDelivery().getDeliveryAddress()))
                                .withOrderReferenceFromOrder(order, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
        expectedMultiOrderId = PICKUP_PREFIX + deliveryTask.getId();

        var userShift = deliveryTask.getRoutePoint().getUserShift();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        testUserHelper.arriveAtRoutePoint(deliveryTask.getRoutePoint());
        commandService.finishLoadingLocker(user, new UserShiftCommand.FinishLoadingLocker(userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(), null, ScanRequest.builder()
                            .successfullyScannedOrders(new ArrayList<>(deliveryTask.getOrderIds()))
                            .build()));

        commandService.finishUnloadingLocker(user, new UserShiftCommand.FinishUnloadingLocker(
                userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(),
                Set.of(new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, null))
        ));

        returnTask = userShift.streamReturnRoutePoints()
                .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void map(boolean newAddressMapperEnabled) {
        configuration.insertValue(ConfigurationProperties.NEW_LOCKER_ADDRESS_MAPPING_ENABLED, newAddressMapperEnabled);
        TaskDto taskDto = returnTaskDtoMapper.mapToTaskDto(returnTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);

        assertThat(taskDto).isEqualTo(expectedReturnTasks());
    }

    TaskDto expectedReturnTasks() {
        var returnTaskDto = new OrderReturnTaskDto();
        List<OrderScanTaskDto.OrderForScanDto> orders = List.of(
                new OrderScanTaskDto.OrderForScanDto(
                        true,
                        expectedMultiOrderId,
                        clientReturnBarcodeExternalCreated1,
                        null,
                        null,
                        address.getAddressString(),
                        null,
                        OrderType.LOCKER,
                        List.of(new PlaceForScanDto(clientReturnBarcodeExternalCreated1)),
                        null,
                        OrderDeliveryDto.builder()
                                .address(address.getAddressString())
                                .recipientPhone(PICKUP_POINT_DEFAULT_PHONE)
                                .realRecipientPhone(PICKUP_POINT_DEFAULT_PHONE)
                                .build(),
                        null
                )
        );
        genericTaskDtoMapper.mapGenericTask(returnTaskDto, returnTask);
        returnTaskDto.setStatus(OrderReturnTaskStatus.NOT_STARTED);
        returnTaskDto.setOrders(orders);
        returnTaskDto.setCompletedOrders(List.of());
        returnTaskDto.setSkippedOrders(List.of());
        returnTaskDto.setComment(null);
        returnTaskDto.setBatches(Set.of());
        returnTaskDto.setOutsideOrders(Set.of());
        returnTaskDto.setDestinations(List.of(
                ScanTaskDestinationDto.builder()
                        .type(OrderType.PVZ)
                        .batches(Set.of())
                        .outsideOrders(Set.of(
                                DestinationOrderDto.builder()
                                        .externalOrderId(clientReturnBarcodeExternalCreated1)
                                        .places(List.of(PlaceDto.builder()
                                                .barcode(clientReturnBarcodeExternalCreated1)
                                                .build()))
                                        .build()
                        ))
                        .delivery(OrderDeliveryDto.builder()
                                .address(address.getAddressString())
                                .recipientPhone(PICKUP_POINT_DEFAULT_PHONE)
                                .realRecipientPhone(PICKUP_POINT_DEFAULT_PHONE)
                                .build())
                        .ordinalNumber(1)
                        .build()
        ));
        returnTaskDto.setTakePhoto(new CollectDropshipTakePhotoDto(PhotoRequirementType.REQUIRED));
        return returnTaskDto;
    }

}
