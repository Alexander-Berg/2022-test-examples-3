package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseSchedule;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.OrderPickupTaskDtoMapper;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplTaskUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.NEW_LOCKER_ADDRESS_MAPPING_ENABLED;

@RequiredArgsConstructor
class OrderPickupTaskDtoMapperTest extends TplAbstractTest {
    public static final String LOGISTICPOINT_ID_FOR_RETURN_DROPOFF = "1234567";
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final MovementGenerator movementGenerator;
    private final AddressGenerator addressGenerator;
    private final Clock clock;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final TransactionTemplate transactionTemplate;
    private final TaskDtoMapper taskDtoMapper;
    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final VehicleGenerateService vehicleGenerateService;
    private final OrderPickupTaskDtoMapper orderPickupTaskDtoMapper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TestDataFactory testDataFactory;

    private UserShift userShift;
    private User user;
    private OrderPickupTask pickupTask;
    private Movement movementDropoffReturn;
    private PickupPoint pickupPoint;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        movementDropoffReturn = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouseTo(orderWarehouseRepository.saveAndFlush(new OrderWarehouse(
                        LOGISTICPOINT_ID_FOR_RETURN_DROPOFF,
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
                .build());

        int hour = 12;
        String address = "addr1";
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(NewDeliveryRoutePointData.builder()
                        .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                        .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                        .name("Доставка " + address)
                        .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                        .cargoReference(CargoReference.builder()
                                .movementId(movementDropoffReturn.getId())
                                .build())
                        .updateSc(true)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .build()
                )
                .build();

        transactionTemplate.execute(status -> {
            long userShiftId = commandService.createUserShift(createCommand);
            userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            pickupTask = userShift.streamPickupRoutePoints()
                    .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();

            return null;
        });

        pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);
    }

    @Test
    void appendingMetaOrders_whenDropoffReturns() {
        //given
        List<String> barcodes = List.of("barcode1", "barcode2");
        barcodes.forEach(this::addDropoffCargo);

        //when
        List<TaskDto> mappedTasks = transactionTemplate.execute(st ->
                taskDtoMapper.mapTasks(pickupTask.getRoutePoint(), Collections.emptyList(), null)
        );

        //then
        assertNotNull(mappedTasks);

        Set<OrderPickupTaskDto> mappedPickupPointTasks = mappedTasks
                .stream()
                .filter(OrderPickupTaskDto.class::isInstance)
                .map(OrderPickupTaskDto.class::cast)
                .collect(Collectors.toSet());

        Set<OrderScanTaskDto.OrderForScanDto> dtoOrders = mappedPickupPointTasks
                .stream()
                .map(OrderPickupTaskDto::getOrders)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        //check external ids
        Set<OrderScanTaskDto.OrderForScanDto> metaOrders = dtoOrders
                .stream()
                .filter(order -> barcodes.contains(order.getExternalOrderId()))
                .collect(Collectors.toSet());

        var dtoVehicles = mappedPickupPointTasks
                .stream()
                .map(OrderPickupTaskDto::getVehicleInstances)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(dtoVehicles.size()).isEqualTo(0);

        assertThat(metaOrders).hasSize(barcodes.size());

        //check places
        assertThat(metaOrders
                .stream()
                .map(OrderScanTaskDto.OrderForScanDto::getPlaces)
                .flatMap(Collection::stream)
                .map(PlaceForScanDto::getBarcode)
                .collect(Collectors.toSet())).containsAll(barcodes);

        //check destionation
        assertTrue(mappedPickupPointTasks
                .stream()
                .map(OrderScanTaskDto::getDestinations)
                .flatMap(Collection::stream)
                .anyMatch(destinationDto -> barcodes.containsAll(destinationDto
                        .getOutsideOrders()
                        .stream()
                        .map(DestinationOrderDto::getPlaces)
                        .flatMap(Collection::stream)
                        .map(PlaceDto::getBarcode)
                        .collect(Collectors.toSet()))
                ));
    }

    @ParameterizedTest
    @EnumSource(VehicleInstanceType.class)
    void testMapVehicleInstances(VehicleInstanceType vehicleInstanceType) {
        //given
        var vehicle = vehicleGenerateService.generateVehicle();
        var vehicleColor = vehicleGenerateService.generateVehicleColor("White");
        var vehicleInstance =
                vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .type(vehicleInstanceType)
                        .infoUpdatedAt(Instant.now())
                        .users(List.of(user))
                        .color(vehicleColor)
                        .vehicle(vehicle)
                        .build());

