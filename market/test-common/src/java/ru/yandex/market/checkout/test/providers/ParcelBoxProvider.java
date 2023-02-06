package ru.yandex.market.checkout.test.providers;

import java.util.Arrays;
import java.util.List;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;

public final class ParcelBoxProvider {

    private ParcelBoxProvider() {
    }

    public static ParcelBox buildBox(ParcelBoxItem... items) {
        return buildBox(Arrays.asList(items));
    }

    public static ParcelBox buildBox(List<ParcelBoxItem> items) {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(items);
        return parcelBox;
    }
}
