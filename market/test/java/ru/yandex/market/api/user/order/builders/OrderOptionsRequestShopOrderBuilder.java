package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderOptionsRequestShopOrderBuilder extends RandomBuilder<OrderOptionsRequest.ShopOrder> {

    OrderOptionsRequest.ShopOrder shopOrder = new OrderOptionsRequest.ShopOrder();

    @Override
    public OrderOptionsRequestShopOrderBuilder random() {
        shopOrder.setShopId(random.getInt(1, 1000));
        return this;
    }

    public OrderOptionsRequestShopOrderBuilder withDelivery(OrderOptionsRequest.DeliveryPoint delivery) {
        shopOrder.setDeliveryPoint(delivery);
        return this;
    }

    public OrderOptionsRequestShopOrderBuilder addItem(OrderOptionsRequest.OrderItem orderItem) {
        shopOrder.setCartItems(orderItem);
        return this;
    }

    public OrderOptionsRequestShopOrderBuilder withShopId(int shopId) {
        shopOrder.setShopId(shopId);
        return this;
    }

    public OrderOptionsRequestShopOrderBuilder withLabel(String label) {
        shopOrder.setLabel(label);
        return this;
    }

    public OrderOptionsRequestShopOrderBuilder withRegionId(int regionId) {
        shopOrder.setRegionId(regionId);
        return this;
    }

    @Override
    public OrderOptionsRequest.ShopOrder build() {
        return shopOrder;
    }
}
