package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.logistics4shops.ExcludeOrderFromShipmentRequestCreated;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.factory.TmFactory;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.ExportExcludeOrderFromShipmentEventPayload;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Задача экспорта события о создании заявки на перенос заказа из огрузки")
@DatabaseSetup("/queue/processor/excludeorderfromshipment/export/before/prepare.xml")
class ExportExcludeOrderFromShipmentEventProcessorTest extends AbstractIntegrationTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-12-12T00:00:00Z");
    @Autowired
    private TransportManagerClient tmClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private LesProducer lesProducer;
    @Autowired
    private ExportExcludeOrderFromShipmentEventProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lesProducer);
    }

    @Test
    @DisplayName("Успешная отправка ивента о создании заявки на исключение заказа от отгрузки")
    @ExpectedDatabase(
        value = "/queue/processor/excludeorderfromshipment/export/after/request_is_processing.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void success() {
        mockClients();
        softly.assertThat(processor.execute(buildPayload(1L)))
            .isEqualTo(TaskExecutionResult.finish());

        verify(lesProducer).send(buildEvent(), "logistics4shops_out");
    }

    @Test
    @DisplayName("Отправка ивента о создании заявки на исключение заказа от отгрузки - заявка уже отправлена")
    @DatabaseSetup(
        value = "/queue/processor/excludeorderfromshipment/export/before/request_is_already_processing.xml",
        type = DatabaseOperation.REFRESH
    )
    void requestInWrongStatus() {
        softly.assertThat(processor.execute(buildPayload(1L)))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DisplayName("Отправка ивента о создании заявки на исключение заказа от отгрузки - заявки не существует")
    void requestIsNotFound() {
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(2L)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [EXCLUDE_ORDER_FROM_SHIPMENT_REQUEST] with id [2]");
    }

    @Test
    @DisplayName("Отправка ивента о создании заявки на исключение заказа от отгрузки - LES недоступен")
    void lesIsUnavailable() {
        mockClients();
        doThrow(new RuntimeException("LES is unavailable")).when(lesProducer).send(any(), any());
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(1L)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("LES is unavailable");
        verify(lesProducer).send(buildEvent(), "logistics4shops_out");
    }

    @Test
    @DatabaseSetup(
        value = "/queue/processor/excludeorderfromshipment/export/before/outbound_confirmed.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Отправка ивента о создании заявки на исключение заказа от отгрузки - ошибка валидации отгрузки")
    void outboundIsConfirmed() {
        mockClients();
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(1L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Validation failed for shipment from request 1. Reason: SHIPMENT_IS_CONFIRMED");
    }

    @Test
    @DisplayName("Отправка ивента о создании заявки на исключение заказа от отгрузки - ошибка валидации заказа")
    void orderDoesNotBelongToShipment() {
        when(tmClient.getTransportation(1000L)).thenReturn(Optional.of(TmFactory.transportation()));
        when(tmClient.searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class)))
            .thenReturn(new Page<RegisterUnitDto>().setData(List.of()));
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(
                LogisticsPointResponse.newBuilder()
                    .address(Address.newBuilder().locationId(213).build())
                    .build()
            )
        );
        softly.assertThatThrownBy(() -> processor.execute(buildPayload(1L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Validation failed for order from request 1. Reasons: [DOES_NOT_BELONG_TO_SHIPMENT]");
    }

    @Nonnull
    private Event buildEvent() {
        return new Event(
            "logistics4shops",
            "1",
            FIXED_TIME.toEpochMilli(),
            ExcludeOrderFromShipmentRequestCreated.EVENT_TYPE,
            new ExcludeOrderFromShipmentRequestCreated(
                "1",
                1L
            ),
            ""
        );
    }

    @Nonnull
    private ExportExcludeOrderFromShipmentEventPayload buildPayload(long excludeOrderFromShipmentRequestId) {
        return ExportExcludeOrderFromShipmentEventPayload.builder()
            .excludeOrderFromShipmentRequestId(excludeOrderFromShipmentRequestId)
            .eventId(1L)
            .orderId(1L)
            .build();
    }


    private void mockClients() {
        when(tmClient.getTransportation(1000L)).thenReturn(Optional.of(TmFactory.transportation()));
        when(tmClient.searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class)))
            .thenReturn(new Page<RegisterUnitDto>().setData(List.of(TmFactory.registerUnitDto())));
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(
                LogisticsPointResponse.newBuilder()
                    .address(Address.newBuilder().locationId(213).build())
                    .build()
            )
        );
    }
}
