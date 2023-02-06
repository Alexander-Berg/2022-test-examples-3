package ru.yandex.market.checkout.helpers.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ParcelComparisonUtils {

    private ParcelComparisonUtils() {
    }

    public static void assertParcelListsSimilar(List<Parcel> createdParcels, List<Parcel> updatedParcels) {
        if (CollectionUtils.isEmpty(createdParcels) && CollectionUtils.isEmpty(updatedParcels)) {
            return;
        }
        assertFalse(CollectionUtils.isEmpty(createdParcels));
        assertFalse(CollectionUtils.isEmpty(updatedParcels));
        assertEquals(createdParcels.size(), updatedParcels.size());
        assertEquals(createdParcels.size(), updatedParcels.size(), "Amount of Parcels expects to be equal.");

        Map<Long, Parcel> updatedParcelsList = updatedParcels.stream()
                .collect(Collectors.toMap(Parcel::getId, p -> p));
        createdParcels.forEach(p -> assertParcelSimilar(p, updatedParcelsList.get(p.getId())));
    }

    public static void assertParcelSimilar(Parcel createdParcel, Parcel updatedParcel) {
        assertNotNull(createdParcel);
        assertNotNull(updatedParcel);
        if (Objects.isNull(createdParcel.getParcelItems())) {
            assertNull(updatedParcel.getParcelItems());
            return;
        }
        assertEquals(createdParcel.getParcelItems().size(), updatedParcel.getParcelItems().size());
        Map<Long, ParcelItem> updatedParcels = updatedParcel.getParcelItems().stream()
                .collect(Collectors.toMap(ParcelItem::getItemId, i -> i));
        createdParcel.getParcelItems()
                .forEach(pi -> assertParcelItemSimilar(pi, updatedParcels.get(pi.getItemId())));

    }

    public static void assertParcelItemSimilar(ParcelItem createdParcelItem, ParcelItem updatedPrcelItem) {
        assertNotNull(createdParcelItem);
        assertNotNull(updatedPrcelItem);
        assertEquals(createdParcelItem.getItemId(), updatedPrcelItem.getItemId());
        assertEquals(createdParcelItem.getParcelId(), updatedPrcelItem.getParcelId());
        assertEquals(createdParcelItem.getCount(), updatedPrcelItem.getCount());
    }
}
