package ru.yandex.market.checkout.pushapi.shop.entity;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;

public class ShopOrder extends Order {
    private DeliveryWithRegion deliveryWithRegion;

    public ShopOrder() {
    }

    public DeliveryWithRegion getDeliveryWithRegion() {
        return deliveryWithRegion;
    }

    public void setDeliveryWithRegion(DeliveryWithRegion deliveryWithRegion) {
        this.deliveryWithRegion = deliveryWithRegion;
    }

    @Override
    public String toString() {
        return "ShopOrder{" +
            "deliveryWithRegion=" + deliveryWithRegion +
            "} " + super.toString();
    }
}