        //when
        List<TaskDto> mappedTasks = transactionTemplate.execute(st -> taskDtoMapper.mapTasks(pickupTask.getRoutePoint(),
                Collections.emptyList(), null));

        //then
        assertNotNull(mappedTasks);

        Set<OrderPickupTaskDto> mappedPickupPointTasks = mappedTasks
                .stream()
                .filter(OrderPickupTaskDto.class::isInstance)
                .map(OrderPickupTaskDto.class::cast)
                .collect(Collectors.toSet());

        var dtoVehicles = mappedPickupPointTasks
                .stream()
                .map(OrderPickupTaskDto::getVehicleInstances)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(dtoVehicles.size()).isEqualTo(1);

        var dtoVehicle = dtoVehicles.get(0);

        assertThat(dtoVehicle.getId()).isEqualTo(vehicleInstance.getId());
        assertThat(dtoVehicle.getBrand()).isEqualTo(vehicleInstance.getVehicle().getVehicleBrand().getName());
        assertThat(dtoVehicle.getName()).isEqualTo(vehicleInstance.getVehicle().getName());
        assertThat(dtoVehicle.getType()).isNotNull();
        assertThat(dtoVehicle.getColor()).isEqualTo(vehicleInstance.getColor().getName());
        assertThat(dtoVehicle.getRegistrationNumber()).isEqualTo(vehicleInstance.getRegistrationNumber() + vehicleInstance.getRegistrationNumberRegion());

    }

    @SneakyThrows
    @Test
    void orderDeliveryTaskDtoNotNullOrderTest() {
        User user = testUserHelper.findOrCreateUser(1L);
        Long orderId = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 18769);
        var userShiftId = userShiftTestHelper.start(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, orderId))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build()
        );
        OrderPickupTask pickupTask = transactionTemplate.execute(t -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            return userShiftTestHelper.getOrderPickupTask(userShift);
        });

        OrderPickupTaskDto taskDto = (OrderPickupTaskDto) transactionTemplate.execute(t ->
                orderPickupTaskDtoMapper.mapToTaskDto(pickupTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT)
        );

        assertThat(taskDto).isNotNull();
        Method getOrdersMethod = OrderPickupTaskDto.class.getSuperclass().getDeclaredMethod("getOrders");
        getOrdersMethod.trySetAccessible();
        List<Order> orders = (List<Order>) getOrdersMethod.invoke(taskDto);
        assertThat(orders).isNotNull();
    }

    @Test
    void lockerDeliveryTaskDto() {
        when(configurationProviderAdapter.isBooleanEnabled(NEW_LOCKER_ADDRESS_MAPPING_ENABLED)).thenReturn(true);
        User user = testUserHelper.findOrCreateUser(1L);
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .pickupPoint(pickupPoint)
                .build()
        );
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 18769);
        var userShiftId = userShiftTestHelper.start(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskLockerDelivery(order, pickupPoint.getId(), 10))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build()
        );

        OrderPickupTaskDto taskDto = (OrderPickupTaskDto) transactionTemplate.execute(t -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
            OrderPickupTask pickupTask = userShiftTestHelper.getOrderPickupTask(userShift);
            return orderPickupTaskDtoMapper.mapToTaskDto(pickupTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);
        });

        assertThat(taskDto).isNotNull();
        assertThat(taskDto.getOrders()).asList().hasSize(1);
        var orderDto = taskDto.getOrders().get(0);
        assertThat(orderDto.getType()).isEqualTo(OrderType.LOCKER);
        assertThat(orderDto.getDelivery().getAddress())
                .isEqualTo(order.getDelivery().getDeliveryAddress().getAddress());
        assertThat(taskDto.getDestinations()).asList().hasSize(1);
        var destinationsDto = taskDto.getDestinations().get(0);
        assertThat(destinationsDto.getDelivery().getAddress())
                .isEqualTo(order.getDelivery().getDeliveryAddress().getAddress());
    }

    private void addDropoffCargo(String barcode) {
        dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom("fakeIdFrom")
                        .logisticPointIdTo(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF)
                        .build());
    }
}
