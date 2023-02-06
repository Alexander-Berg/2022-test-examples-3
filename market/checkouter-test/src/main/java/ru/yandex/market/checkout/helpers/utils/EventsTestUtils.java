package ru.yandex.market.checkout.helpers.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.ClientRoleHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mmetlov
 */
public final class EventsTestUtils {

    private EventsTestUtils() {
    }

    public static Collection<Object[]> parameters(Color rgb) {
        return Arrays.asList(
                new Object[]{
                        "GET /orders/{orderId}/events",
                        (EventGetter) (orderId, mockMvc, serializationService) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/{orderId}/events", orderId);
                            ClientRoleHelper.addClientInfoParameters(ClientInfo.SYSTEM, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            PagedEvents events = serializationService.deserializeCheckouterObject(
                                    result.getResponse().getContentAsString(), PagedEvents.class);
                            return events.getItems();
                        }
                },
                new Object[]{
                        "GET /orders/events?lastEventId=?",
                        (EventGetter) (orderId, mockMvc, serializationService) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events")
                                    .param("lastEventId", "0")
                                    .param("withWaitInterval", "false")
                                    .param("rgb", rgb.name());

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            OrderHistoryEvents response = serializationService.deserializeCheckouterObject(
                                    result.getResponse().getContentAsString(), OrderHistoryEvents.class);
                            List<OrderHistoryEvent> events = new ArrayList<>(response.getContent());
                            Collections.reverse(events);
                            return events;
                        }
                },
                new Object[]{
                        "GET /orders/events/by-order-id?orderId=?",
                        (EventGetter) (orderId, mockMvc, serializationService) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events/by-order-id")
                                    .param("orderId", orderId.toString());

                            ClientRoleHelper.addClientInfoParameters(ClientInfo.SYSTEM, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            OrderHistoryEvents response = serializationService.deserializeCheckouterObject(
                                    result.getResponse().getContentAsString(), OrderHistoryEvents.class);
                            return response.getContent();
                        }
                }
        );
    }

    // только для ручек с параметром clientRole
    public static Collection<Object[]> parameters(ClientInfo clientInfo) {
        return Arrays.asList(
                new Object[]{
                        "GET /orders/{orderId}/events " + clientInfo.getRole(),
                        (EventGetter) (orderId, mockMvc, serializationService) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/{orderId}/events", orderId);
                            ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            PagedEvents events = serializationService.deserializeCheckouterObject(
                                    result.getResponse().getContentAsString(), PagedEvents.class);
                            return events.getItems();
                        }
                },
                new Object[]{
                        "GET /orders/events/by-order-id?orderId=? " + clientInfo.getRole(),
                        (EventGetter) (orderId, mockMvc, serializationService) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events/by-order-id")
                                    .param("orderId", orderId.toString());

                            ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            OrderHistoryEvents response = serializationService.deserializeCheckouterObject(
                                    result.getResponse().getContentAsString(), OrderHistoryEvents.class);
                            return response.getContent();
                        }
                }
        );
    }

    public interface EventGetter {

        Collection<OrderHistoryEvent> getEvents(Long orderId, MockMvc mockMvc,
                                                TestSerializationService serializationService) throws Exception;
    }
}
