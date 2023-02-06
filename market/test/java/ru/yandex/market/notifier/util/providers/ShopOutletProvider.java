package ru.yandex.market.notifier.util.providers;

import java.util.Collections;

import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;

public abstract class ShopOutletProvider {

    public static ShopOutlet getShopOutlet() {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setMinDeliveryDays(1);
        shopOutlet.setMinDeliveryDays(3);
        shopOutlet.setCost(100L);
        shopOutlet.setRegionId(213L);
        shopOutlet.setCity("Питер");
        shopOutlet.setStreet("Победы");
        shopOutlet.setHouse("13");
        shopOutlet.setBlock("666");
        shopOutlet.setPhones(Collections.singletonList(new ShopOutletPhone("7", "495", "2234562", "")));
        shopOutlet.setName("Аутлет");
        return shopOutlet;
    }
}
