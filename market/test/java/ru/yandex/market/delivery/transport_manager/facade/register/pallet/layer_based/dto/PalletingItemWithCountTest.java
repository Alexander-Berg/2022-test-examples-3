package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;

public class PalletingItemWithCountTest {

    public static final PallettingId ID = new PallettingId(1, 1, CountType.FIT);

    @Test
    void testPutUprightNormedSize() {
        PalletingItemWithCount item = new PalletingItemWithCount(ID, 1000, 2, 3, 0.1, 1);
        Assertions.assertEquals(
            new PalletingItemWithCount(ID, 3, 2, 1000, 0.1, 1),
            item.putUpright()
        );
    }

    @Test
    void testPutUprightNotNormedSize() {
        PalletingItemWithCount item = new PalletingItemWithCount(ID, 2, 1000, 3, 0.1, 1);
        Assertions.assertEquals(
            new PalletingItemWithCount(ID, 3, 2, 1000, 0.1, 1),
            item.putUpright()
        );
    }
}
