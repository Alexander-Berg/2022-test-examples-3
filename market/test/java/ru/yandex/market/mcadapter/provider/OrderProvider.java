package ru.yandex.market.mcadapter.provider;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.mcadapter.model.Order;
import ru.yandex.market.mcadapter.model.OrderItem;

/**
 * @author zagidullinri
 * @date 06.07.2022
 */
public class OrderProvider {
    public static String DEFAULT_MULTI_ORDER_ID = "7b0d8798-fd11-11ec-a20e-7351467f4018";

    public static Order getDefaultOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(111111L);
        order.setMultiOrderSize(1);
        order.setMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        order.setShopId(100500L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setSubstatus(OrderSubstatus.STARTED);
        order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        order.setPrice(BigDecimal.valueOf(2300));
        order.setCurrency(Currency.RUR);
        order.setCashbackAccrual(BigDecimal.valueOf(230));
        order.setDeliveryType(DeliveryType.DELIVERY);
        order.setDeliveryPrice(BigDecimal.valueOf(300));

        OrderItem orderItem = OrderItemProvider.getDefaultOrderItem();
        orderItem.setOrderId(order.getId());
        OrderItem orderItem2 = OrderItemProvider.getDefaultOrderItem();
        orderItem2.setId(2L);
        orderItem2.setOrderId(order.getId());
        order.setOrderItems(List.of(orderItem, orderItem2));

        return order;
    }
}
