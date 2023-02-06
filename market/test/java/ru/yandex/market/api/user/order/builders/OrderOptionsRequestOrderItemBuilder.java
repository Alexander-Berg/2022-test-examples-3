package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;

import java.math.BigDecimal;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderOptionsRequestOrderItemBuilder extends RandomBuilder<OrderOptionsRequest.OrderItem> {

    OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequest.OrderItem();

    @Override
    public OrderOptionsRequestOrderItemBuilder random() {
        orderItem.setOfferId(new OfferId(random.getString(), null));
        orderItem.setCount(random.getInt(1, 1000));
        orderItem.setPrice(random.getPrice(1000, 0));
        return this;
    }

    /**
     * Задает offer id
     */
    public OrderOptionsRequestOrderItemBuilder withOfferId(OfferId offerId) {
        orderItem.setOfferId(offerId);
        return this;
    }

    public OrderOptionsRequestOrderItemBuilder withCount(int count) {
        orderItem.setCount(count);
        return this;
    }

    public OrderOptionsRequestOrderItemBuilder withPrice(BigDecimal price) {
        orderItem.setPrice(price);
        return this;
    }


    @Override
    public OrderOptionsRequest.OrderItem build() {
        return orderItem;
    }
}
