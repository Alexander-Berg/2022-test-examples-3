package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.yandex.market.api.user.order.DeliveryOption;
import ru.yandex.market.api.user.order.ShopOrderItem;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import java.util.Collection;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderOptionsResponseShopOptionsBuilder extends RandomBuilder<OrderOptionsResponse.ShopOptions> {

    OrderOptionsResponse.ShopOptions shopOptions = new OrderOptionsResponse.ShopOptions();

    @Override
    public OrderOptionsResponseShopOptionsBuilder random() {
        shopOptions.setShopId(random.getInt(1, 1000));
        shopOptions.setPaymentMethods(Sets.newHashSet(random.from(PaymentMethod.class)));
        return this;
    }

    public OrderOptionsResponseShopOptionsBuilder withShopId(long shopId) {
        shopOptions.setShopId(shopId);
        return this;
    }

    public OrderOptionsResponseShopOptionsBuilder withItems(ShopOrderItem item) {
        shopOptions.setItems(item);
        return this;
    }

    public OrderOptionsResponseShopOptionsBuilder withItems(Collection<ShopOrderItem> items) {
        shopOptions.setItems(Lists.newArrayList(items));
        return this;
    }

    public OrderOptionsResponseShopOptionsBuilder withPaymentMethods(PaymentMethod method) {
        shopOptions.setPaymentMethods(Sets.newHashSet(method));
        return this;
    }

    public OrderOptionsResponseShopOptionsBuilder withDelivery(DeliveryOption ... delivery) {
        shopOptions.setDeliveryOptions(delivery);
        return this;
    }

    @Override
    public OrderOptionsResponse.ShopOptions build() {
        return shopOptions;
    }
}
