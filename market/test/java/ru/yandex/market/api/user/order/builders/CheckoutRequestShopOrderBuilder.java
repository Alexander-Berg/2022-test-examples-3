package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;
import ru.yandex.market.api.user.order.checkout.DeliveryPoint;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class CheckoutRequestShopOrderBuilder extends RandomBuilder<CheckoutRequest.ShopOrder> {

    private CheckoutRequest.ShopOrder order = new CheckoutRequest.ShopOrder();

    @Override
    public CheckoutRequestShopOrderBuilder random() {
        order.setShopId(random.getInt(1000));
        order.setId(random.getInt(100000));
        order.setLabel(random.getString());
        return this;
    }

    public CheckoutRequestShopOrderBuilder withItem(CheckoutRequest.OrderItem item) {
        order.setItems(Lists.newArrayList(item));
        return this;
    }

    public CheckoutRequestShopOrderBuilder withItems(CheckoutRequest.OrderItem ... items) {
        order.setItems(Lists.newArrayList(items));
        return this;
    }

    public CheckoutRequestShopOrderBuilder withShopId(int value) {
        order.setShopId(value);
        return this;
    }

    public CheckoutRequestShopOrderBuilder withDeliveryPoint(DeliveryPoint value) {
        order.setDeliveryPoint(value);
        return this;
    }

    public CheckoutRequestShopOrderBuilder withLabel(String value) {
        order.setLabel(value);
        return this;
    }

    @Override
    public CheckoutRequest.ShopOrder build() {
        return order;
    }
}
