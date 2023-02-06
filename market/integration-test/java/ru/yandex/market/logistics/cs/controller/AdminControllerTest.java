package ru.yandex.market.logistics.cs.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.request.OrdersEventsRequest;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.dbqueue.logbroker.checkouter.LogbrokerCheckouterConsumptionProducer;
import ru.yandex.market.logistics.cs.domain.dto.CapacityValueCounterDto;
import ru.yandex.market.logistics.cs.domain.dto.InternalEventDto;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderEventConsumer;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

@DisplayName("Админский контроллер")
class AdminControllerTest extends AbstractIntegrationTest {
    @Autowired
    private CheckouterOrderHistoryEventsApi orderHistoryEventsApi;
    @Autowired
    private OrderEventConsumer orderEventConsumer;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private LogbrokerCheckouterConsumptionProducer producer;

    @Test
    @Disabled("Нужен реальный пример события")
    @SneakyThrows
    @DisplayName("Импорт эвентов")
    void importHistoryEvents() {
        OrderHistoryEvent expectedEvent = TestDtoFactory
            .randomHistoryEventWithRoute(NEW_ORDER, PROCESSING, PROCESSING)
            .getEvent();

        MockHttpServletRequestBuilder operation =
            MockMvcRequestBuilders.post("/admin/import-history-events")
                .content(mapper.writeValueAsString(List.of(expectedEvent)))
                .contentType(ContentType.APPLICATION_JSON.getMimeType());

        ResultActions resultActions = mockMvc.perform(operation);
        assertEquals(resultActions.andReturn().getResponse().getStatus(), HttpStatus.OK.value());

        verify(orderEventConsumer, times(1)).consume(List.of(expectedEvent));
    }

    @Test
    @SneakyThrows
    @DisplayName("Импорт заказов")
    void importOrders() {
        ArgumentCaptor<OrdersEventsRequest> requestCaptor = ArgumentCaptor.forClass(OrdersEventsRequest.class);
        OrderHistoryEvent expectedEvent = TestDtoFactory.orderHistoryEvent(NEW_ORDER);

        when(orderHistoryEventsApi.getOrdersHistoryEvents(requestCaptor.capture()))
            .thenReturn(new OrderHistoryEvents(List.of(expectedEvent)));

        MockHttpServletRequestBuilder operation =
            MockMvcRequestBuilders.post("/admin/import-orders")
                .content("[1, 2, 3]")
                .contentType(ContentType.APPLICATION_JSON.getMimeType());

        ResultActions resultActions = mockMvc.perform(operation);
        assertEquals(resultActions.andReturn().getResponse().getStatus(), HttpStatus.OK.value());

        OrdersEventsRequest request = requestCaptor.getValue();
        softly.assertThat(request)
            .usingRecursiveComparison()
            .isEqualTo(OrdersEventsRequest.builder(new long[] {1, 2, 3})
                .withEventTypes(new HistoryEventType[] {NEW_ORDER, ORDER_STATUS_UPDATED})
                .build());

        verify(orderHistoryEventsApi, times(1)).getOrdersHistoryEvents(request);
        verify(orderEventConsumer, times(1)).consume(List.of(expectedEvent));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение списка событий со счеткимами по id заказа")
    @DatabaseSetup("/controller/events/events_with_counters.xml")
    void getEventsByOrderId() {
        MockHttpServletRequestBuilder operation =
            MockMvcRequestBuilders.get("/admin/internal-order-events")
                .accept(MediaType.APPLICATION_JSON)
                .param("orderId", "123123");

        ResultActions resultActions = mockMvc.perform(operation);
        assertEquals(HttpStatus.OK.value(), resultActions.andReturn().getResponse().getStatus());

        List<InternalEventDto> events = mapper.readValue(
            resultActions.andReturn().getResponse().getContentAsString(),
            new TypeReference<List<InternalEventDto>>() {
            }
        );

        assertEquals(4, events.size());
        events.forEach(event -> {
            assertEquals("123123", event.getKey().split("_")[0]);
            switch (event.getType()) {
                case NEW:
                    assertTrue(event.isProcessed());
                    assertEquals(2, event.getCounters().size());
                    break;
                case CHANGE_ROUTE:
                    assertTrue(event.isProcessed());
                    assertEquals(4, event.getCounters().size());
                    break;
                case CANCELLED:
                    assertFalse(event.isProcessed());
                    assertEquals(0, event.getCounters().size());
                    break;
                default:
                    fail("Unknown event type");
            }
        });

        Set<CapacityValueCounterDto> allCounters = events.stream()
            .flatMap(internalEventDto -> internalEventDto.getCounters().stream())
            .collect(Collectors.toSet());
        assertEquals(6, allCounters.size());
    }
}
