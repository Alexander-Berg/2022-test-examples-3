package ru.yandex.market.checkout.checkouter.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.json.BooleanHolder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.dsbsOrderItem;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.shopDeliveryOrder;

public class EventOrderControllerTest extends AbstractWebTestBase {

    private static final Set<HistoryEventType> IMPORTANT_EVENT_TYPES = ImmutableSet.of(
            HistoryEventType.ITEMS_UPDATED, HistoryEventType.ORDER_DELIVERY_UPDATED
    );

    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private OrderPayHelper paymentHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    public static void checkNewOrderEvent(OrderHistoryEvent event) {
        assertThat(event.getType(), is(HistoryEventType.NEW_ORDER));
        assertThat(event.getAuthor().getRole(), is(ClientRole.USER));
        assertThat(event.getAuthor().getId(), is(BuyerProvider.UID));

        assertThat(event.getOrderBefore(), nullValue());

        assertThat(event.getOrderAfter(), notNullValue());
        assertThat(event.getOrderAfter().getStatus(), is(OrderStatus.PLACING));
    }

    public static void checkOrderStatusUpdated(OrderHistoryEvent event, OrderStatus before, OrderStatus after) {
        checkOrderStatusUpdated(event, before, after, ClientRole.SYSTEM);
    }

    private static void checkOrderStatusUpdated(OrderHistoryEvent event, OrderStatus before, OrderStatus after,
                                                ClientRole role) {
        assertThat(event.getType(), is(HistoryEventType.ORDER_STATUS_UPDATED));
        assertThat(event.getAuthor().getRole(), is(role));

        assertThat(event.getOrderBefore(), notNullValue());
        assertThat(event.getOrderBefore().getStatus(), is(before));

        assertThat(event.getOrderAfter(), notNullValue());
        assertThat(event.getOrderAfter().getStatus(), is(after));
    }

    private static void checkDeliveryExpiryStarted(OrderHistoryEvent event) {
        assertThat(event.getType(), is(HistoryEventType.DELIVERY_EXPIRY_STARTED));
    }

    /**
     * Проверяем события, генерирующиеся
     */
    @Test
    public void shouldCreateEventsWhenCreatingOrder() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        assertThat(events.getItems(), hasSize(3));

        List<OrderHistoryEvent> list = new ArrayList<>(events.getItems());
        Collections.reverse(list);

