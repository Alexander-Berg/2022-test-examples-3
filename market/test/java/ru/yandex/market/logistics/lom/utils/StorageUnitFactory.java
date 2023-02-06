package ru.yandex.market.logistics.lom.utils;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.entity.embedded.Korobyte;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;

@UtilityClass
@ParametersAreNonnullByDefault
public class StorageUnitFactory {

    @Nonnull
    public StorageUnit getStorageUnit(@Nullable Integer weight, @Nullable Integer... placesWeight) {
        StorageUnit root = new StorageUnit()
            .setUnitType(StorageUnitType.ROOT)
            .setDimensions(createKorobyte(weight));

        if (placesWeight == null) {
            return root;
        }
        Arrays.stream(placesWeight).forEach(
            placeWeight -> root.addChild(
                new StorageUnit()
                    .setUnitType(StorageUnitType.PLACE)
                    .setDimensions(createKorobyte(placeWeight))
            )
        );
        return root;
    }

    @Nullable
    private Korobyte createKorobyte(@Nullable Integer scale) {
        if (scale == null) {
            return null;
        }
        return new Korobyte()
            .setWeightGross(BigDecimal.valueOf(scale))
            .setHeight(2 * scale)
            .setWidth(3 * scale)
            .setLength(4 * scale);
    }
}
