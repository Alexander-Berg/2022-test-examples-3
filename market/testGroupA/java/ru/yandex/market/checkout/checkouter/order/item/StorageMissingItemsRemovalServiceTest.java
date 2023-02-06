package ru.yandex.market.checkout.checkouter.order.item;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SubstatusProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PLACING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.RESERVED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PACKAGING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_USER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.SubstatusProvider.anySubstatuses;
import static ru.yandex.market.checkout.checkouter.order.SubstatusProvider.onlySubstatuses;
import static ru.yandex.market.checkout.checkouter.order.SubstatusProvider.withoutSubstatuses;

class StorageMissingItemsRemovalServiceTest {

    private static final boolean AVAILABLE = true;
    private static final boolean FORBIDDEN = false;
    private MissingItemsRemovalService missingItemsRemovalService;

    private static Stream<Arguments> checkRemoveAvailableSource() {
        return Stream.of(
                Arguments.of(OrderType.FF, PLACING, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FF, RESERVED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FF, UNPAID, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FF, PROCESSING, anySubstatuses(), AVAILABLE),
                Arguments.of(OrderType.FF, DELIVERY, withoutSubstatuses(USER_RECEIVED), AVAILABLE),
                Arguments.of(OrderType.FF, PICKUP, withoutSubstatuses(PICKUP_USER_RECEIVED), AVAILABLE),
                Arguments.of(OrderType.FF, DELIVERED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FF, CANCELLED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FF, PENDING, anySubstatuses(), FORBIDDEN),

                Arguments.of(OrderType.FBS, PLACING, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FBS, RESERVED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FBS, UNPAID, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FBS, PROCESSING, onlySubstatuses(STARTED, PACKAGING), AVAILABLE),
                Arguments.of(OrderType.FBS, PROCESSING, withoutSubstatuses(STARTED, PACKAGING, SHIPPED), FORBIDDEN),
                Arguments.of(OrderType.FBS, DELIVERY, withoutSubstatuses(USER_RECEIVED), AVAILABLE),
                Arguments.of(OrderType.FBS, PICKUP, withoutSubstatuses(PICKUP_USER_RECEIVED), AVAILABLE),
                Arguments.of(OrderType.FBS, DELIVERED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FBS, CANCELLED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.FBS, PENDING, anySubstatuses(), FORBIDDEN),

                Arguments.of(OrderType.DBS, PLACING, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.DBS, RESERVED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.DBS, UNPAID, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.DBS, PROCESSING, anySubstatuses(), AVAILABLE),
                Arguments.of(OrderType.DBS, DELIVERY, anySubstatuses(), AVAILABLE),
                Arguments.of(OrderType.DBS, PICKUP, anySubstatuses(), AVAILABLE),
                Arguments.of(OrderType.DBS, DELIVERED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.DBS, CANCELLED, anySubstatuses(), FORBIDDEN),
                Arguments.of(OrderType.DBS, PENDING, anySubstatuses(), AVAILABLE)
        );
    }

    @BeforeEach
    void setUp() {
        OrderStatusGraph orderStatusGraph = Mockito.mock(OrderStatusGraph.class);
        OrderStatusGraph.OrderStatusCompareDecorator afterProcessing
                = Mockito.mock(OrderStatusGraph.OrderStatusCompareDecorator.class);

        OrderStatusGraph.OrderStatusCompareDecorator beforeProcessing
                = Mockito.mock(OrderStatusGraph.OrderStatusCompareDecorator.class);

        Mockito.when(afterProcessing.isAfter(PROCESSING)).thenReturn(true);
        Mockito.when(beforeProcessing.isAfter(PROCESSING)).thenReturn(false);

        Mockito.when(orderStatusGraph.compareStatus(DELIVERY)).thenReturn(afterProcessing);
        Mockito.when(orderStatusGraph.compareStatus(DELIVERED)).thenReturn(afterProcessing);
        Mockito.when(orderStatusGraph.compareStatus(PICKUP)).thenReturn(afterProcessing);
        Mockito.when(orderStatusGraph.compareStatus(DELIVERED)).thenReturn(afterProcessing);
        Mockito.when(orderStatusGraph.compareStatus(CANCELLED)).thenReturn(afterProcessing);
        Mockito.when(orderStatusGraph.compareStatus(PENDING)).thenReturn(afterProcessing);

        Mockito.when(orderStatusGraph.compareStatus(UNPAID)).thenReturn(beforeProcessing);
        Mockito.when(orderStatusGraph.compareStatus(RESERVED)).thenReturn(beforeProcessing);
        Mockito.when(orderStatusGraph.compareStatus(PLACING)).thenReturn(beforeProcessing);
        Mockito.when(orderStatusGraph.compareStatus(PROCESSING)).thenReturn(beforeProcessing);


        missingItemsRemovalService = new StorageMissingItemsRemovalService(
                null, null, null, null, null, null, orderStatusGraph, null);
    }

    @ParameterizedTest
    @MethodSource("checkRemoveAvailableSource")
    void checkRemoveAvailable(OrderType type, OrderStatus status, SubstatusProvider provider, boolean result) {
        for (OrderSubstatus substatus : provider.getSubstatusesFor(status)) {
            Order order = initOrder(type, status, substatus);
            System.out.println("OrderType: " + type);
            System.out.println("Status: " + status);
            System.out.println("Substatus: " + substatus);
            assertThat(missingItemsRemovalService.isOrderStateAvailableRemove(order)).isEqualTo(result);
        }
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private Order initOrder(OrderType orderType, OrderStatus status, OrderSubstatus substatus) {
        Order order = new Order();
        order.setId(100L);
        switch (orderType) {
            case FF:
                order.setFulfilment(true);
                order.setDelivery(initDeliveryWithType(DeliveryPartnerType.YANDEX_MARKET));
                break;
            case FBS:
                order.setFulfilment(false);
                order.setDelivery(initDeliveryWithType(DeliveryPartnerType.YANDEX_MARKET));
                break;
            case DBS:
                order.setFulfilment(false);
                order.setDelivery(initDeliveryWithType(DeliveryPartnerType.SHOP));
                break;
        }
        order.setStatus(status);
        order.setSubstatus(substatus);
        return order;
    }

    private Delivery initDeliveryWithType(DeliveryPartnerType type) {
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(type);
        return delivery;
    }

    private enum OrderType {
        FF, FBS, DBS
    }
}
