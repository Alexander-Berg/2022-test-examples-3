package ru.yandex.market.checkout.checkouter.events;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EventsControllerOrderStatusTest extends AbstractWebTestBase {

    @Autowired
    TestSerializationService testSerializationService;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @DisplayName("Ручка /orders/events должна фильтровать по статусу")
    @Test
    public void shouldFilterByOrderStatus() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        long orderId = order.getId();
        OrderStatus orderStatus = OrderStatus.PROCESSING;

        MvcResult result = mockMvc.perform(get("/orders/events/by-order-id")
                .param("orderId", String.valueOf(orderId))
                .param("orderStatus", orderStatus.name()))
                .andExpect(status().isOk())
                .andReturn();

        OrderHistoryEvents orderHistoryEvents =
                testSerializationService.deserializeCheckouterObject(result.getResponse().getContentAsString(),
                        OrderHistoryEvents.class);
        Assertions.assertTrue(orderHistoryEvents.getContent().stream()
                .allMatch(ohe -> ohe.getOrderAfter().getStatus() == orderStatus), "should return only events with " +
                "request status");
    }

    @DisplayName("Пейджер должен выдавать столько же событий в total, сколько выдает ручка /orders/{orderId}/events")
    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @Test
    public void shouldFilterByOrderStatusInSingleOrderEvents() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        long orderId = order.getId();

        MvcResult result = mockMvc.perform(get("/orders/{id}/events", orderId)
                .param("eventTypes", HistoryEventType.ORDER_STATUS_UPDATED.name())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        PagedEvents pagedEvents = testSerializationService.deserializeCheckouterObject(response, PagedEvents.class);

        Assertions.assertEquals(pagedEvents.getItems().size(), pagedEvents.getPager().getTotal().intValue());
    }
}
