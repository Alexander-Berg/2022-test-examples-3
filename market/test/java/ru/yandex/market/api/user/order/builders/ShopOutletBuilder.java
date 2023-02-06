package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.delivery.outlet.DayTimeRange;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;

import java.util.List;

public class ShopOutletBuilder extends RandomBuilder<ShopOutlet> {
    private ShopOutlet shopOutlet = new ShopOutlet();
    @Override
    public ShopOutletBuilder random() {
        shopOutlet.setId(random.getLong());
        return this;
    }

    public ShopOutletBuilder outletId(long outletId) {
        shopOutlet.setId(outletId);
        return this;
    }

    public ShopOutletBuilder phones(List<ShopOutletPhone> phones) {
        shopOutlet.setPhones(phones);
        return this;
    }

    @Override
    public ShopOutlet build() {
        return shopOutlet;
    }
}
