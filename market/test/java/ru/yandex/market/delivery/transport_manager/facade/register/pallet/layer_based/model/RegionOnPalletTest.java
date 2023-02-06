package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Position;

public class RegionOnPalletTest extends LayerSetTest<RegionOnPallet> {
    @Override
    void newLayer() {
        final int height = 100;
        final RegionOnPallet regionOnPallet = create();
        Assertions.assertEquals(
            Collections.emptyList(),
            regionOnPallet.layers
        );
        regionOnPallet.newLayer(null, height, false, true);
        Assertions.assertEquals(
            List.of(new SimpleLayer(MODEL_PARAMETERS, null, PALLET_SIZE, height, false)),
            regionOnPallet.layers
        );
    }

    @Override
    RegionOnPallet create() {
        return new RegionOnPallet("100", null, new Position(1, 1), PALLET_SIZE, MODEL_PARAMETERS);
    }
}
