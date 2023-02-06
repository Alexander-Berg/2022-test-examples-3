package ru.yandex.market.checkout.test.providers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;

public abstract class ShopOutletProvider {

    public static final List<Function<ShopOutlet, Object>> MINIFIED_PROPERTIES = Arrays.asList(
            ShopOutlet::getPhones,
            ShopOutlet::getBlock,
            ShopOutlet::getBuilding,
            ShopOutlet::getCity,
            ShopOutlet::getEstate,
            ShopOutlet::getPersonalAddressId,
            ShopOutlet::getGps,
            ShopOutlet::getPersonalGpsId,
            ShopOutlet::getHouse,
            ShopOutlet::getName,
            ShopOutlet::getNotes,
            ShopOutlet::getStreet
    );

    private ShopOutletProvider() {
    }

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
