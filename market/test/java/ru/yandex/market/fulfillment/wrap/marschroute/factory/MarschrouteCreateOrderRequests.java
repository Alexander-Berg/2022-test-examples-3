package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.order.MarschrouteCreateOrderRequest;

import java.util.List;

/**
 * Класс по созданию экземпляров класса MarschrouteCreateOrderRequest для тестов.
 */
public class MarschrouteCreateOrderRequests {

    public static MarschrouteCreateOrderRequest createOrderRequest() {
        return createOrderRequest(null, null);
    }

    public static MarschrouteCreateOrderRequest createOrderRequest(MarschrouteOrder order) {
        return createOrderRequest(order, null);
    }

    public static MarschrouteCreateOrderRequest createOrderRequest(List<MarschrouteItem> items) {
        return createOrderRequest(null, items);
    }

    public static MarschrouteCreateOrderRequest createOrderRequest(MarschrouteOrder order, List<MarschrouteItem> items) {
        MarschrouteCreateOrderRequest request = new MarschrouteCreateOrderRequest();
        request.setItems(items);
        request.setOrder(order);

        return request;
    }
}
