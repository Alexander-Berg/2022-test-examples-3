package ru.yandex.market.tpl.core.service.dropoff;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.query.usershift.mapper.TplViewResolver;
import ru.yandex.market.tpl.core.query.usershift.mapper.dropoff.DropoffScanDtoMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DROPOFF_RETURN_SUPPORT_ENABLED;

@ExtendWith(MockitoExtension.class)
class MovementCargoPickupSupportServiceUnitTest {
    public static final long EXISTED_MOVEMENT_ID = 1L;
    public static final String EXISTED_LOGISITNG_POINT_ID = "existedLogisitngPoint";
    public static final String EXPECTED_WAREHOUSE_CITY = "WareHouseCity";
    public static final String EXPECTED_WAREHOUSE_STREET = "WareHouseStreet";
    public static final int ORDINAL_NUMBER_FROM_TASK_DTO = 100;
    public static final int ORDINAL_NUMBER_FROM_MAPPER_1 = 200;
    public static final int ORDINAL_NUMBER_FROM_MAPPER_2 = 300;

    @Mock
    private OrderWarehouseRepository orderWarehouseRepository;
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private MovementCargoCollector movementCargoCollector;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private DropoffScanDtoMapper dropoffScanDtoMapper;
    @Mock
    private TplViewResolver tplViewResolver;

    @InjectMocks
    private MovementCargoSupportService movementCargoSupportService;

    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(DROPOFF_RETURN_SUPPORT_ENABLED)).thenReturn(true);
    }

    @Test
    public void enrichWithCargoMetaOrders_CargoNotEmpty() {
        //given
        String expectedBarcode = "barcode1";
        int expectedOrdinalNumber = 12345;

        Map<Long, List<DropoffCargo>> cargoMovementMap = Map.of(EXISTED_MOVEMENT_ID,
                List.of(buildDropoffCargo(expectedBarcode)));

        TplViewResolver.OrdinalNumber ordinalNumber = TplViewResolver.OrdinalNumber.builder()
                .forMovements(Map.of(EXISTED_MOVEMENT_ID, expectedOrdinalNumber))
                .build();

        when(tplViewResolver.resolveOrdinalNumbers(any()))
                .thenReturn(ordinalNumber);

        when(movementCargoCollector.collectPickupCargosMap(any())).thenReturn(cargoMovementMap);
        when(dropoffScanDtoMapper.mapToOrders(eq(cargoMovementMap), any())).thenReturn(
                List.of(buildOrderScanTaskDto("externalId1"),
                        buildOrderScanTaskDto("externalId2")));

        when(dropoffScanDtoMapper.mapToDestinationDtos(eq(cargoMovementMap.entrySet()), any())).thenReturn(
                List.of(buildScanTaskDestinationDto(ORDINAL_NUMBER_FROM_MAPPER_1),
                        buildScanTaskDestinationDto(ORDINAL_NUMBER_FROM_MAPPER_2)));

        OrderPickupTaskDto dto = buildTaskDto(
                List.of(buildOrderScanTaskDto("externalId3")),
                List.of(buildScanTaskDestinationDto(ORDINAL_NUMBER_FROM_TASK_DTO)));

        //when
        movementCargoSupportService.enrichWithCargoMetaOrders(buildMockedTaskEntity(), dto);

        //then
        //TODO check all fields...
        //asserts for Orders
        assertThat(dto.getOrders()).hasSize(3);
        Set<String> enrichedExternalOrderIds =
                dto.getOrders().stream().map(OrderScanTaskDto.OrderForScanDto::getExternalOrderId)
                        .collect(Collectors.toSet());
        assertThat(enrichedExternalOrderIds).containsExactlyInAnyOrder("externalId1", "externalId2", "externalId3");

        //asserts for Destinations
        assertThat(dto.getDestinations()).hasSize(3);

        Set<Integer> ordinalNumbers = dto.getDestinations().stream().map(ScanTaskDestinationDto::getOrdinalNumber)
                .collect(Collectors.toSet());
        assertThat(ordinalNumbers).containsExactlyInAnyOrder(ORDINAL_NUMBER_FROM_TASK_DTO,
                ORDINAL_NUMBER_FROM_MAPPER_1, ORDINAL_NUMBER_FROM_MAPPER_2);
    }


    private OrderScanTaskDto.OrderForScanDto buildOrderScanTaskDto(String externalOrderId) {
        return new OrderScanTaskDto.OrderForScanDto(false, null, externalOrderId,
                OrderScanTaskDto.ScanOrderDisplayMode.OK, "",
                null, null, OrderType.CLIENT, null,
                OrderFlowStatus.SORTING_CENTER_PREPARED, null, 1);
    }

    private ScanTaskDestinationDto buildScanTaskDestinationDto(Integer ordinalNumber) {
        return new ScanTaskDestinationDto(null, ordinalNumber, null, null, null);
    }

    private OrderPickupTaskDto buildTaskDto(List<OrderScanTaskDto.OrderForScanDto> orders,
                                            List<ScanTaskDestinationDto> destinations) {
        OrderPickupTaskDto orderPickupTaskDto = new OrderPickupTaskDto();
        orderPickupTaskDto.setOrders(orders);
        orderPickupTaskDto.setDestinations(destinations);
        return orderPickupTaskDto;
    }

    private DropoffCargo buildDropoffCargo(String barcode) {
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode(barcode);
        dropoffCargo.setLogisticPointIdTo(EXISTED_LOGISITNG_POINT_ID);
        return dropoffCargo;
    }

    private OrderPickupTask buildMockedTaskEntity() {
        OrderPickupTask mockedTask = mock(OrderPickupTask.class);
        RoutePoint mockedRP = mock(RoutePoint.class);
        when(mockedTask.getRoutePoint()).thenReturn(mockedRP);
        return mockedTask;
    }
}
