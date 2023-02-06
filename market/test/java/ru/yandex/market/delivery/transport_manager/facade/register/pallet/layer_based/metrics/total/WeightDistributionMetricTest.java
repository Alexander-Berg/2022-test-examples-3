package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.metrics.total;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.Pallet;

class WeightDistributionMetricTest {
    @Test
    void uniform() {
        final Pallet pallet = Mockito.mock(Pallet.class);
        Mockito.when(pallet.getWeight()).thenReturn(100D);
        final Double m = new WeightDistributionMetric().compute(List.of(
            pallet,
            pallet,
            pallet,
            pallet,
            pallet
        ));
        Assertions.assertEquals(1, m);
    }

    @Test
    void oneHeavyPallet() {
        final Pallet pallet1 = Mockito.mock(Pallet.class);
        final Pallet pallet0 = Mockito.mock(Pallet.class);
        Mockito.when(pallet1.getWeight()).thenReturn(500D);
        Mockito.when(pallet0.getWeight()).thenReturn(0D);
        final Double m = new WeightDistributionMetric().compute(List.of(
            pallet1,
            pallet0,
            pallet0,
            pallet0,
            pallet0
        ));
        Assertions.assertEquals(0, m);
    }
}
