package ru.yandex.market.tpl.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.ApiIntTest;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.PlaceToAccept;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ApiIntTest
public class ScanRequestMapperTest {

    private final OrderGenerateService orderGenerateService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final DropoffCargoCommandService cargoCommandService;

    private final ScanRequestMapper scanRequestMapper;
    private User user;
    private UserShift userShift;

    @BeforeEach
    public void init() {
        LocalDate date = LocalDate.now(clock);
        user = testUserHelper.findOrCreateUser(824125L, date);
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(TRANSPORTATION_RECIPIENT)
                .build());
        userShift = testUserHelper.createOpenedShift(user, order, date);
    }

    @Test
    void mapOrdersFromScanRequest() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        OrderScanTaskRequestDto dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(order1.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(order2.getExternalOrderId())))
                .comment("123")
                .build();

        ScanRequest scanRequest = scanRequestMapper.mapScanRequest(dto, user, 1, List.of());

        assertThat(scanRequest.getSuccessfullyScannedOrders()).containsExactly(order1.getId());
        assertThat(scanRequest.getSkippedOrders()).containsExactly(order2.getId());
    }

    @Test
    void mapOrdersFromScanRequest_withCargoPartly() {
        Order cargoMetaOrder = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        DropoffCargo cargo = buildCargo(cargoMetaOrder.getExternalOrderId());
        List<DropoffCargo> allDropoffCargoBarcodes = List.of(cargo);

        OrderScanTaskRequestDto dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(cargoMetaOrder.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(order2.getExternalOrderId())))
                .comment("123")
                .build();

        ScanRequest scanRequest = scanRequestMapper.mapScanRequest(dto, user, 1, allDropoffCargoBarcodes);

        assertThat(scanRequest.getSuccessfullyScannedOrders()).isEmpty();
        assertThat(scanRequest.getSkippedOrders()).containsExactly(order2.getId());
        assertThat(scanRequest.getSuccessfullyScannedDropoffCargos()).contains(cargo.getId());
        assertThat(scanRequest.getSkippedDropoffCargos()).isEmpty();
    }


    @Test
    void mapOrdersFromScanRequest_withCargoAll() {
        Order cargoMetaOrder = orderGenerateService.createOrder();
        Order cargoMetaOrder2 = orderGenerateService.createOrder();

        DropoffCargo cargo1 = buildCargo(cargoMetaOrder.getExternalOrderId());
        DropoffCargo cargo2 = buildCargo(cargoMetaOrder2.getExternalOrderId());
        List<DropoffCargo> allDropoffCargoBarcodes = List.of(cargo1, cargo2);

        OrderScanTaskRequestDto dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(cargoMetaOrder.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(cargoMetaOrder2.getExternalOrderId())))
                .comment("123")
                .build();

        ScanRequest scanRequest = scanRequestMapper.mapScanRequest(dto, user, 1, allDropoffCargoBarcodes);

        assertThat(scanRequest.getSuccessfullyScannedOrders()).isEmpty();
        assertThat(scanRequest.getSkippedOrders()).isEmpty();
        assertThat(scanRequest.getSuccessfullyScannedDropoffCargos()).contains(cargo1.getId());
        assertThat(scanRequest.getSkippedDropoffCargos()).contains(cargo2.getId());
    }

    @Test
    void mapOrdersFromScanRequestV2_withCargoAll() {
        Order cargoMetaOrder = orderGenerateService.createOrder();
        Order cargoMetaOrder2 = orderGenerateService.createOrder();

        DropoffCargo cargo1 = buildCargo(cargoMetaOrder.getExternalOrderId());
        DropoffCargo cargo2 = buildCargo(cargoMetaOrder2.getExternalOrderId());
        List<DropoffCargo> allDropoffCargo = List.of(cargo1, cargo2);
        Set<String> allDropoffCargoBarcodes = Set.of(cargoMetaOrder.getExternalOrderId(),
                cargoMetaOrder2.getExternalOrderId());

        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        RoutePointAddress myAddress = new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat());
        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(Instant.now())
                .expectedDeliveryTime(Instant.now())
                .name("my_name")
                .withOrderReferenceFromOrder(cargoMetaOrder, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();
        long userShiftId = commandService.createUserShift(createCommand);
        Long deliveryTaskId = userShiftRepository.findById(userShiftId)
                .stream()
                .flatMap(UserShift::streamPickupTasks)
                .findFirst()
                .map(OrderPickupTask::getId)
                .orElseThrow();

        OrderScanTaskRequestDto dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(cargoMetaOrder.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(cargoMetaOrder2.getExternalOrderId())))
                .comment("123")
                .scannedOutsidePlaces(Set.of(ScannedPlaceDto.builder()
                        .orderExternalId(cargoMetaOrder.getExternalOrderId())
                        .build()))
                .scannedBatches(Set.of())
                .build();

        ScanRequest scanRequest = scanRequestMapper.mapScanRequestV2(
                dto,
                Set.of(),
                Set.of(PlaceToAccept.builder()
                                .orderExternalId(cargoMetaOrder.getExternalOrderId())
                                .build(),
                        PlaceToAccept.builder()
                                .orderExternalId(cargoMetaOrder2.getExternalOrderId())
                                .build()
                ),
                allDropoffCargoBarcodes,
                user,
                deliveryTaskId,
                allDropoffCargo
        );

        assertThat(scanRequest.getSuccessfullyScannedOrders()).isEmpty();
        assertThat(scanRequest.getSkippedOrders()).isEmpty();
        assertThat(scanRequest.getSuccessfullyScannedDropoffCargos()).contains(cargo1.getId());
        assertThat(scanRequest.getSkippedDropoffCargos()).contains(cargo2.getId());
    }

    @Test
    void mapOrdersDirationFromScanRequest() {
        Order order1 = orderGenerateService.createOrder();
        Order order2 = orderGenerateService.createOrder();

        OrderScanTaskRequestDto dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(order1.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(order2.getExternalOrderId())))
                .duration(Duration.ofHours(2L))
                .comment("123")
                .build();

        ScanRequest scanRequest = scanRequestMapper.mapScanRequest(dto, user, 1, List.of());

        assertThat(scanRequest.getScanStartAt()).isEqualTo(Instant.now(clock).minus(1L, ChronoUnit.HOURS));

        dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(order1.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(order2.getExternalOrderId())))
                .duration(Duration.ofMinutes(30L))
                .comment("123")
                .build();

        scanRequest = scanRequestMapper.mapScanRequest(dto, user, 1, List.of());

        assertThat(scanRequest.getScanStartAt()).isEqualTo(Instant.now(clock).minus(30L, ChronoUnit.MINUTES));
    }

    private OrderScanTaskDto.OrderForScanDto buildScanDto(String externalOrderId) {
        return OrderScanTaskDto.OrderForScanDto.builder()
                .displayMode(OrderScanTaskDto.ScanOrderDisplayMode.OK)
                .externalOrderId(externalOrderId)
                .build();
    }

    @Test
    void mapClientReturnsFromScanRequest() {
        ClientReturn clientReturn1 = clientReturnGenerator.generate();
        ClientReturn clientReturn2 = clientReturnGenerator.generate();

        OrderScanTaskRequestDto dto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(clientReturn1.getBarcode())))
                .skippedOrders(List.of(buildScanDto(clientReturn2.getBarcode())))
                .comment("123")
                .build();

        ScanRequest scanRequest = scanRequestMapper.mapScanRequest(dto, user, 1, List.of());

        assertThat(scanRequest.getSuccessfullyScannedClientReturns()).containsExactly(clientReturn1.getId());
        assertThat(scanRequest.getSkippedClientReturns()).containsExactly(clientReturn2.getId());
    }

    @Test
    void testScanStartedAtIsEqualToTaskStartedAt() {
        var order1 = orderGenerateService.createOrder();
        var order2 = orderGenerateService.createOrder();

        var requestDto = OrderScanTaskRequestDto.builder()
                .completedOrders(List.of(buildScanDto(order1.getExternalOrderId())))
                .skippedOrders(List.of(buildScanDto(order2.getExternalOrderId())))
                .duration(Duration.parse("PT10H10M59S"))
                .comment("123")
                .build();

        commandService.arriveAtRoutePoint(userShift.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        userShift.streamPickupRoutePoints().findFirst().orElseThrow().getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        var routePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamTasks().findFirst().orElseThrow();
        commandService.startOrderPickup(userShift.getUser(),
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        routePoint.getId(),
                        task.getId()
                ));
        var scanRequest = scanRequestMapper.mapScanRequest(requestDto, user, task.getId(), List.of());

        assertThat(scanRequest.getScanStartAt()).isEqualTo((task.getStartedAt()));
    }

    private DropoffCargo buildCargo(String barcode) {
        return cargoCommandService.createOrGet(DropoffCargoCommand.Create.builder()
                .barcode(barcode)
                .logisticPointIdFrom("fromId")
                .logisticPointIdTo("toId")
                .build());
    }

}
