package ru.yandex.market.checkout.test.builders;


import java.time.LocalDate;
import java.util.List;

import org.assertj.core.util.Lists;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.TariffType;

public class ParcelBuilder {

    private final Parcel parcel;

    public ParcelBuilder() {
        parcel = new Parcel();
    }

    public ParcelBuilder(Parcel parcel) {
        this.parcel = parcel;
    }

    public static ParcelBuilder instance() {
        return new ParcelBuilder();
    }

    public ParcelBuilder withShopShipmentId(Long shopShipmentId) {
        parcel.setShopShipmentId(shopShipmentId);
        return this;
    }


    public ParcelBuilder withShipmentId(Long shopShipmentId) {
        parcel.setShipmentId(shopShipmentId);
        return this;
    }


    public ParcelBuilder withWeight(Long weight) {
        parcel.setWeight(weight);
        return this;
    }

    public ParcelBuilder withWidth(Long width) {
        parcel.setWidth(width);
        return this;
    }

    public ParcelBuilder withHeight(Long height) {
        parcel.setHeight(height);
        return this;
    }

    public ParcelBuilder withDepth(Long depth) {
        parcel.setDepth(depth);
        return this;
    }

    public ParcelBuilder withStatus(ParcelStatus status) {
        parcel.setStatus(status);
        return this;
    }

    public ParcelBuilder withLabelURL(String labelURL) {
        parcel.setLabelURL(labelURL);
        return this;
    }

    public ParcelBuilder withTracks(List<Track> tracks) {
        parcel.setTracks(tracks);
        return this;
    }

    public ParcelBuilder withParcelItems(ParcelItem... items) {
        return withParcelItems(Lists.newArrayList(items));
    }

    public ParcelBuilder withParcelItems(List<ParcelItem> items) {
        parcel.setParcelItems(items);
        return this;
    }

    public ParcelBuilder addParcelItem(ParcelItem item) {
        parcel.addParcelItem(item);
        return this;
    }

    public ParcelBuilder withId(Long id) {
        parcel.setId(id);
        return this;
    }

    public ParcelBuilder withOrderId(Long orderId) {
        parcel.setOrderId(orderId);
        return this;
    }

    public ParcelBuilder withDeliveryId(Long deliveryId) {
        parcel.setDeliveryId(deliveryId);
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

    public ParcelBuilder withBoxes(ParcelBox... box) {
        return withBoxes(Lists.newArrayList(box));
    }

    public ParcelBuilder withBoxes(List<ParcelBox> boxes) {
        parcel.setBoxes(boxes);
        return this;
    }

    public Parcel build() {
        return parcel;
    }

}
