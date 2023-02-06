package ru.yandex.market.checkout.checkouter.order.status;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;

public class OrderControllerDeliveredWithRealDeliveryDateTest extends AbstractWebTestBase {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(CheckouterDateFormats.DATE_FORMAT);

    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    public static Stream<Arguments> realDeliveryDateForCancelledOrDeliveredFromPickupTestData() {
        return Stream.of(
                new Object[]{DELIVERED, null},
                new Object[]{CANCELLED, OrderSubstatus.USER_CHANGED_MIND}
        ).map(Arguments::of);
    }

    public static Stream<Arguments> saveRealDeliveryDateTestData() throws ParseException {
        return Stream.of(
                new Object[]{DELIVERED, "25-10-2021", status().isOk(), DATE_FORMATTER.parse("25-10-2021")},
                new Object[]{PICKUP, "25-10-2021", status().isOk(), DATE_FORMATTER.parse("25-10-2021")},
                new Object[]{DELIVERED, "26-10-2021", status().isOk(), DATE_FORMATTER.parse("26-10-2021")},
                new Object[]{PICKUP, "26-10-2021", status().isOk(), DATE_FORMATTER.parse("26-10-2021")},
                new Object[]{DELIVERED, "30-10-2021", status().isOk(), DATE_FORMATTER.parse("30-10-2021")},
                new Object[]{PICKUP, "30-10-2021", status().isOk(), DATE_FORMATTER.parse("30-10-2021")},
                new Object[]{DELIVERED, null, status().isOk(), DATE_FORMATTER.parse("30-10-2021")},
                new Object[]{PICKUP, null, status().isOk(), DATE_FORMATTER.parse("30-10-2021")},
                // дата позже чем текущая
                new Object[]{DELIVERED, "31-10-2021", status().isBadRequest(), null},
                new Object[]{PICKUP, "31-10-2021", status().isBadRequest(), null},
                // дата раньше чем дата создания заказа
                new Object[]{DELIVERED, "24-10-2021", status().isBadRequest(), null},
                new Object[]{PICKUP, "24-10-2021", status().isBadRequest(), null}


        ).map(Arguments::of);
    }

