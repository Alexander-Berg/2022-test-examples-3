package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;

import java.util.Arrays;
import java.util.List;

/**
 * Класс по созданию экземпляров класса MarschrouteItem для тестов.
 */
public class MarschrouteItems {

    public static List<MarschrouteItem> items(MarschrouteItem... items) {
        return Arrays.asList(items);
    }

    public static MarschrouteItem item(Integer price, Integer quantity) {
        MarschrouteItem marschrouteItem = new MarschrouteItem();
        marschrouteItem.setPrice(price);
        marschrouteItem.setQuantity(quantity);

        return marschrouteItem;
    }
}
