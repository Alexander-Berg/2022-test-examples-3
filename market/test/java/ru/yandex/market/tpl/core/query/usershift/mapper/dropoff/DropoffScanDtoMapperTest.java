package ru.yandex.market.tpl.core.query.usershift.mapper.dropoff;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryAddressDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.query.usershift.mapper.TplViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropoffScanDtoMapperTest {

    public static final long EXISTED_MOVEMENT_ID = 1L;
    public static final String EXISTED_LOGISITNG_POINT_ID = "existedLogisitngPoint";
    public static final String EXPECTED_WAREHOUSE_CITY = "WareHouseCity";
    public static final String EXPECTED_WAREHOUSE_STREET = "WareHouseStreet";

    @Mock
    private MovementRepository movementRepository;
    @Mock
    private OrderWarehouseRepository orderWarehouseRepository;
    @InjectMocks
    private DropoffScanDtoMapper mapper;

    @Test
    void mapToOrders() {
        //given
        String expectedBarcode = "barcode1";
        int expectedOrdinalNumber = 12345;

        OrderWarehouse mockedOrderWarehouse = buildMockedOrderWarehouse();
        when(orderWarehouseRepository.findFirstByYandexIdOrderByIdAscCacheable(EXISTED_LOGISITNG_POINT_ID))
                .thenReturn(Optional.of(mockedOrderWarehouse));

        //when
        List<OrderScanTaskDto.OrderForScanDto> orderForScanDtos = mapper.mapToOrders(Map.of(EXISTED_MOVEMENT_ID,
                List.of(buildDropoffCargo(expectedBarcode))), TplScanDtoBatchMapperContext
                .builder()
                .ordinalNumber(TplViewResolver.OrdinalNumber.builder()
                        .forMovements(Map.of(EXISTED_MOVEMENT_ID, expectedOrdinalNumber))
                        .build())
                .build());

        //then
        assertThat(orderForScanDtos).hasSize(1);
        orderForScanDtos.forEach(order -> {
            assertEquals(expectedOrdinalNumber, order.getOrdinalNumber());
            assertsMetaOrderForCargo(order);
            assertEquals(expectedBarcode, order.getExternalOrderId());
            assertThat(order.getPlaces()).hasSize(1);
            assertEquals(expectedBarcode, order.getPlaces().get(0).getBarcode());
        });
    }

    @Test
    void mapToDestinationDtos() {
        //given
        String expectedBarcode = "barcode1";
        int expectedOrdinalNumber = 12345;

        Movement movement = buildMockedMovement();
        when(movementRepository.findById(EXISTED_MOVEMENT_ID)).thenReturn(Optional.of(movement));

        //when
        List<ScanTaskDestinationDto> scanTaskDestinationDtos = mapper.mapToDestinationDtos(Map.of(EXISTED_MOVEMENT_ID,
                List.of(buildDropoffCargo(expectedBarcode))).entrySet(), TplScanDtoBatchMapperContext
                .builder()
                .ordinalNumber(TplViewResolver.OrdinalNumber.builder()
                        .forMovements(Map.of(EXISTED_MOVEMENT_ID, expectedOrdinalNumber))
                        .build())
                .build());

        //then
        assertThat(scanTaskDestinationDtos).hasSize(1);
        scanTaskDestinationDtos.forEach(destination -> {
            assertEquals(expectedOrdinalNumber, destination.getOrdinalNumber());

            assertEquals(OrderType.PVZ, destination.getType());
            assertNotNull(destination.getDelivery());

            OrderDeliveryAddressDto addressDetails = destination.getDelivery().getAddressDetails();
            assertNotNull(addressDetails);
            assertEquals(EXPECTED_WAREHOUSE_CITY, addressDetails.getCity());
            assertEquals(EXPECTED_WAREHOUSE_STREET, addressDetails.getStreet());

            assertThat(destination.getOutsideOrders()).hasSize(1);

            DestinationOrderDto outsideOrder = destination.getOutsideOrders().iterator().next();
            assertEquals(expectedBarcode, outsideOrder.getExternalOrderId());

            assertThat(outsideOrder.getPlaces()).hasSize(1);
            assertEquals(expectedBarcode, outsideOrder.getPlaces().get(0).getBarcode());
        });
    }

    @Test
    void mapToOrderScanDtoForDropoff() {
        //given
        String expectedBarcode = "barcode1";

        OrderWarehouse mockedOrderWarehouse = buildMockedOrderWarehouse();
        when(orderWarehouseRepository.findFirstByYandexIdOrderByIdAscCacheable(EXISTED_LOGISITNG_POINT_ID))
                .thenReturn(Optional.of(mockedOrderWarehouse));

        int expectedOrdinalNumber = 1234;

        //when
        OrderScanTaskDto.OrderForScanDto orderForScanDto =
                mapper.mapToOrderScanDtoForDropoff(buildDropoffCargo(expectedBarcode),
                        TplScanDtoSingleMapperContext.of(expectedOrdinalNumber, true));

        //then
        assertsMetaOrderForCargo(orderForScanDto);
        assertEquals(expectedBarcode, orderForScanDto.getExternalOrderId());
        assertThat(orderForScanDto.getPlaces()).hasSize(1);
        assertEquals(expectedBarcode, orderForScanDto.getPlaces().get(0).getBarcode());
        assertEquals(expectedOrdinalNumber, orderForScanDto.getOrdinalNumber());
    }

    private Movement buildMockedMovement() {
        OrderWarehouse orderWarehouse = buildMockedOrderWarehouse();
        Movement mockedMovement = mock(Movement.class);
        when(mockedMovement.getWarehouseTo()).thenReturn(orderWarehouse);
        return mockedMovement;
    }

    private OrderWarehouse buildMockedOrderWarehouse() {
        OrderWarehouseAddress mockedAddress = mock(OrderWarehouseAddress.class);
        when(mockedAddress.getCity()).thenReturn(EXPECTED_WAREHOUSE_CITY);
        when(mockedAddress.getStreet()).thenReturn(EXPECTED_WAREHOUSE_STREET);
        OrderWarehouse orderWarehouse = mock(OrderWarehouse.class);
        when(orderWarehouse.getAddress()).thenReturn(mockedAddress);
        when(orderWarehouse.getPhones()).thenReturn(List.of("+79999999999"));
        return orderWarehouse;
    }

    private void assertsMetaOrderForCargo(OrderScanTaskDto.OrderForScanDto order) {
        assertEquals(OrderType.LOCKER, order.getType());
        assertEquals(OrderScanTaskDto.ScanOrderDisplayMode.OK, order.getDisplayMode());
        assertEquals(OrderFlowStatus.SORTING_CENTER_PREPARED, order.getOrderFlowStatus());
    }

    private DropoffCargo buildDropoffCargo(String barcode) {
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode(barcode);
        dropoffCargo.setLogisticPointIdTo(EXISTED_LOGISITNG_POINT_ID);
        return dropoffCargo;
    }
}
