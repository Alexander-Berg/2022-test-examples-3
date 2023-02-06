package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;
import ru.yandex.market.logistics.lom.service.async.FulfillmentUpdateOrderAsyncResultService;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.LomCreateOrderErrorPayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FulfillmentServiceUpdateOrderAsyncResultTest extends AbstractContextualTest {
    @Autowired
    private FulfillmentUpdateOrderAsyncResultService fulfillmentUpdateOrderAsyncResultService;

    @Autowired
    private MqmClient mqmClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    void success() {
        fulfillmentUpdateOrderAsyncResultService.processSuccess(
            new BusinessProcessState(),
            new UpdateOrderSuccessDto("LO-123", 23L, 1L)
        );
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_EXTERNAL_SUCCESS\t" +
                "payload=Order updated\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=barcode,partner\t" +
                "entity_values=barcode:LO-123,partner:23"
        );
    }

    @Test
    @DatabaseSetup("/controller/order/update/async/before/fulfillment_setup.xml")
    void error() {
        fulfillmentUpdateOrderAsyncResultService.processError(
            createBusinessProcessState(),
            createUpdateOrderErrorDto()
        );
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=ERROR\t" +
                "format=plain\t" +
                "code=UPDATE_ORDER_EXTERNAL_ERROR\t" +
                "payload=Code: 1234, message: 'error message'\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=barcode,partner\t" +
                "entity_values=barcode:LO-123,partner:12"
        );

        verify(mqmClient).pushMonitoringEvent(new EventCreateRequest(
                EventType.LOM_CREATE_ORDER_ERROR,
                new LomCreateOrderErrorPayload(
                    "LO-123",
                    12L,
                    "",
                    1234,
                    "error message",
                    "FF",
                    1L,
                    false,
                    1L,
                    Instant.parse("2018-01-01T12:00:00.00Z"),
                    "VALIDATION_ERROR",
                    1L
                )
            )
        );
    }

    @Test
    @DatabaseSetup("/controller/order/update/async/before/fulfillment_setup.xml")
    void errorMqmException() {
        doThrow(new RuntimeException()).when(mqmClient).pushMonitoringEvent(any());
        fulfillmentUpdateOrderAsyncResultService.processError(
            createBusinessProcessState(),
            createUpdateOrderErrorDto()
        );
        verify(mqmClient).pushMonitoringEvent(any());
    }

    @Test
    @DisplayName("Обработка ошибки не отправляется т.к. есть 120 ЧП")
    @DatabaseSetup("/controller/order/update/async/before/fulfillment_with_ready_to_ship_setup.xml")
    void errorWithoutOrderErrorNotifyBecause120() {
        fulfillmentUpdateOrderAsyncResultService.processError(
            createBusinessProcessState(),
            createUpdateOrderErrorDto()
        );
    }

    @Test
    @DisplayName("Обработка ошибки не отправляется т.к. заказ отменен")
    @DatabaseSetup("/controller/order/update/async/before/fulfillment_with_ready_to_ship_setup_canceled.xml")
    void errorWithoutOrderErrorNotifyBecauseOrderIsCanceled() {
        fulfillmentUpdateOrderAsyncResultService.processError(
            createBusinessProcessState(),
            createUpdateOrderErrorDto()
        );
        verify(mqmClient, never()).pushMonitoringEvent(any());
    }

    @Nonnull
    private static BusinessProcessState createBusinessProcessState() {
        BusinessProcessState businessProcessState = mock(BusinessProcessState.class);
        when(businessProcessState.getEntityId(EntityType.WAYBILL_SEGMENT)).thenReturn(1L);
        when(businessProcessState.getId()).thenReturn(1L);
        return businessProcessState;
    }

    @Nonnull
    private static UpdateOrderErrorDto createUpdateOrderErrorDto() {
        return new UpdateOrderErrorDto(
            "LO-123",
            12L,
            1L,
            "error message",
            1234,
            false
        );
    }
}
