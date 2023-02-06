package ru.yandex.market.checkout.pushapi.shop.entity;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;

import java.util.List;

public class ExternalCart extends Cart {
    private DeliveryWithRegion deliveryWithRegion;

    public ExternalCart() {
    }

    public ExternalCart(DeliveryWithRegion deliveryWithRegion, Currency currency, List<OrderItem> items) {
        super(null, currency, items);
        this.deliveryWithRegion = deliveryWithRegion;
    }

    public DeliveryWithRegion getDeliveryWithRegion() {
        return deliveryWithRegion;
    }

    public void setDeliveryWithRegion(DeliveryWithRegion deliveryWithRegion) {
        this.deliveryWithRegion = deliveryWithRegion;
    }

    @Override
    public String toString() {
        return "ExternalCart{" +
            "deliveryWithRegion=" + deliveryWithRegion +
            "} " + super.toString();
    }
}
