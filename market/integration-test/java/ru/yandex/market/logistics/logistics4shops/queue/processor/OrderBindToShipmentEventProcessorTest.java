package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.OrderBindToShipmentEventPayload;
import ru.yandex.market.logistics.logistics4shops.utils.CheckouterModelFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Обработка события назначения заказу отгрузки")
class OrderBindToShipmentEventProcessorTest extends AbstractIntegrationTest {
    private static final OrderBindToShipmentEventPayload PAYLOAD = OrderBindToShipmentEventPayload.builder()
        .orderBarcode("1")
        .transportationId(2L)
        .outboundId(3L)
        .build();
    private static final RequestClientInfo REQUEST_CLIENT_INFO = CheckouterModelFactory.requestClientInfo(SYSTEM);
    private static final OrderRequest ORDER_REQUEST  = CheckouterModelFactory.orderRequest(1L);
    @Autowired
    private OrderBindToShipmentEventProcessor processor;

    @Autowired
    private CheckouterAPI checkouterApi;

    @Autowired
    private TransportManagerClient transportManagerClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(checkouterApi, transportManagerClient);
    }

    @Test
    @DisplayName("Нет заявок на исключение")
    void orderHasNoRequest() {
        processor.execute(PAYLOAD);
    }

    @Test
    @DisplayName("Заказ не найден")
    @DatabaseSetup("/queue/processor/excludeorderfromshipment/orderbind/before/order_has_request.xml")
    void orderNotFound() {
        when(checkouterApi.getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST)))
            .thenThrow(new OrderNotFoundException(1L));
        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found: 1");
        verify(checkouterApi).getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST));
    }

    @Test
    @DisplayName("Посылка не найдена")
    @DatabaseSetup("/queue/processor/excludeorderfromshipment/orderbind/before/order_has_request.xml")
    void parcelNotFound() {
        when(checkouterApi.getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST)))
            .thenReturn(CheckouterModelFactory.buildOrderWithParcels(List.of()));
        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Parcel for order 1 not found");
        verify(checkouterApi).getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST));
    }

    @Test
    @DisplayName("Перемещение не найдено")
    @DatabaseSetup("/queue/processor/excludeorderfromshipment/orderbind/before/order_has_request.xml")
    void transportationNotFound() {
        when(checkouterApi.getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST)))
            .thenReturn(CheckouterModelFactory.buildOrderWithDefaultParcel());
        softly.assertThatThrownBy(() -> processor.execute(PAYLOAD))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SHIPMENT] with id [2]");
        verify(checkouterApi).getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST));
        verify(transportManagerClient).getTransportation(2L);
    }

    @Test
    @DisplayName("Успешное обновление даты отгрузки")
    @DatabaseSetup("/queue/processor/excludeorderfromshipment/orderbind/before/order_has_request.xml")
    void shipmentDateIsUpdated() {
        when(transportManagerClient.getTransportation(2L)).thenReturn(
            Optional.of(new TransportationDto().setOutbound(
                TransportationUnitDto.builder()
                    .plannedIntervalEnd(LocalDateTime.of(2022, 1, 2, 12, 0))
                    .build())
            )
        );
        when(checkouterApi.getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST)))
            .thenReturn(CheckouterModelFactory.buildOrderWithDefaultParcel());
        processor.execute(PAYLOAD);
        verify(checkouterApi).getOrder(safeRefEq(REQUEST_CLIENT_INFO), safeRefEq(ORDER_REQUEST));
        verify(transportManagerClient).getTransportation(2L);
        verifyShipmentUpdate();
    }

    private void verifyShipmentUpdate() {
        ParcelPatchRequest parcelPatchRequest = new ParcelPatchRequest();
        parcelPatchRequest.setShipmentDateTimeBySupplier(LocalDateTime.of(2022, 1, 2, 12, 0));
        parcelPatchRequest.setReason(HistoryEventReason.DELAYED_DUE_EXTERNAL_CONDITIONS);

        verify(checkouterApi).updateParcel(eq(1L), eq(101L), safeRefEq(parcelPatchRequest), eq(SYSTEM), eq(0L));
    }
}