    @Test
    void shouldNotAllowForNotDBSOrder() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        updateOrderStatusDeliveredWithRealDeliveryDate(order, ClientRole.SHOP, DELIVERED,
                status().isBadRequest());
    }

    @Test
    void shouldNotAllowForNotShopUser() {
        Order order = createDBSOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        updateOrderStatusDeliveredWithRealDeliveryDate(order, ClientRole.USER, DELIVERED,
                status().isForbidden());
    }

    @Test
    void shouldNotAllowChangeStatusNotDeliveredOrPickup() {
        Order order = createDBSOrder();
        updateOrderStatusDeliveredWithRealDeliveryDate(order, ClientRole.SHOP, OrderStatus.DELIVERY,
                status().isBadRequest());
        updateOrderStatusDeliveredWithRealDeliveryDate(order, ClientRole.SHOP_USER, OrderStatus.CANCELLED,
                status().isBadRequest());

    }

    @Test
    void shouldNotShowRealDeliveryDateForDeliveredNotDBSOrder() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Order deliveryOrder = orderService.getOrder(order.getId());
        assertNull(deliveryOrder.getDelivery().getRealDeliveryDate());

        orderStatusHelper.proceedOrderToStatus(deliveryOrder, DELIVERED);
        Order deliveredOrder = orderService.getOrder(order.getId());
        assertNull(deliveredOrder.getDelivery().getRealDeliveryDate());
    }

    @ParameterizedTest(name = "OrderStatus = {0}")
    @EnumSource(value = OrderStatus.class, names = {"PICKUP", "DELIVERED"})
    void shouldShowRealDeliveryDateForDeliveredDBSOrder(OrderStatus status) throws Exception {
        freezeTimeAt("2021-10-30T18:00:00Z");
        Order order = createDBSOrderByDeliveredStatus(status);

        orderStatusHelper.proceedOrderToStatus(order, status);
        Order deliveredOrder = orderService.getOrder(order.getId());
        assertEquals("30-10-2021", DATE_FORMATTER.format(deliveredOrder.getDelivery().getRealDeliveryDate()));

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(deliveredOrder.getId());
        OrderHistoryEvent orderHistoryEvent = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_STATUS_UPDATED)
                .filter(ohe -> status.equals(ohe.getOrderAfter().getStatus()))
                .findAny()
                .orElse(null);

        assertNotNull(orderHistoryEvent, "Should generate ORDER_STATUS_UPDATED event");

        Order orderAfter = orderHistoryEvent.getOrderAfter();
        assertEquals("30-10-2021",
                DATE_FORMATTER.format(orderAfter.getProperty(OrderPropertyType.REAL_DELIVERY_DATE)));
        assertEquals("30-10-2021",
                DATE_FORMATTER.format(orderAfter.getDelivery().getRealDeliveryDate()));
    }

    @ParameterizedTest(name = "OrderStatus = {0}")
    @MethodSource("realDeliveryDateForCancelledOrDeliveredFromPickupTestData")
    void shouldShowRealDeliveryDateForCancelledOrDeliveredFromPickupDBSOrder(
            OrderStatus status, OrderSubstatus substatus) throws Exception {
        freezeTimeAt("2021-10-25T18:00:00Z");
        Order order = createDBSOrderByDeliveredStatus(PICKUP);

        orderStatusHelper.proceedOrderToStatus(order, PICKUP);
        Order pickupOrder = orderService.getOrder(order.getId());
        assertEquals("25-10-2021", DATE_FORMATTER.format(pickupOrder.getDelivery().getRealDeliveryDate()));

        freezeTimeAt("2021-10-30T18:00:00Z");
        orderStatusHelper.updateOrderStatus(order.getId(), status, substatus);

        Order updatedOrder = orderService.getOrder(order.getId());
        assertEquals("25-10-2021", DATE_FORMATTER.format(updatedOrder.getDelivery().getRealDeliveryDate()));
        assertEquals("25-10-2021",
                DATE_FORMATTER.format(updatedOrder.getProperty(OrderPropertyType.REAL_DELIVERY_DATE)));

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(updatedOrder.getId());
        OrderHistoryEvent orderHistoryEvent = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_STATUS_UPDATED)
                .filter(ohe -> status.equals(ohe.getOrderAfter().getStatus()))
                .findAny()
                .orElse(null);

        assertNotNull(orderHistoryEvent, "Should generate ORDER_STATUS_UPDATED event");

        Order orderAfter = orderHistoryEvent.getOrderAfter();
        assertEquals("25-10-2021",
                DATE_FORMATTER.format(orderAfter.getProperty(OrderPropertyType.REAL_DELIVERY_DATE)));
        assertEquals("25-10-2021",
                DATE_FORMATTER.format(orderAfter.getDelivery().getRealDeliveryDate()));
    }

    @ParameterizedTest(name = "OrderStatus = {0}, realDeliveryDate = {1}, expectedRealDeliveryDate = {3}")
    @MethodSource("saveRealDeliveryDateTestData")
    public void shouldSaveRealDeliveryDate(OrderStatus status, String realDeliveryDate,
                                           ResultMatcher expectedRequestStatus,
                                           Date expectedRealDeliveryDate) throws Exception {
        freezeTimeAt("2021-10-25T18:00:00Z");
        Order order = createDBSOrderByDeliveredStatus(status);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        freezeTimeAt("2021-10-30T18:00:00Z");
        Order updatedOrder = updateOrderStatusDeliveredWithRealDeliveryDate(order, ClientRole.SHOP, status,
                realDeliveryDate,
                expectedRequestStatus);
        Order fetchedOrder = orderService.getOrder(order.getId());
        if (expectedRealDeliveryDate != null) {
            checkOrderRealDeliveryDate(updatedOrder, status, expectedRealDeliveryDate);
            checkOrderRealDeliveryDate(fetchedOrder, status, expectedRealDeliveryDate);
        } else {
            assertEquals(DELIVERY, fetchedOrder.getStatus());
            assertNull(fetchedOrder.getDelivery().getRealDeliveryDate());
            assertNull(fetchedOrder.getProperty(OrderPropertyType.REAL_DELIVERY_DATE));
        }

    }

    private void checkOrderRealDeliveryDate(Order order, OrderStatus status, Date expectedRealDeliveryDate)
            throws Exception {
        assertEquals(status, order.getStatus());
        assertEquals(expectedRealDeliveryDate, order.getProperty(OrderPropertyType.REAL_DELIVERY_DATE));
        assertEquals(expectedRealDeliveryDate, order.getDelivery().getRealDeliveryDate());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        OrderHistoryEvent orderHistoryEvent = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_STATUS_UPDATED)
                .filter(ohe -> status.equals(ohe.getOrderAfter().getStatus()))
                .findAny()
                .orElse(null);

        assertNotNull(orderHistoryEvent, "Should generate ORDER_STATUS_UPDATED event");

        Order orderAfter = orderHistoryEvent.getOrderAfter();
        assertNotNull(orderAfter.getProperty(OrderPropertyType.REAL_DELIVERY_DATE));
        assertEquals(expectedRealDeliveryDate, orderAfter.getProperty(OrderPropertyType.REAL_DELIVERY_DATE));
        assertNotNull(orderAfter.getDelivery().getRealDeliveryDate());
        assertEquals(expectedRealDeliveryDate, orderAfter.getDelivery().getRealDeliveryDate());
    }

    private Order createDBSOrder() {
        return createDBSOrderByDeliveredStatus(null);
    }

    private Order createDBSOrderByDeliveredStatus(OrderStatus deliveredStatus) {
        return createDBSOrderWithDeliveryType(
                PICKUP.equals(deliveredStatus) ? DeliveryType.PICKUP : DeliveryType.DELIVERY
        );
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private Order createDBSOrderWithDeliveryType(DeliveryType deliveryType) {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setShopId(DeliveryProvider.OUTLET_SHOP_ID);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setDeliveryType(deliveryType);
        switch (deliveryType) {
            case DELIVERY:
                parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
                parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                        .dates(DeliveryDates.deliveryDates(getClock(), 0, 1))
                        .buildResponse(DeliveryResponse::new));
                break;
            case PICKUP:
                parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());
                parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                        .dates(DeliveryDates.deliveryDates(getClock(), 0, 1))
                        .buildResponse(DeliveryResponse::new));
                break;
            case POST:
                parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
                parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPostDelivery()
                        .dates(DeliveryDates.deliveryDates(getClock(), 0, 1))
                        .buildResponse(DeliveryResponse::new));
                break;
        }
        return orderCreateHelper.createOrder(parameters);
    }

    private void updateOrderStatusDeliveredWithRealDeliveryDate(Order order,
                                                                ClientRole clientRole,
                                                                OrderStatus status,
                                                                ResultMatcher expectedStatus) {
        updateOrderStatusDeliveredWithRealDeliveryDate(order, clientRole, status, null, expectedStatus);
    }

    private MockHttpServletRequestBuilder requestBuilder(Order order,
                                                         ClientRole clientRole,
                                                         OrderStatus status,
                                                         String realDeliveryDate) {
        MockHttpServletRequestBuilder requestBuilder =
                post("/orders/{orderId}/status/delivered-with-real-delivery-date", order.getId())
                        .param(CheckouterClientParams.STATUS, String.valueOf(status))
                        .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(order.getShopId()))
                        .param(CheckouterClientParams.SHOP_ID, String.valueOf(order.getShopId()))
                        .param(CheckouterClientParams.BUSINESS_ID, String.valueOf(order.getBusinessId()));
        if (realDeliveryDate != null) {
            requestBuilder
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"realDeliveryDate\": \"" + realDeliveryDate + "\"}");
        }
        return requestBuilder;
    }

    private Order updateOrderStatusDeliveredWithRealDeliveryDate(Order order,
                                                                 ClientRole clientRole,
                                                                 OrderStatus status,
                                                                 String realDeliveryDate,
                                                                 ResultMatcher expectedStatus) {
        try {
            MockHttpServletRequestBuilder requestBuilder = requestBuilder(order, clientRole, status, realDeliveryDate);
            return testSerializationService.deserializeCheckouterObject(
                    mockMvc.perform(requestBuilder)
                            .andExpect(expectedStatus)
                            .andReturn().getResponse().getContentAsString(), Order.class);
        } catch (Exception e) {
            return fail("Setting DELIVERED or PICKUP status with saving real delivery date failed", e);
        }
    }
}
