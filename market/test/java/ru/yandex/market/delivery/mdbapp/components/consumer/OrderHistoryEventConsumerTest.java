package ru.yandex.market.delivery.mdbapp.components.consumer;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class OrderHistoryEventConsumerTest extends AbstractTest {

    OrderEventsService eventsService = Mockito.mock(OrderEventsService.class);
    CheckouterServiceClient checkouterServiceClient = Mockito.mock(CheckouterServiceClient.class);
    OrderHistoryEventConsumer orderHistoryEventConsumer = new OrderHistoryEventConsumer(
        checkouterServiceClient,
        eventsService
    );

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(eventsService, checkouterServiceClient);
    }

    @Test
    public void testSuccess() {
        OrderHistoryEvent event = createEvent();
        when(checkouterServiceClient.isEligibleForProcessing(eq(HistoryEventType.NEW_ORDER))).thenReturn(true);
        orderHistoryEventConsumer.accept(List.of(event));
        verify(eventsService).enqueueEvent(eq(event));
        verify(checkouterServiceClient).isEligibleForProcessing(eq(HistoryEventType.NEW_ORDER));
    }

    @Test
    public void testNotSupportedEventType() {
        OrderHistoryEvent event = createEvent();
        when(checkouterServiceClient.isEligibleForProcessing(eq(HistoryEventType.NEW_ORDER))).thenReturn(false);
        orderHistoryEventConsumer.accept(List.of(event));
        verify(checkouterServiceClient).isEligibleForProcessing(eq(HistoryEventType.NEW_ORDER));
    }

    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(10L);
        event.setType(HistoryEventType.NEW_ORDER);
        Order orderAfter = new Order();
        orderAfter.setId(123L);
        event.setOrderAfter(orderAfter);
        return event;
    }

}
