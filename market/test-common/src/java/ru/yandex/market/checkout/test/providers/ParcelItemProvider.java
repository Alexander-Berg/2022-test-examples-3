package ru.yandex.market.checkout.test.providers;

import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;

public abstract class ParcelItemProvider {
    public static ParcelItem buildParcelItem(long itemId, int count) {
        return new ParcelItem(itemId, count);
    }
}
