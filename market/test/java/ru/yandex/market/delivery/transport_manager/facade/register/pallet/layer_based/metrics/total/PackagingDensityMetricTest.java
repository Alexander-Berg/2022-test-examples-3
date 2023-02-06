package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.metrics.total;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.Pallet;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.PalletDefaults;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.SimpleLayer;

class PackagingDensityMetricTest {
    Pallet p1;
    Pallet p2;

    @BeforeEach
    void setUp() {
        p1 = Mockito.mock(Pallet.class);
        p2 = Mockito.mock(Pallet.class);
    }

    @DisplayName("Пустая паллета")
    @Test
    void empty() {
        Mockito.when(p1.getTotalCellsVolume()).thenReturn(0);
        Mockito.when(p1.getLayers()).thenReturn(Collections.emptyList());
        Assertions.assertEquals(0, new PackagingDensityMetric().compute(List.of(p1)));
    }

    @DisplayName("Слои полностью заполнены")
    @Test
    void fullLayer() {
        final SimpleLayer layer = Mockito.mock(SimpleLayer.class);

        Mockito.when(p1.getTotalCellsVolume()).thenReturn(960_000);
        Mockito.when(p1.getLayers()).thenReturn(List.of(layer));

        Mockito.when(layer.getSize()).thenReturn(PalletDefaults.SIZE);
        Mockito.when(layer.getHeight()).thenReturn(100);

        Assertions.assertEquals(1, new PackagingDensityMetric().compute(List.of(p1)));
    }

    @DisplayName("Слои частично заполнены")
    @Test
    void partiallyFilledLayer() {
        final SimpleLayer layer1 = Mockito.mock(SimpleLayer.class);
        final SimpleLayer layer2 = Mockito.mock(SimpleLayer.class);

        Mockito.when(p1.getTotalCellsVolume()).thenReturn(768_000);
        Mockito.when(p1.getLayers()).thenReturn(List.of(layer1, layer2));

        Mockito.when(layer1.getSize()).thenReturn(PalletDefaults.SIZE);
        Mockito.when(layer1.getHeight()).thenReturn(100);

        Mockito.when(layer2.getSize()).thenReturn(PalletDefaults.SIZE);
        Mockito.when(layer2.getHeight()).thenReturn(60);

        Assertions.assertEquals(0.5, new PackagingDensityMetric().compute(List.of(p1)));
    }
}
