package ru.yandex.market.checkout.pushapi.client.entity;


import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import static java.util.Arrays.asList;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.buildList;

public class CartResponseBuilder implements Builder<CartResponse> {

    private CartResponse cartResponse;

    public CartResponseBuilder() {
        cartResponse = new CartResponse();
        cartResponse.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.SHOP_PREPAID));
    }

    public CartResponseBuilder setItems(OrderItemBuilder... orderItemBuilders) {
        if(orderItemBuilders != null) {
            cartResponse.setItems(buildList(orderItemBuilders));
        } else {
            cartResponse.setItems(null);
        }
        return this;
    }

    public CartResponseBuilder setPaymentMethods(PaymentMethod... paymentMethods) {
        if(paymentMethods != null) {
            cartResponse.setPaymentMethods(asList(paymentMethods));
        } else {
            cartResponse.setPaymentMethods(null);
        }
        return this;
    }

    public CartResponseBuilder setDeliveryOptions(DeliveryBuilder ... deliveryBuilders) {
        if(deliveryBuilders == null) {
            cartResponse.setDeliveryOptions(null);
        } else {
            cartResponse.setDeliveryOptions(buildList(deliveryBuilders));
        }
        return this;
    }

    public CartResponseBuilder setTaxSystem(TaxSystem taxSystem) {
        cartResponse.setTaxSystem(taxSystem);
        return this;
    }

    @Override
    public CartResponse build() {
        return cartResponse;
    }

}
