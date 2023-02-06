package ru.yandex.market.checkout.test.providers;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;

public abstract class ParcelBoxItemProvider {
    public static ParcelBoxItem parcelBoxItem(long itemId, int count) {
        ParcelBoxItem item = new ParcelBoxItem();
        item.setItemId(itemId);
        item.setCount(count);
        return item;
    }
}
