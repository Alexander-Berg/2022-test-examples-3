package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.PostDeliveryId;

/**
 * Класс по созданию экземпляров класса MarschrouteOrder для тестов.
 */
public class MarschrouteOrders {

    public static MarschrouteOrder order() {
        return order(null, null, null, null);
    }

    public static MarschrouteOrder order(String pickupPointCode) {
        return order(null, null, null, pickupPointCode);
    }

    public static MarschrouteOrder order(MarschrouteLocation location,
                                         MarschroutePaymentType paymentType,
                                         Integer weight,
                                         String pickupPointCode,
                                         String deliveryServiceId) {
        MarschrouteOrder order = order(location, paymentType, weight, pickupPointCode);
        order.setDeliveryServiceId(deliveryServiceId);
        return order;
    }

    public static MarschrouteOrder order(MarschrouteLocation location,
                                         MarschroutePaymentType paymentType,
                                         Integer weight,
                                         String pickupPointCode) {
        MarschrouteOrder order = new MarschrouteOrder();
        order.setLocation(location);
        order.setPaymentType(paymentType);
        order.setWeight(weight);
        order.setPickupPointCode(pickupPointCode);

        return order;
    }
}
