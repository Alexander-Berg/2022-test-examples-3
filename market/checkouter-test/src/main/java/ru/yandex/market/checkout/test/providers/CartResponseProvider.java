package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.util.Collections;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

public abstract class CartResponseProvider {

    public static CartResponse getCartResponse(Delivery delivery) {
        DeliveryResponse deliveryResponse = new DeliveryResponse();
        deliveryResponse.setType(DeliveryType.PICKUP);
        deliveryResponse.setDeliveryServiceId(delivery.getDeliveryServiceId());
        deliveryResponse.setDeliveryPartnerType(delivery.getDeliveryPartnerType());
        deliveryResponse.setPrice(new BigDecimal("1.11"));
        deliveryResponse.setServiceName(delivery.getServiceName());
        deliveryResponse.setOutletIds(Collections.singleton(111L));
        deliveryResponse.setOutletCodes(Collections.singleton("111"));
        deliveryResponse.setDeliveryDates(delivery.getDeliveryDates());

        CartResponse order = new CartResponse();
        order.setDeliveryCurrency(Currency.RUR);
        order.setItems(Collections.singletonList(OrderItemProvider.pushApiOrderItem()));
        order.setDeliveryOptions(Collections.singletonList(deliveryResponse));
        return order;
    }
}
