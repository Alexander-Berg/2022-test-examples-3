package ru.yandex.market.notifier.util.providers;

import com.google.common.collect.Lists;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;

public abstract class ParcelProvider {

    private ParcelProvider() {
        throw new UnsupportedOperationException();
    }

    public static Parcel createParcelWithTracks(Track... tracks) {
        Parcel shipment = new Parcel();
        shipment.setTracks(Lists.newArrayList(tracks));
        return shipment;
    }
}
