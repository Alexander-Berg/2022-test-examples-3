package ru.yandex.market.checkout.checkouter.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

/**
 * @author mmetlov
 */

public class EventsControllerOrdersDecorationTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService serializationService;

    private Order order;

    public static Stream<Arguments> parameterizedTestData() {

        return EventsTestUtils.parameters(Color.BLUE).stream().map(Arguments::of);
    }

    @BeforeEach
    public void init() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        order = orderCreateHelper.createOrder(parameters);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void testDecoration(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        List<OrderHistoryEvent> eventsList = new ArrayList<>(events);

        assertThat(eventsList, contains(
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(NEW_ORDER))
        ));

        OrderHistoryEvent newOrderEvent = eventsList.get(2);
        OrderHistoryEvent reservedEvent = eventsList.get(1);
        OrderHistoryEvent processingEvent = eventsList.get(0);

        assertNull(newOrderEvent.getOrderAfter().getDelivery().getShopAddress());
        assertNull(reservedEvent.getOrderAfter().getDelivery().getShopAddress());
        assertNull(reservedEvent.getOrderBefore().getDelivery().getShopAddress());
        assertNull(processingEvent.getOrderAfter().getDelivery().getShopAddress());
        assertNull(processingEvent.getOrderBefore().getDelivery().getShopAddress());
        assertNotNull(newOrderEvent.getOrderAfter().getDelivery().getOutlet());
        assertNotNull(reservedEvent.getOrderAfter().getDelivery().getOutlet());
        assertNotNull(reservedEvent.getOrderBefore().getDelivery().getOutlet());
        assertNotNull(processingEvent.getOrderAfter().getDelivery().getOutlet());
        assertNotNull(processingEvent.getOrderBefore().getDelivery().getOutlet());
    }
}
