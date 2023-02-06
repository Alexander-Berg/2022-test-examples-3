package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import java.util.Collection;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderOptionsResponseBuilder extends RandomBuilder<OrderOptionsResponse> {

    OrderOptionsResponse response = new OrderOptionsResponse();

    @Override
    public OrderOptionsResponseBuilder random() {
        response.setPaymentMethods(Sets.newHashSet(random.from(PaymentMethod.class)));
        return this;
    }

    public OrderOptionsResponseBuilder withPaymentMethods(Collection<PaymentMethod> methods) {
        response.setPaymentMethods(Sets.newHashSet(methods));
        return this;
    }

    public OrderOptionsResponseBuilder withPaymentMethods(PaymentMethod ... methods) {
        response.setPaymentMethods(Sets.newHashSet(methods));
        return this;
    }

    public OrderOptionsResponseBuilder withShopOptions(OrderOptionsResponse.ShopOptions option) {
        response.setShops(Lists.newArrayList(option));
        return this;
    }

    public OrderOptionsResponseBuilder withShopOptions(Collection<OrderOptionsResponse.ShopOptions> options) {
        response.setShops(Lists.newArrayList(options));
        return this;
    }

    @Override
    public OrderOptionsResponse build() {
        return response;
    }
}
