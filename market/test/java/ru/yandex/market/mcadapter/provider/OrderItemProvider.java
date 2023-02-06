package ru.yandex.market.mcadapter.provider;

import java.math.BigDecimal;

import ru.yandex.market.mcadapter.model.OrderItem;

/**
 * @author zagidullinri
 * @date 06.07.2022
 */
public class OrderItemProvider {

    public static OrderItem getDefaultOrderItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(BigDecimal.TEN);
        orderItem.setCategoryId(1);
        orderItem.setPrice(BigDecimal.valueOf(100));
        orderItem.setCashbackAccrual(BigDecimal.valueOf(11.5));
        return orderItem;
    }
}
