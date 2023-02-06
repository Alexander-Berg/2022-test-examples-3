package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.metrics.total;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.ModelParameters;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.Pallet;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.PalletDefaults;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PackagingBlock;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.WeightClass;

import static ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.PallettingIdConverter.toPalletingIds;

class VolumeUtilizationMetricTest {
    public static final ModelParameters MODEL_PARAMETERS =
        new ModelParameters(ModelParameters.SplitDirection.SHORT_SIDE_FIRST, true, 0, 0, true, 1D);

    @DisplayName("Две пустые паллеты")
    @Test
    void empty() {
        final Pallet p1 = new Pallet("1", PalletDefaults.SIZE, MODEL_PARAMETERS);
        final Pallet p2 = new Pallet("2", PalletDefaults.SIZE, MODEL_PARAMETERS);
        final Double utilization = new VolumeUtilizationMetric().compute(List.of(p1, p2));

        Assertions.assertEquals(0D, utilization);
    }

    @DisplayName("КГТ")
    @Test
    void oversize() {
        final Pallet p1 = new Pallet("1", PalletDefaults.SIZE, MODEL_PARAMETERS);
        p1.addCell(block(PalletDefaults.SIZE, 200), true, false, false);

        final Double utilization = new VolumeUtilizationMetric().compute(List.of(p1));

        Assertions.assertEquals(1D, utilization);
    }

    @DisplayName("50%")
    @Test
    void utilization50p() {
        final Pallet p1 = new Pallet("1", PalletDefaults.SIZE, MODEL_PARAMETERS);
        p1.addCell(block(PalletDefaults.SIZE, 100), true, false, false);

        final Pallet p2 = new Pallet("2", PalletDefaults.SIZE, MODEL_PARAMETERS);
        p2.addCell(block(new Size(60, 80), 70), true, false, false);
        p2.addCell(block(new Size(60, 80), 90), true, false, false);

        final Double utilization = new VolumeUtilizationMetric().compute(List.of(p1, p2));

        Assertions.assertEquals(0.5, utilization);
    }

    private PackagingBlock block(Size size, int height) {
        return new PackagingBlock(
            toPalletingIds(1L),
            size,
            height,
            100,
            100,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );
    }
}
