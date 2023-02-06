package ru.yandex.market.providers;

import java.time.LocalDate;
import java.util.Arrays;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.shopadminstub.model.Delivery;
import ru.yandex.market.shopadminstub.model.DeliveryDates;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.OrderAcceptRequest;

public abstract class OrderAcceptRequestProvider {

    public static final String DEFAULT_ORDER_ID = "orderId";
    public static final PaymentMethod DEFAULT_PAYMENT_METHOD = PaymentMethod.CASH_ON_DELIVERY;
    public static final boolean DEFAULT_FULFILMENT = false;

    private OrderAcceptRequestProvider() {
        throw new UnsupportedOperationException();
    }

    public static OrderAcceptRequest buildOrderAcceptRequest() {
        return buildOrderAcceptRequest(ItemProvider.buildDefaultItem());
    }

    public static OrderAcceptRequest buildOrderAcceptRequest(Item... items) {
        OrderAcceptRequest orderAcceptRequest = new OrderAcceptRequest();

        orderAcceptRequest.setId(DEFAULT_ORDER_ID);
        orderAcceptRequest.setPaymentMethod(DEFAULT_PAYMENT_METHOD);
        orderAcceptRequest.setPaymentType(DEFAULT_PAYMENT_METHOD.getPaymentType());
        orderAcceptRequest.setFulfilment(DEFAULT_FULFILMENT);
        orderAcceptRequest.setItems(Arrays.asList(items));
        return orderAcceptRequest;
    }

    public static OrderAcceptRequest buildOrderAcceptRequest(DeliveryPartnerType deliveryPartnerType,
                                                             LocalDate deliveryFrom,
                                                             Item... items) {
        OrderAcceptRequest orderAcceptRequest = buildOrderAcceptRequest(items);
        orderAcceptRequest.setDelivery(new Delivery());
        orderAcceptRequest.getDelivery().setDeliveryPartnerType(deliveryPartnerType);
        orderAcceptRequest.getDelivery().setDeliveryDates(new DeliveryDates(deliveryFrom, deliveryFrom));
        return orderAcceptRequest;
    }
}
