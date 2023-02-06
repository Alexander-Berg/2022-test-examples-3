package ru.yandex.market.checkout.test.providers;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.TariffType;
import ru.yandex.market.checkout.test.builders.ParcelBuilder;

public abstract class ParcelProvider {

    private ParcelProvider() {
        throw new UnsupportedOperationException();
    }

    public static Parcel createParcel(ParcelItem... parcelItems) {
        return createParcel(Arrays.asList(parcelItems));
    }

    public static Parcel createParcel(List<ParcelItem> parcelItems) {
        return ParcelBuilder.instance()
                .withParcelItems(parcelItems)
                .build();
    }

    public static Parcel createParcelWithTracksAndItems(
            long deliveryServiceId,
            String trackCode,
            ParcelItem... parcelItems
    ) {

        return ParcelBuilder.instance()
                .withTracks(Lists.newArrayList(TrackProvider.createTrack(trackCode, deliveryServiceId)))
                .withParcelItems(Lists.newArrayList(parcelItems))
                .build();
    }

    public static Parcel createParcelWithTracksAndItems(List<Track> tracks, List<ParcelItem> parcelItems) {
        return ParcelBuilder.instance()
                .withTracks(tracks)
                .withParcelItems(parcelItems)
                .build();
    }

    public static Parcel createParcelWithTracksAndItems(
            long deliveryServiceId,
            String trackCode,
            LocalDate fromDate,
            LocalDate toDate,
            TariffType tariffType,
            ParcelItem... parcelItems
    ) {
        return ParcelBuilder.instance()
                .withTracks(Lists.newArrayList(TrackProvider.createTrack(trackCode, deliveryServiceId)))
                .withParcelItems(Lists.newArrayList(parcelItems))
                .withFromDate(fromDate)
                .withToDate(toDate)
                .withTariffType(tariffType)
                .build();
    }

    public static Parcel createParcelWithItems(ParcelItem... items) {
        Parcel parcel = new Parcel();
        parcel.setParcelItems(Lists.newArrayList(items));
        return parcel;
    }

    public static Parcel createParcelWithIdAndItems(long parcelId, List<ParcelItem> items) {
        Parcel parcel = new Parcel();
        parcel.setId(parcelId);
        parcel.setParcelItems(items);
        return parcel;
    }

    public static Parcel createParcelWithId(long shipmentId) {
        Parcel shipment = new Parcel();
        shipment.setId(shipmentId);
        return shipment;
    }

    public static Parcel createParcelWithIdAndStatus(long shipmentId, ParcelStatus status) {
        Parcel shipment = new Parcel();
        shipment.setId(shipmentId);
        shipment.setStatus(status);
        return shipment;
    }

    public static Parcel createParcelWithProperties(
            Long width,
            Long height,
            Long depth,
            Long weight
    ) {
        Parcel shipment = new Parcel();
        shipment.setWidth(width);
        shipment.setHeight(height);
        shipment.setDepth(depth);
        shipment.setWeight(weight);
        return shipment;
    }

    public static Parcel createParcelWithIdAndProperties(
            long shipmentId,
            long width,
            long height,
            long depth,
            long weight
    ) {
        Parcel shipment = new Parcel();
        shipment.setId(shipmentId);
        shipment.setWidth(width);
        shipment.setHeight(height);
        shipment.setDepth(depth);
        shipment.setWeight(weight);
        return shipment;
    }

    public static Parcel createParcelWithIdAndItems(
            long shipmentId,
            long itemId,
            int count
    ) {
        return createParcelWithIdAndItems(shipmentId, Lists.newArrayList(
                new ParcelItem(itemId, count)
        ));
    }

    public static Parcel createParcelWithIdAndTracks(
            long shipmentId,
            Track... tracks
    ) {
        Parcel shipment = new Parcel();
        shipment.setId(shipmentId);
        shipment.setTracks(Lists.newArrayList(tracks));
        return shipment;
    }

    public static Parcel createParcelWithIdAndTracks(
            long shipmentId,
            List<Track> tracks
    ) {
        Parcel shipment = new Parcel();
        shipment.setId(shipmentId);
        shipment.setTracks(tracks);
        return shipment;
    }

    public static Parcel createParcelWithTracks(Track... tracks) {
        Parcel shipment = new Parcel();
        shipment.setTracks(Lists.newArrayList(tracks));
        return shipment;
    }

    public static Parcel createParcelWithTracks(
            long deliveryServiceId,
            String trackCode
    ) {
        return createParcelWithTracks(TrackProvider.createTrack(trackCode, deliveryServiceId));
    }

    public static Parcel createParcelWithTracks(
            long deliveryServiceId,
            String trackCode, long trackerId
    ) {
        return createParcelWithTracks(TrackProvider.createTrack(trackCode, deliveryServiceId, trackerId));
    }
}
