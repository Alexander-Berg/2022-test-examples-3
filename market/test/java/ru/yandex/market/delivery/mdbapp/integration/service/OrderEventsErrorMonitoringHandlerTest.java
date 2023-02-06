package ru.yandex.market.delivery.mdbapp.integration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.MessagingException;

import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.logging.OrderEventAction;
import ru.yandex.market.delivery.mdbapp.components.logging.OrderEventField;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.enums.ProcessingOrderErrorType;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.EventCreateRequest;
import ru.yandex.market.logistics.mqm.model.request.monitoringevent.MdbProcessingOrderErrorPayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderEventsErrorMonitoringHandlerTest {

    @Mock
    private MqmClient mqmClient;

    @Mock
    private EventFlowParametersHolder parametersHolder;

    @InjectMocks
    private OrderEventsErrorMonitoringHandler errorMonitoringHandler;

    @Test
    public void testMonitoringOrderCreateEventPushed() {
        mockParameterHolder(OrderEventAction.FF_ORDER_CREATE);

        errorMonitoringHandler.handle(new MessagingException("Some exception while creating order"));

        captureAndCheckIssueCreateRequest(
            OrderEventAction.FF_ORDER_CREATE.name(),
            ProcessingOrderErrorType.CREATE_ORDER
        );
    }

    @Test
    public void testMonitoringOrderUpdateEventPushed() {
        mockParameterHolder(OrderEventAction.SC_ORDER_UPDATE);

        errorMonitoringHandler.handle(new MessagingException("Some exception while creating order"));

        captureAndCheckIssueCreateRequest(
            OrderEventAction.SC_ORDER_UPDATE.name(),
            ProcessingOrderErrorType.UPDATE_ORDER
        );
    }

    private void mockParameterHolder(OrderEventAction action) {
        when(parametersHolder.getParameter(OrderEventField.EVENT_ID)).thenReturn("123");
        when(parametersHolder.getParameter(OrderEventField.ORDER_ID)).thenReturn("345");
        when(parametersHolder.getAction()).thenReturn(action);
    }

    private void captureAndCheckIssueCreateRequest(String action, ProcessingOrderErrorType errorType) {
        ArgumentCaptor<EventCreateRequest> requestCaptor = ArgumentCaptor.forClass(EventCreateRequest.class);

        verify(mqmClient, atLeastOnce()).pushMonitoringEvent(requestCaptor.capture());
        EventCreateRequest request = requestCaptor.getValue();

        assertEquals(request.getEventType(), EventType.MDB_PROCESSING_ORDER_ERROR);
        checkPayload((MdbProcessingOrderErrorPayload) request.getPayload(), errorType, action);
    }

    private void checkPayload(
        MdbProcessingOrderErrorPayload payload,
        ProcessingOrderErrorType errorType,
        String action
    ) {
        assertEquals(payload.getErrorType(), errorType);
        assertEquals(payload.getErrorActionName(), action);
        assertEquals("null", payload.getExceptionHeader());
        assertThat(payload.getExceptionTrace().startsWith(
            "Exception class: org.springframework.messaging.MessagingException\n"
                + "Message: Some exception while creating order\n"
                + "Stack trace:\n"
                + "org.springframework.messaging.MessagingException: Some exception while creating order"
        ));
    }

    @Test
    public void testMonitoringEventNotPushed() {
        when(parametersHolder.getAction()).thenReturn(OrderEventAction.GET_TARIFF_DATA);

        errorMonitoringHandler.handle(new MessagingException("Some exception while creating order"));

        verify(mqmClient, never()).pushMonitoringEvent(any());
    }
}
