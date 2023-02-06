package ru.yandex.market.checkout.checkouter.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.collections.CollectionUtils.first;

/**
 * Тесты проходят для ручек
 * GET /orders/{orderId}/events
 * GET /orders/events?lastEventId=?
 * GET /orders/events?orderId=?
 *
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */

public class OrderHistoryEventsRoleControllerTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    private static void applyShowCheckpointEvents(boolean showCheckpointEvents, MockHttpServletRequestBuilder builder) {
        if (!showCheckpointEvents) {
            builder.param("eventTypes", "TRACK_CHECKPOINT_CHANGED")
                    .param("eventType", "TRACK_CHECKPOINT_CHANGED")
                    .param("ignoreEventTypes", "true");
        }
    }

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{
                        "GET /orders/{orderId}/events",
                        (EventChecker) (order, showCheckpointEvents, mockMvc, bodyUtils,
                                        clientRole, clientId, shopId) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/{orderId}/events", order.getId());
                            buildRoles(clientRole, clientId, shopId, false, builder);

                            applyShowCheckpointEvents(showCheckpointEvents, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            String contentAsString = result.getResponse().getContentAsString();

                            PagedEvents events = bodyUtils.deserializeCheckouterObject(contentAsString,
                                    PagedEvents.class);

                            return new ArrayList<>(events.getItems());
                        }
                },
                new Object[]{
                        "GET /orders/events?lastEventId=?",
                        (EventChecker) (order, showCheckpointEvents, mockMvc, bodyUtils,
                                        clientRole, clientId, shopId) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events")
                                    .param("lastEventId", "0")
                                    .param("rgb", "BLUE")
                                    .param("withWaitInterval", "false");

                            buildRoles(clientRole, clientId, shopId, true, builder);

                            applyShowCheckpointEvents(showCheckpointEvents, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            String contentAsString = result.getResponse().getContentAsString();

                            OrderHistoryEvents response = bodyUtils.deserializeCheckouterObject(contentAsString,
                                    OrderHistoryEvents.class);
                            return new ArrayList<>(response.getContent());
                        }
                },
                new Object[]{
                        "GET /orders/events/by-order-id?orderId=?",
                        (EventChecker) (order, showCheckpointEvents, mockMvc, bodyUtils,
                                        clientRole, clientId, shopId) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events/by-order-id")
                                    .param("orderId", order.getId().toString());

                            buildRoles(clientRole, clientId, shopId, false, builder);

                            applyShowCheckpointEvents(showCheckpointEvents, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            String contentAsString = result.getResponse().getContentAsString();

                            OrderHistoryEvents response = bodyUtils.deserializeCheckouterObject(contentAsString,
                                    OrderHistoryEvents.class);
                            return new ArrayList<>(response.getContent());
                        }
                }
        ).stream().map(Arguments::of);
    }

    private static void buildRoles(@Nullable ClientRole clientRole,
                                   @Nullable Long clientId,
                                   @Nullable Long shopId,
                                   boolean clientShopId,
                                   @Nonnull MockHttpServletRequestBuilder builder) {
        if (clientRole != null) {
            builder.param(CheckouterClientParams.CLIENT_ROLE, String.valueOf(clientRole));
        }
        if (clientId != null) {
            builder.param(CheckouterClientParams.CLIENT_ID, String.valueOf(clientId));
        }
        if (shopId != null) {
            if (clientShopId) {
                builder.param(CheckouterClientParams.CLIENT_SHOP_ID, String.valueOf(shopId));
            } else {
                builder.param(CheckouterClientParams.SHOP_ID, String.valueOf(shopId));
            }
        }
    }

    private Order createOrder() {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        order.setRgb(Color.BLUE);
        return order;
    }

    private Order editDelivery(Order order,
                               Delivery newDelivery,
                               ClientInfo clientInfo) {
        return orderUpdateService.updateOrderDelivery(order.getId(), newDelivery, clientInfo);
    }

    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetEditParcelItemEvent(@Nonnull String method,
                                           @Nonnull EventChecker eventChecker) throws Exception {
        // Подготовка
        Order order = createOrder();

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = first(order.getItems()).getId();

        Parcel newShipment = new Parcel();
        newShipment.addParcelItem(new ParcelItem(itemId, 4));
        order.getDelivery().setParcels(Collections.singletonList(newShipment));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        ClientInfo shopClientInfo = new ClientInfo(ClientRole.SHOP, order.getShopId());
        editDelivery(order, newDelivery, shopClientInfo);


        newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());
        newShipment.addParcelItem(new ParcelItem(itemId, 5));

        newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        ClientInfo shopUserClientInfo = new ClientInfo(ClientRole.SHOP_USER, 12345L, order.getShopId());
        editDelivery(order, newDelivery, shopUserClientInfo);

        // Действия
        List<OrderHistoryEvent> systemEvents = eventChecker.check(order,
                true,
                mockMvc,
                testSerializationService,
                ClientRole.SYSTEM,
                null, null);
        Assertions.assertEquals(6, systemEvents.size());

        List<OrderHistoryEvent> shopEvents = eventChecker.check(order,
                true,
                mockMvc,
                testSerializationService,
                ClientRole.SHOP,
                order.getShopId(), 12345L);
        Assertions.assertEquals(5, shopEvents.size());

        List<OrderHistoryEvent> shopUserEvents = eventChecker.check(order,
                true,
                mockMvc,
                testSerializationService,
                ClientRole.SHOP_USER,
                12345L, order.getShopId());
        Assertions.assertEquals(5, shopUserEvents.size());
    }

    @FunctionalInterface
    protected interface EventChecker {

        List<OrderHistoryEvent> check(@Nonnull Order order,
                                      boolean showCheckpointEvents,
                                      @Nonnull MockMvc mockMvc,
                                      @Nonnull TestSerializationService bodyUtils,
                                      @Nullable ClientRole clientRole,
                                      @Nullable Long clientId,
                                      @Nullable Long shopId) throws Exception;
    }
}
