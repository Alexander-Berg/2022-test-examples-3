package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.order.locker.LockerDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatch;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.DeliverySubtask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDeliveryMapper;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.DeliveryTaskAddressToDeliveryAddressMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.GenericTaskDtoMapper;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class LockerDeliveryTaskMapperTest {

    private static final LockerDto EXPECTED_LOCKER = new LockerDto(
            "https://avatars.mds.yandex.net/get-market-shop-logo/1528691/package_1/orig",
            null,
            "Сначала налева, потом направо",
            "+79999999999",
            PickupPointType.LOCKER,
            PartnerSubType.LOCKER,
            "4"
    );

    private final TaskDtoMapper taskDtoMapper;
    private final GenericTaskDtoMapper genericTaskDtoMapper;
    private final OrderDtoMapper orderDtoMapper;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final OrderDeliveryMapper orderDeliveryMapper;
    private final DeliveryTaskAddressToDeliveryAddressMapper deliveryAddressMapper;
    private final AddressGenerator addressGenerator;
    private final Clock clock;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private LockerDeliveryTask deliveryTask;
    private Order order;
    private Order order2;

    @BeforeEach
    void init() {
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        pickupPoint.setCode("4");
        DeliveryAddress deliveryAddress = addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(geoPoint)
                .build());
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .pickupPoint(pickupPoint)
                .build());
        order.getDelivery().setDeliveryAddress(deliveryAddress);
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .pickupPoint(pickupPoint)
                .places(
                        List.of(
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("145", "place1"))
                                        .build(),
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("145", "place2"))
                                        .build()
                        ))
                .withBatch(true)
                .build());
        order2.getDelivery().setDeliveryAddress(deliveryAddress);

        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        RoutePointAddress address = new RoutePointAddress("my_address", geoPoint);
        deliveryTask = (LockerDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .deliveryTaskAddress(deliveryAddressMapper.map(order.getDelivery().getDeliveryAddress()))
                                .withOrderReferenceFromOrder(order, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .deliveryTaskAddress(deliveryAddressMapper.map(order2.getDelivery().getDeliveryAddress()))
                                .withOrderReferenceFromOrder(order2, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );


        var userShift = deliveryTask.getRoutePoint().getUserShift();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

        DeliverySubtask deliverySubtask = deliveryTask.streamDeliveryOrderSubtasks()
                .findFirst(st -> Objects.equals(st.getOrderId(), order.getId()))
                .orElseThrow(IllegalStateException::new);
        commandService.failDeliverySubtask(user, new UserShiftCommand.FailOrderDeliverySubtask(
                userShiftId, deliveryTask.getRoutePoint().getId(), deliverySubtask,
                new OrderDeliveryFailReason(
                        OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, null
                )
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void map(boolean newMappingEnabled) {
        configurationServiceAdapter.insertValue(ConfigurationProperties.NEW_LOCKER_ADDRESS_MAPPING_ENABLED, newMappingEnabled);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.GET_PICKUP_POINT_INFO_BY_ID_FROM_LOCKER_DELIVERY_TASK, newMappingEnabled);

        List<TaskDto> taskDtos = taskDtoMapper.mapTasks(deliveryTask.getRoutePoint(), List.of(order, order2), null);
        TaskDto expectedDeliveryTask = expectedDeliveryTask();
        assertThat(taskDtos.get(0)).isEqualTo(expectedDeliveryTask);
        LockerDeliveryTaskDto task = (LockerDeliveryTaskDto) taskDtos.get(0);
        OrderDto order2Dto = StreamEx.of(task.getOrders())
                .filter(o -> o.getExternalOrderId().equals(order2.getExternalOrderId()))
                .findFirst().orElseThrow();
        Optional<PlaceDto> placeInsideBatch = StreamEx.of(order2Dto.getPlaces())
                .findFirst(p -> p.getBatchBarcode() != null);
        assertThat(placeInsideBatch).isNotEmpty();
    }

    private TaskDto expectedDeliveryTask() {
        var deliveryTaskDto = new LockerDeliveryTaskDto();
        genericTaskDtoMapper.mapGenericTask(deliveryTaskDto, deliveryTask);
        deliveryTaskDto.setStatus(LockerDeliveryTaskStatus.NOT_STARTED);
        deliveryTaskDto.setOrders(List.of(
                orderDtoMapper.mapOrderDto(
                        order2, deliveryTask.getExpectedDeliveryTime(), deliveryTask.getOrdinalNumber())
        ));
        deliveryTaskDto.setCompletedOrders(List.of());
        deliveryTaskDto.setLocker(EXPECTED_LOCKER);
        deliveryTaskDto.setFinishedAt(deliveryTask.getExpectedDeliveryTime());
        deliveryTaskDto.setClientReturns(List.of());
        deliveryTaskDto.setCompletedClientReturns(List.of());

        OrderPlace placeInsideBatch = StreamEx.of(order2.getPlaces())
                .findFirst(p -> p.getCurrentBatch().isPresent())
                .orElseThrow();
        OrderPlace placeOutsideBatch = StreamEx.of(order2.getPlaces())
                .findFirst(p -> p.getCurrentBatch().isEmpty())
                .orElseThrow();

        OrderBatch batch = placeInsideBatch.getCurrentBatch().orElseThrow();
        deliveryTaskDto.setBatches(Set.of(
                OrderBatchDto.builder()
                        .barcode(batch.getBarcode())
                        .orders(Set.of(
                                OrderDto.builder()
                                        .externalOrderId(placeInsideBatch.getOrder().getExternalOrderId())
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode(placeInsideBatch.getBarcode().getBarcode())
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        ));
        deliveryTaskDto.setOutsideOrders(
                Set.of(
                        DestinationOrderDto.builder()
                                .externalOrderId(placeInsideBatch.getOrder().getExternalOrderId())
                                .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                .places(List.of(
                                        PlaceDto.builder()
                                                .barcode(placeOutsideBatch.getBarcode().getBarcode())
                                                .build()
                                ))
                                .build()
                )
        );
        OrderDeliveryDto delivery = orderDeliveryMapper.mapDelivery(order2.getDelivery(), null, null);
        deliveryTaskDto.setDestinations(
                Set.of(
                        ScanTaskDestinationDto.builder()
                                .type(OrderType.LOCKER)
                                .delivery(delivery)
                                .batches(Set.of(
                                        OrderBatchDto.builder()
                                                .barcode(batch.getBarcode())
                                                .orders(Set.of(
                                                        OrderDto.builder()
                                                                .externalOrderId(placeInsideBatch.getOrder().getExternalOrderId())
                                                                .places(List.of(
                                                                        PlaceDto.builder()
                                                                                .barcode(placeInsideBatch.getBarcode().getBarcode())
                                                                                .build()
                                                                ))
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .outsideOrders(Set.of(
                                        DestinationOrderDto.builder()
                                                .orderFlowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                                                .externalOrderId(placeInsideBatch.getOrder().getExternalOrderId())
                                                .places(List.of(
                                                        PlaceDto.builder()
                                                                .barcode(placeOutsideBatch.getBarcode().getBarcode())
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                )
        );
        return deliveryTaskDto;
    }

}
