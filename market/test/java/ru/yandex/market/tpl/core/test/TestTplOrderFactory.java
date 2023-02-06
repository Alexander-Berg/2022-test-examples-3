package ru.yandex.market.tpl.core.test;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;


import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UtilityClass
public class TestTplOrderFactory {
    public static Order buildOrder(String externalOrderId, List<String> uits) {
        Order mockedOrder = mock(Order.class);
        when(mockedOrder.getExternalOrderId()).thenReturn(externalOrderId);
        List<OrderItem> orderItems = List.of(buildOrderItem(uits));
        when(mockedOrder.getItems()).thenReturn(orderItems);
        return mockedOrder;
    }

    public static OrderItem buildOrderItem(List<String> uits) {
        OrderItem mockedOrderItem = mock(OrderItem.class);
        Supplier<Stream<OrderItemInstance>> supplier = () -> uits == null ? null : uits.stream()
                .map(TestTplOrderFactory::buildOrderItemInstance);
        when(mockedOrderItem.streamInstances()).thenReturn(supplier.get(), supplier.get());
        return mockedOrderItem;
    }

    public static OrderItemInstance buildOrderItemInstance(String uit) {
        return OrderItemInstance
                .builder()
                .uit(uit)
                .build();
    }
}
