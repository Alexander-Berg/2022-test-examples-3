package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PalletTest extends LayerSetTest<Pallet> {

    @Override
    @DisplayName("Создание нового слоя")
    @Test
    void newLayer() {
        final int height = 100;
        final Pallet pallet = create();

        Assertions.assertEquals(Collections.emptyList(), pallet.getLayers());
        pallet.newLayer(null, height, false, true);

        Assertions.assertEquals(
            List.of(
                new SimpleLayer(MODEL_PARAMETERS, null, PALLET_SIZE, height, true)
            ),
            pallet.getLayers()
        );
    }

    @DisplayName("Подсчёт высоты паллеты с учётом доп. областей, которые ниже основных слоёв")
    @Test
    void computeHeight() {
        final Pallet pallet = create();

        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);
        final Layer l3 = Mockito.mock(Layer.class);

        RegionOnPallet sub1 = Mockito.mock(RegionOnPallet.class);
        RegionOnPallet sub2 = Mockito.mock(RegionOnPallet.class);

        Mockito.when(l1.getHeight()).thenReturn(80);
        Mockito.when(l2.getHeight()).thenReturn(30);
        Mockito.when(l3.getHeight()).thenReturn(30);

        Mockito.when(sub1.computeHeight()).thenReturn(120);
        Mockito.when(sub2.computeHeight()).thenReturn(120);

        pallet.layers.add(new MultiLayer(l1, List.of(sub1, sub2)));
        pallet.layers.add(l2);
        pallet.layers.add(l3);

        Assertions.assertEquals(140, pallet.computeHeight());
    }

    @DisplayName("Подсчёт высоты паллеты с учётом доп. областей, которые выше основных слоёв")
    @Test
    void computeHeightSubLayersAreHigher() {
        final Pallet pallet = create();

        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);
        final Layer l3 = Mockito.mock(Layer.class);

        RegionOnPallet sub1 = Mockito.mock(RegionOnPallet.class);
        RegionOnPallet sub2 = Mockito.mock(RegionOnPallet.class);

        Mockito.when(l1.getHeight()).thenReturn(80);
        Mockito.when(l2.getHeight()).thenReturn(30);
        Mockito.when(l3.getHeight()).thenReturn(30);

        Mockito.when(sub1.computeHeight()).thenReturn(150);
        Mockito.when(sub2.computeHeight()).thenReturn(150);

        pallet.layers.add(new MultiLayer(l1, List.of(sub1, sub2)));
        pallet.layers.add(l2);
        pallet.layers.add(l3);

        Assertions.assertEquals(150, pallet.computeHeight());
    }

    @DisplayName("Создание доп. слоёв")
    @Test
    void createAdditionalLayers() {
        final Pallet pallet = create();
        pallet.layers.add(new SimpleLayer(MODEL_PARAMETERS, null, PALLET_SIZE, 100, false));
        Assertions.assertEquals(1, pallet.layers.size());

        pallet.createAdditionalLayers();

        Assertions.assertEquals(1, pallet.layers.size());
    }

    @Override
    Pallet create() {
        return new Pallet("1", PALLET_SIZE, MODEL_PARAMETERS);
    }
}
