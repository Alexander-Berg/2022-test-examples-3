package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PackagingBlock;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.WeightClass;

import static ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.PallettingIdConverter.toPalletingIds;

class MultiLayerTest {
    public static final PackagingBlock BLOCK = new PackagingBlock(
        toPalletingIds(1L, 2L),
        new Size(30, 20),
        40,
        1,
        1,
        WeightClass.LIGHT,
        null,
        null,
        1
    );
    private MultiLayer multiLayer;
    private Layer main;
    private RegionOnPallet additional1;
    private RegionOnPallet additional2;

    @BeforeEach
    void setUp() {
        main = Mockito.mock(Layer.class);
        additional1 = Mockito.mock(RegionOnPallet.class);
        additional2 = Mockito.mock(RegionOnPallet.class);
        multiLayer = new MultiLayer(main, List.of(additional1, additional2));
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(main, additional1, additional2);
    }

    @DisplayName("Добавление элемента в основной слой: не может быть добавлен")
    @Test
    void addFail() {
        Assertions.assertFalse(multiLayer.add(BLOCK));
    }

    @DisplayName("Добавление элемента в доп. слои")
    @Test
    void addToAdditional() {
        Mockito.when(additional1.addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false)))
            .thenReturn(true);
        Assertions.assertTrue(multiLayer.addToAdditional(BLOCK, false));
        Mockito.verify(additional1).addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false));
    }

    @DisplayName("Добавление элемента в доп. слои 2")
    @Test
    void addToAdditional2() {
        Mockito.when(additional1.addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false)))
            .thenReturn(false);
        Mockito.when(additional2.addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false)))
            .thenReturn(true);
        Assertions.assertTrue(multiLayer.addToAdditional(BLOCK, false));
        Mockito.verify(additional1).addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false));
        Mockito.verify(additional2).addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false));
    }

    @DisplayName("Добавление элемента в доп. слои: ошибка")
    @Test
    void addToAdditionalFail() {
        Mockito.when(additional1.addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false)))
            .thenReturn(false);
        Mockito.when(additional2.addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false)))
            .thenReturn(false);
        Assertions.assertFalse(multiLayer.addToAdditional(BLOCK, false));
        Mockito.verify(additional1).addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false));
        Mockito.verify(additional2).addCell(Mockito.eq(BLOCK), Mockito.eq(true), Mockito.eq(false), Mockito.eq(false));
    }

    @DisplayName("Дефрагментация всех слоёв")
    @Test
    void defrag() {
        multiLayer.defrag();
        Mockito.verify(main).defrag();
        Mockito.verify(additional1).defrag();
        Mockito.verify(additional2).defrag();
    }

    @DisplayName("Получене высоты (только собственной)")
    @Test
    void getHeight() {
        final int height = 10;
        Mockito.when(main.getHeight()).thenReturn(height);
        Assertions.assertEquals(height, multiLayer.getHeight());
        Mockito.verify(main).getHeight();
    }

    @DisplayName("Получене высоты низа слоя над уровнем паллеты (только собственной)")
    @Test
    void getFloorHeight() {
        final int height = 0;
        Mockito.when(main.getFloorHeight()).thenReturn(height);
        Assertions.assertEquals(height, multiLayer.getFloorHeight());
        Mockito.verify(main).getFloorHeight();
    }

    @DisplayName("Пуст ли слой (без доп. слоёв)")
    @Test
    void isEmpty() {
        final boolean empty = true;
        Mockito.when(main.isEmpty()).thenReturn(empty);
        Mockito.when(additional1.isEmpty()).thenReturn(empty);
        Mockito.when(additional2.isEmpty()).thenReturn(empty);
        Assertions.assertEquals(empty, multiLayer.isEmpty());
        Mockito.verify(main).isEmpty();
        Mockito.verify(additional1).isEmpty();
        Mockito.verify(additional2).isEmpty();
    }

    @DisplayName("Является ли основной слой верхним")
    @Test
    void isTop() {
        final boolean top = true;
        Mockito.when(main.isTop()).thenReturn(top);
        Assertions.assertEquals(top, multiLayer.isTop());
        Mockito.verify(main).isTop();
    }

    @DisplayName("Сделать главный слой верхним")
    @Test
    void setTop() {
        final boolean top = true;
        multiLayer.setTop(top);
        Mockito.verify(main).setTop(Mockito.eq(top));
    }

    @DisplayName("Получене площади (только собственной)")
    @Test
    void area() {
        final int area = 12;
        Mockito.when(main.area()).thenReturn(area);
        Assertions.assertEquals(area, multiLayer.area());
        Mockito.verify(main).area();
    }

    @DisplayName("Получене размеров (только собственных)")
    @Test
    void getSize() {
        final Size size = new Size(10, 12);
        Mockito.when(main.getSize()).thenReturn(size);
        Assertions.assertEquals(size, multiLayer.getSize());
        Mockito.verify(main).getSize();
    }

    @DisplayName("Создать доп. слои невозможно - уже созданы")
    @Test
    void createAdditionalLayers() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> multiLayer.createAdditionalLayers("1")
        );
    }

    @DisplayName("id всех товаров")
    @Test
    void getItemIds() {
        Mockito.when(main.getAllItemIds()).thenReturn(toPalletingIds(1L, 2L));
        Mockito.when(additional1.getItemIds()).thenReturn(toPalletingIds(3L, 4L));
        Mockito.when(additional2.getItemIds()).thenReturn(toPalletingIds(2L, 5L));
        Assertions.assertEquals(
            toPalletingIds(1L, 2L, 2L, 3L, 4L, 5L),
            multiLayer.getAllItemIds().stream().sorted().collect(Collectors.toList())
        );

        Mockito.verify(main).getAllItemIds();
        Mockito.verify(additional1).getItemIds();
        Mockito.verify(additional2).getItemIds();
    }
}