        checkNewOrderEvent(list.get(0));
        checkOrderStatusUpdated(list.get(1), OrderStatus.PLACING, OrderStatus.RESERVED);
        checkOrderStatusUpdated(list.get(2), OrderStatus.RESERVED, OrderStatus.UNPAID);
    }

    @Test
    public void shouldCreateEventsWhenCreatingPartnerInterfaceOrder() throws Exception {
        Order order = shopDeliveryOrder()
                .stubApi()
                .itemBuilder(dsbsOrderItem()
                        .offer("some-offer-1")
                        .price(1000))
                .itemBuilder(dsbsOrderItem()
                        .offer("some-offer-2")
                        .price(100))
                .itemBuilder(dsbsOrderItem()
                        .offer("some-offer-3")
                        .price(10))
                .build();

        var params = new Parameters(order);
        params.setColor(Color.WHITE);
        params.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        long orderId = orderCreateHelper.createOrder(params).getId();

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(orderId);

        assertThat(events.getItems(), hasSize(4));

        List<OrderHistoryEvent> list = events.getItems().stream()
                .filter(e -> e.getType() == HistoryEventType.ORDER_STATUS_UPDATED
                        || e.getType() == HistoryEventType.NEW_ORDER)
                .sorted(Comparator.comparing(OrderHistoryEvent::getId))
                .collect(Collectors.toUnmodifiableList());

        checkNewOrderEvent(list.get(0));
        checkOrderStatusUpdated(list.get(1), OrderStatus.PLACING, OrderStatus.RESERVED);
        checkOrderStatusUpdated(list.get(2), OrderStatus.RESERVED, OrderStatus.UNPAID);
    }

    @Test
    public void shouldCreateEventsWhenCreatePrepaidOrder() throws Exception {
        trustMockConfigurer.mockWholeTrust();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        paymentHelper.payForOrder(order);

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        assertThat(events.getItems(), hasSize(6));

        List<OrderHistoryEvent> list = new ArrayList<>(events.getItems());
        Collections.reverse(list);

        checkNewOrderEvent(list.get(0));
        checkOrderStatusUpdated(list.get(1), OrderStatus.PLACING, OrderStatus.RESERVED);
        checkOrderStatusUpdated(list.get(2), OrderStatus.RESERVED, OrderStatus.UNPAID);
        checkNewPayment(list.get(3));
        checkOrderStatusUpdated(list.get(4), OrderStatus.UNPAID, OrderStatus.PROCESSING);
        checkReceiptPrinted(list.get(5));
    }

    @Test
    public void shouldCreateEventWhenShopUpdatesOrderStatus() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderStatusHelper.updateOrderStatus(order.getId(), new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                OrderStatus.PROCESSING, null);

        List<OrderHistoryEvent> events = eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems()
                .stream()
                .limit(2)
                .collect(toList());
        //берем второе с конца событие для проверки, потому что последнее - добавление коробок в парсел
        checkOrderStatusUpdatedByShop(events.get(1), OrderStatus.PENDING, OrderStatus.PROCESSING);
    }

    @Test
    public void shouldCreateDeliveryUpdateEventWithCorrectAddress() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderDeliveryHelper.updateOrderDelivery(
                order.getId(),
                new ClientInfo(ClientRole.SHOP, order.getShopId()),
                buildDeliveryUpdate()
        );
        OrderHistoryEvent deliveryUpdateEvent = eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems().stream()
                .filter(event -> event.getType() == HistoryEventType.ORDER_DELIVERY_UPDATED)
                .findAny()
                .orElseThrow(() -> new RuntimeException("DELIVERY_UPDATED event not found"));
        checkDeliveryUpdateEventHasCorrectAddresses(deliveryUpdateEvent);
    }

    @Nonnull
    private Delivery buildDeliveryUpdate() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setOutletId(DeliveryProvider.FREE_MARKET_OUTLET_ID);
        return delivery;
    }

    @Test
    public void shouldShowOnlyImportantEvents() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderDeliveryHelper.updateOrderDelivery(
                order.getId(),
                new ClientInfo(ClientRole.SHOP, order.getShopId()),
                buildDeliveryUpdate()
        );

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, false, null
        );

        Assertions.assertTrue(events.getItems().stream().anyMatch(e -> !IMPORTANT_EVENT_TYPES.contains(e.getType())),
                "All events");


        PagedEvents importantEvents = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, true, null
        );

        Assertions.assertTrue(importantEvents.getItems().stream().allMatch(e ->
                IMPORTANT_EVENT_TYPES.contains(e.getType())), "Only important events");

        assertThat(events.getItems().size(), Matchers.greaterThanOrEqualTo(importantEvents.getItems().size()));
    }

    @Test
    public void shouldLimitEventsWhenPropertyIsOn() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderDeliveryHelper.updateOrderDelivery(
                order.getId(),
                new ClientInfo(ClientRole.SHOP, order.getShopId()),
                buildDeliveryUpdate()
        );
        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, false, null
        );

        assertThat(events.getItems().size(), Matchers.greaterThan(1));

        checkouterFeatureWriter.writeValue(IntegerFeatureType.LIMIT_ORDER_HISTORY_FETCHING_FROM_DB, 1);

        events = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, false, null
        );

        assertThat(events.getItems().size(), Matchers.equalTo(1));
        checkouterFeatureWriter.writeValue(IntegerFeatureType.LIMIT_ORDER_HISTORY_FETCHING_FROM_DB, -1);
    }

    @Test
    public void shouldShowOnlyImportantUnreadEvents() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderDeliveryHelper.updateOrderDelivery(
                order.getId(),
                new ClientInfo(ClientRole.SHOP, order.getShopId()),
                buildDeliveryUpdate()
        );

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, true, true
        );

        assertThat(events.getItems(), Matchers.not(Matchers.empty()));

        for (OrderHistoryEvent event : events.getItems()) {
            Long eventId = event.getId();
            mockMvc.perform(put("/orders/{orderId}/events/{eventId}/read-by-user", order.getId(), eventId)
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                    .param(CheckouterClientParams.CLIENT_ID, String.valueOf(order.getBuyer().getUid()))
                    .content(testSerializationService.serializeCheckouterObject(new BooleanHolder(true)))
                    .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk());
        }

        PagedEvents unreadEvents = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, true, true
        );

        assertThat(unreadEvents.getItems(), Matchers.empty());

        PagedEvents readEvents = eventsGetHelper.getOrderHistoryEvents(
                Collections.singleton(order.getId()), ClientInfo.SYSTEM, true, false
        );

        assertThat(readEvents.getItems(), Matchers.hasSize(events.getItems().size()));
    }

    private void checkDeliveryUpdateEventHasCorrectAddresses(OrderHistoryEvent deliveryUpdateEvent) {
        assertThat(deliveryUpdateEvent.getOrderBefore().getDelivery().getBuyerAddress(), notNullValue());
        assertThat(deliveryUpdateEvent.getOrderBefore().getDelivery().getShopAddress(), notNullValue());
    }

    private void checkNewPayment(OrderHistoryEvent event) {
        assertThat(event.getType(), is(HistoryEventType.NEW_PAYMENT));
        assertThat(event.getAuthor().getRole(), is(ClientRole.USER));
        assertThat(event.getAuthor().getId(), is(BuyerProvider.UID));

        assertThat(event.getOrderBefore().getPaymentId(), nullValue());
        assertThat(event.getOrderAfter().getPaymentId(), notNullValue());
    }

    private void checkOrderStatusUpdatedByShop(OrderHistoryEvent event, OrderStatus before, OrderStatus after) {
        checkOrderStatusUpdated(event, before, after, ClientRole.SHOP);
    }

    private void checkReceiptPrinted(OrderHistoryEvent event) {
        assertThat(event.getType(), is(HistoryEventType.RECEIPT_PRINTED));
        assertThat(event.getAuthor().getRole(), is(ClientRole.SYSTEM));

        assertThat(event.getReceipt().getId(), not(0));
        assertThat(event.getReceipt().getType(), is(ReceiptType.INCOME));
        assertThat(event.getReceipt().getStatus(), is(ReceiptStatus.PRINTED));
    }
}
