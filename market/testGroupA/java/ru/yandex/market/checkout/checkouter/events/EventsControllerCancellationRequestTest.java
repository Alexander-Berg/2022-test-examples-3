package ru.yandex.market.checkout.checkouter.events;

import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_PAYMENT;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CANCELLATION_REQUESTED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.PARCEL_CANCELLATION_REQUESTED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.RECEIPT_PRINTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper.findEvent;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class EventsControllerCancellationRequestTest extends AbstractEventsControllerTestBase {

    public static final String USER_CHANGED_MIND_TEXT = "Товар больше не нужен";
    private static final String NOTES = "notes";
    @Autowired
    private OrderStatusGraph orderStatusGraph;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    public static Stream<Arguments> parameterizedTestData() {
        return EventsTestUtils.parameters(Color.BLUE).stream().map(Arguments::of);
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void test(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        cancellationRequestHelper.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                new ClientInfo(ClientRole.USER, BuyerProvider.UID)
        );
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        assertThat(events, contains(
                hasProperty("type", is(ORDER_CHANGE_REQUEST_CREATED)),
                hasProperty("type", is(PARCEL_CANCELLATION_REQUESTED)),
                hasProperty("type", is(ORDER_CANCELLATION_REQUESTED)),
                hasProperty("type", is(RECEIPT_PRINTED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(NEW_PAYMENT)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(NEW_ORDER))
        ));

        OrderHistoryEvent event = findEvent(events, ORDER_CANCELLATION_REQUESTED);
        assertNull(event.getOrderBefore().getCancellationRequest());
        assertEquals(USER_CHANGED_MIND, event.getOrderAfter().getCancellationRequest().getSubstatus());
        assertEquals(USER_CHANGED_MIND_TEXT, event.getOrderAfter().getCancellationRequest().getSubstatusText());
        assertEquals(NOTES, event.getOrderAfter().getCancellationRequest().getNotes());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void testUnpaid(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        cancellationRequestHelper.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                new ClientInfo(ClientRole.USER, BuyerProvider.UID)
        );
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        assertThat(events, contains(
                hasProperty("type", is(ORDER_CHANGE_REQUEST_CREATED)),
                hasProperty("type", is(PARCEL_CANCELLATION_REQUESTED)),
                hasProperty("type", is(ORDER_CANCELLATION_REQUESTED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(NEW_ORDER))
        ));

        OrderHistoryEvent event = findEvent(events, ORDER_CANCELLATION_REQUESTED);
        assertNull(event.getOrderBefore().getCancellationRequest());
        assertEquals(cancellationRequest.getSubstatus(), event.getOrderAfter().getCancellationRequest().getSubstatus());
        assertEquals(USER_CHANGED_MIND_TEXT, event.getOrderAfter().getCancellationRequest().getSubstatusText());
        assertEquals(NOTES, event.getOrderAfter().getCancellationRequest().getNotes());
        OrderHistoryEvent statusUpdatedEvent = findEvent(events, ORDER_STATUS_UPDATED);
        assertEquals(
                orderStatusGraph.getSubstatusText(OrderSubstatus.WAITING_USER_INPUT),
                statusUpdatedEvent.getOrderBefore().getSubstatusText()
        );
        assertEquals(USER_CHANGED_MIND_TEXT, statusUpdatedEvent.getOrderAfter().getSubstatusText());

        OrderHistoryEvent parcelEvent = findEvent(events, PARCEL_CANCELLATION_REQUESTED);
        assertNull(parcelEvent.getOrderAfter().getCancellationRequest());
        assertEquals(
                OrderStatus.CANCELLED,
                parcelEvent.getOrderAfter().getStatus()
        );

    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void testProcessingByShop(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .withPartnerInterface(true)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        CancellationRequest cancellationRequest = new CancellationRequest(SHOP_FAILED, NOTES);
        cancellationRequestHelper.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                new ClientInfo(ClientRole.SHOP, SHOP_ID_WITH_SORTING_CENTER)
        );
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        assertThat(events, contains(
                hasProperty("type", is(ORDER_CHANGE_REQUEST_CREATED)),
                hasProperty("type", is(PARCEL_CANCELLATION_REQUESTED)),
                hasProperty("type", is(ORDER_CANCELLATION_REQUESTED)),
                hasProperty("type", is(RECEIPT_PRINTED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(NEW_PAYMENT)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(NEW_ORDER))
        ));

        OrderHistoryEvent event = findEvent(events, ORDER_CANCELLATION_REQUESTED);
        assertNull(event.getOrderBefore().getCancellationRequest());
        assertEquals(SHOP_FAILED, event.getOrderAfter().getCancellationRequest().getSubstatus());
        assertEquals(OrderStatus.PROCESSING, event.getOrderAfter().getStatus());

    }

}
