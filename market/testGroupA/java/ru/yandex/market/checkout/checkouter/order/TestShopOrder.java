package ru.yandex.market.checkout.checkouter.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class TestShopOrder {

    public static final List<PaymentMethod> DEFAULT_ORDER_PAYMENT_METHODS = Arrays.asList(PaymentMethod.BANK_CARD);

    private final List<PaymentMethod> paymentMethods;
    private List<OrderItem> items;

    public TestShopOrder() {
        paymentMethods = DEFAULT_ORDER_PAYMENT_METHODS;
    }

    public TestShopOrder withItems(Collection<OrderItem> items) {
        this.items = new ArrayList<>(items);
        return this;
    }

    public CartResponse build() {
        CartResponse shopCart = new CartResponse();
        shopCart.setPaymentMethods(paymentMethods);
        if (items == null) {
            OrderItem item = new OrderItem();
            item.setCount(1);
            items.add(item);
        }
        shopCart.setItems(items);
        return shopCart;
    }

}
