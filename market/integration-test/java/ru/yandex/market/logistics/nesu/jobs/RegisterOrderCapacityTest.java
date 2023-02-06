package ru.yandex.market.logistics.nesu.jobs;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.model.dto.CapacityServiceType;
import ru.yandex.market.delivery.mdbclient.model.dto.OrderToShipDto;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.nesu.jobs.processor.RegisterOrderCapacityProcessor;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Отправка данных заказов для учета капасити")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class RegisterOrderCapacityTest extends AbstractContextualTest {

    private static final long PLATFORM_CLIENT_ID = 3;
    private static final long ORDER_ID = 1;
    private static final String BARCODE = "barcode-" + ORDER_ID;
    private static final long SENDER_ID = 2;
    private static final long DELIVERY_SERVICE_ID = 100;
    private static final long SORTING_CENTER_ID = 133;
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2020, 3, 4);
    private static final long WAREHOUSE_FROM_ID = 1000;
    private static final int LOCATION_FROM = 213;
    private static final int LOCATION_TO = 2;

    @Autowired
    private RegisterOrderCapacityProcessor processor;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MdbClient mdbClient;

    @BeforeEach
    void setupMocks() {
        when(lmsClient.getLogisticsPoint(WAREHOUSE_FROM_ID))
            .thenReturn(Optional.of(
                LogisticsPointResponse.newBuilder()
                    .type(PointType.WAREHOUSE)
                    .active(true)
                    .address(Address.newBuilder().locationId(LOCATION_FROM).build())
                    .build()
            ));
    }

    @AfterEach
    void verifyClient() {
        verifyNoMoreInteractions(mdbClient);
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() {
        process();
    }

    @Test
    @DisplayName("Заказ без локации получателя")
    void noLocationTo() {
        when(lomClient.getOrder(ORDER_ID, Set.of())).thenReturn(Optional.of(
            new OrderDto().setId(ORDER_ID).setSenderId(SENDER_ID)
        ));

        process();
    }

    @Test
    @DisplayName("Заказ без даты отгрузки")
    void noShipmentDate() {
        mockOrder(List.of(
            waybillSegment(DELIVERY_SERVICE_ID, ShipmentType.IMPORT, PartnerType.DELIVERY, null)
        ));

        process();
    }

    @Test
    @DisplayName("Заказ с одним сегментом")
    void oneSegment() {
        mockOrder(List.of(
            waybillSegment(DELIVERY_SERVICE_ID, ShipmentType.IMPORT, PartnerType.DELIVERY, SHIPMENT_DATE)
        ));

        process();

        ArgumentCaptor<OrderToShipDto> captor = ArgumentCaptor.forClass(OrderToShipDto.class);
        verify(mdbClient).createOrderToShip(captor.capture());
        softly.assertThat(captor.getValue())
            .isEqualTo(orderToShip(DELIVERY_SERVICE_ID, SHIPMENT_DATE, CapacityServiceType.DELIVERY));
    }

    @Test
    @DisplayName("Заказ с двумя сегментами")
    void twoSegments() {
        LocalDate secondShipmentDate = SHIPMENT_DATE.plusDays(1);
        mockOrder(List.of(
            waybillSegment(SORTING_CENTER_ID, ShipmentType.IMPORT, PartnerType.SORTING_CENTER, SHIPMENT_DATE),
            waybillSegment(DELIVERY_SERVICE_ID, ShipmentType.WITHDRAW, PartnerType.DELIVERY, secondShipmentDate)
        ));

        process();

        ArgumentCaptor<OrderToShipDto> captor = ArgumentCaptor.forClass(OrderToShipDto.class);
        verify(mdbClient, times(2)).createOrderToShip(captor.capture());
        softly.assertThat(captor.getAllValues()).containsExactly(
            orderToShip(SORTING_CENTER_ID, SHIPMENT_DATE, CapacityServiceType.SHIPMENT),
            orderToShip(DELIVERY_SERVICE_ID, secondShipmentDate, CapacityServiceType.DELIVERY)
        );
    }

    @Test
    @DisplayName("Повторная отправка заказа")
    void repeatOrder() {
        mockOrder(List.of(
            waybillSegment(DELIVERY_SERVICE_ID, ShipmentType.IMPORT, PartnerType.DELIVERY, SHIPMENT_DATE)
        ));
        when(mdbClient.createOrderToShip(any())).thenThrow(new HttpTemplateException(409, "Error"));

        softly.assertThatCode(this::process)
            .doesNotThrowAnyException();

        ArgumentCaptor<OrderToShipDto> captor = ArgumentCaptor.forClass(OrderToShipDto.class);
        verify(mdbClient).createOrderToShip(captor.capture());
        softly.assertThat(captor.getValue())
            .isEqualTo(orderToShip(DELIVERY_SERVICE_ID, SHIPMENT_DATE, CapacityServiceType.DELIVERY));
    }

    @Test
    @DisplayName("Ошибка при отправке заказа")
    void orderError() {
        mockOrder(List.of(
            waybillSegment(DELIVERY_SERVICE_ID, ShipmentType.IMPORT, PartnerType.DELIVERY, SHIPMENT_DATE)
        ));
        when(mdbClient.createOrderToShip(any())).thenThrow(new HttpTemplateException(500, "Error"));

        softly.assertThatThrownBy(this::process)
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <500>, response body <Error>.");

        ArgumentCaptor<OrderToShipDto> captor = ArgumentCaptor.forClass(OrderToShipDto.class);
        verify(mdbClient).createOrderToShip(captor.capture());
        softly.assertThat(captor.getValue())
            .isEqualTo(orderToShip(DELIVERY_SERVICE_ID, SHIPMENT_DATE, CapacityServiceType.DELIVERY));
    }

    private void mockOrder(List<WaybillSegmentDto> waybill) {
        when(lomClient.getOrder(ORDER_ID, Set.of())).thenReturn(Optional.of(
            new OrderDto()
                .setPlatformClientId(PLATFORM_CLIENT_ID)
                .setId(ORDER_ID)
                .setBarcode(BARCODE)
                .setSenderId(SENDER_ID)
                .setDeliveryType(DeliveryType.COURIER)
                .setRecipient(
                    RecipientDto.builder()
                        .address(AddressDto.builder().geoId(2).build())
                        .build()
                )
                .setWaybill(waybill)
        ));
    }

    @Nonnull
    private WaybillSegmentDto waybillSegment(
        long partnerId,
        ShipmentType shipmentType,
        PartnerType partnerType,
        @Nullable LocalDate shipmentDate
    ) {
        return WaybillSegmentDto.builder()
            .partnerId(partnerId)
            .partnerType(partnerType)
            .shipment(
                WaybillSegmentDto.ShipmentDto.builder()
                    .type(shipmentType)
                    .date(shipmentDate)
                    .locationFrom(LocationDto.builder().warehouseId(WAREHOUSE_FROM_ID).build())
                    .build()
            )
            .build();
    }

    @Nonnull
    private OrderToShipDto orderToShip(
        long partnerId,
        LocalDate shipmentDate,
        CapacityServiceType capacityServiceType
    ) {
        return new OrderToShipDto(
            BARCODE,
            PLATFORM_CLIENT_ID,
            partnerId,
            (long) LOCATION_FROM,
            (long) LOCATION_TO,
            ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType.DELIVERY,
            capacityServiceType,
            shipmentDate
        );
    }

    private void process() {
        processor.processPayload(new OrderIdPayload(ORDER_ID, ""));
    }

}
