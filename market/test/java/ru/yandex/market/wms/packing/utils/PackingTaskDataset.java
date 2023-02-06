package ru.yandex.market.wms.packing.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Value;

import ru.yandex.market.wms.packing.utils.Parcel.ParcelBuilder;

@Value
public final class PackingTaskDataset {
    List<Parcel> parcels;
    Set<String> stuckUits;

    public PackingTaskDataset(List<Parcel> parcels, Set<String> stuckUits) {
        this.parcels = parcels;
        this.stuckUits = stuckUits;
    }

    public PackingTaskDataset(List<Parcel> parcels) {
        this.parcels = parcels;
        this.stuckUits = Collections.emptySet();
    }

    public static PackingTaskDataset of(ParcelBuilder... parcelBuilders) {
        return new PackingTaskDataset(Arrays.stream(parcelBuilders)
                .map(ParcelBuilder::build)
                .collect(Collectors.toList()));
    }
}
