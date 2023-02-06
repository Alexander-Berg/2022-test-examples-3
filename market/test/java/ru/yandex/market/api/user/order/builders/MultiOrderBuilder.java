package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;

import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MultiOrderBuilder extends RandomBuilder<MultiOrder> {

    private MultiOrder order = new MultiOrder();

    @Override
    public MultiOrderBuilder random() {
        order.setPaymentMethod(random.from(PaymentMethod.class));
        order.setPaymentType(order.getPaymentMethod().getPaymentType());
        order.setBuyerRegionId((long) random.getInt(1000));
        order.setUid((long) random.getInt(10000000));
        return this;
    }

    public MultiOrderBuilder withOrders(Order... items) {
        order.setOrders(Lists.newArrayList(items));
        return this;
    }

    public MultiOrderBuilder withFailure(Order item, OrderFailure.Code code, String details) {
        order.setOrderFailures(Lists.newArrayList(new OrderFailure(item, code, details)));
        return this;
    }

    public MultiOrderBuilder withTotals(MultiCartTotals totals) {
        order.setTotals(totals);
        return this;
    }

    @Override
    public MultiOrder build() {
        return order;
    }
}
