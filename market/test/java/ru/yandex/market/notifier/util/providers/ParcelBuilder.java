package ru.yandex.market.notifier.util.providers;


import java.time.LocalDate;
import java.util.List;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.TariffType;

public class ParcelBuilder {

    private Parcel parcel;

    public ParcelBuilder() {
        parcel = new Parcel();
    }

    public static ParcelBuilder instance() {
        return new ParcelBuilder();
    }

    public ParcelBuilder withTracks(List<Track> tracks) {
        parcel.setTracks(tracks);
        return this;
    }

    public ParcelBuilder withParcelItems(List<ParcelItem> items) {
        parcel.setParcelItems(items);
        return this;
    }

    public ParcelBuilder withFromDate(LocalDate fromDate) {
        parcel.setFromDate(fromDate);
        return this;
    }

    public ParcelBuilder withToDate(LocalDate toDate) {
        parcel.setToDate(toDate);
        return this;
    }

    public ParcelBuilder withTariffType(TariffType tariffType) {
        parcel.setTariffType(tariffType);
        return this;
    }

    public ParcelBuilder withBoxes(List<ParcelBox> boxes) {
        parcel.setBoxes(boxes);
        return this;
    }

    public Parcel build() {
        return parcel;
    }

}
