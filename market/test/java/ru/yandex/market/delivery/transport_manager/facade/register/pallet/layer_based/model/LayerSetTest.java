package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.ModelParameters;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PallettingId;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Cell;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PackagingBlock;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Position;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Rect;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.WeightClass;

import static ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.PallettingIdConverter.toPalletingIds;

public abstract class LayerSetTest<T extends LayerSet> {
    public static final ModelParameters MODEL_PARAMETERS =
        new ModelParameters(ModelParameters.SplitDirection.SHORT_SIDE_FIRST, true, 0, 0, true, 1D);
    public static final Size PALLET_SIZE = new Size(10, 5);
    public static final PallettingId ITEM_ID_1 = new PallettingId(1L, 0, CountType.FIT);
    public static final PallettingId ITEM_ID_2 = new PallettingId(2L, 0, CountType.FIT);
    public static final PallettingId ITEM_ID_3 = new PallettingId(3L, 0, CountType.FIT);

    @DisplayName("Создание нового слоя")
    @Test
    abstract void newLayer();

    @DisplayName("Добавить коробку без добавления слоя на пустую паллету")
    @Test
    void addCellToEmptyNoNew() {
        final T pallet = create();
        final PackagingBlock block =
            new PackagingBlock(toPalletingIds(1L, 2L), PALLET_SIZE, 2, 1, 1, WeightClass.LIGHT, null, null, 1);
        Assertions.assertFalse(pallet.addCell(block, false, false, false));
    }

    @DisplayName("Добавить коробку без добавления слоя. Есть слои, но в них не получается добавить")
    @Test
    void addCellFailNoNew() {
        final T pallet = create();
        final Layer l1 = Mockito.mock(SimpleLayer.class);
        final Layer l2 = Mockito.mock(SimpleLayer.class);

        Mockito.when(l1.add(Mockito.any())).thenReturn(false);
        Mockito.when(l2.add(Mockito.any())).thenReturn(false);

        pallet.layers.add(l1);
        pallet.layers.add(l2);

        final PackagingBlock block =
            new PackagingBlock(toPalletingIds(1L, 2L), PALLET_SIZE, 2, 1, 1, WeightClass.LIGHT, null, null, 1);

        Assertions.assertFalse(pallet.addCell(block, false, false, false));
    }

    @DisplayName("Добавить коробку без добавления слоя.")
    @Test
    void addCellOkNoNew() {
        final T pallet = create();
        final SimpleLayer l1 = Mockito.mock(SimpleLayer.class);
        final SimpleLayer l2 = Mockito.mock(SimpleLayer.class);

        Mockito.when(l1.add(Mockito.any())).thenReturn(false);
        Mockito.when(l2.add(Mockito.any())).thenReturn(true);
        Mockito.when(l2.getSupportingSurfaceArea(Mockito.any())).thenAnswer((Answer<Integer>) invocation -> {
            Rect rect = invocation.getArgument(0);
            return rect.area();
        });

        pallet.layers.add(l1);
        pallet.layers.add(l2);

        final PackagingBlock block =
            new PackagingBlock(toPalletingIds(1L, 2L), PALLET_SIZE, 2, 1, 1, WeightClass.LIGHT, null, null, 1);

        Assertions.assertTrue(pallet.addCell(block, false, false, false));
    }

    @DisplayName("Добавить коробку с добавлением слоя.")
    @Test
    void addCellOkNewLayer() {
        final T pallet = create();
        final SimpleLayer l1 = Mockito.mock(SimpleLayer.class);
        final SimpleLayer l2 = Mockito.mock(SimpleLayer.class);

        Mockito.when(l1.add(Mockito.any())).thenReturn(false);
        Mockito.when(l2.add(Mockito.any())).thenReturn(false);
        Mockito.when(l2.getSupportingSurfaceArea(Mockito.any())).thenAnswer((Answer<Integer>) invocation -> {
            Rect rect = invocation.getArgument(0);
            return rect.area();
        });

        pallet.layers.add(l1);
        pallet.layers.add(l2);

        final PackagingBlock block =
            new PackagingBlock(toPalletingIds(1L, 2L), PALLET_SIZE, 2, 1, 1, WeightClass.LIGHT, null, null, 1);

        Assertions.assertTrue(pallet.addCell(block, true, false, false));
        Assertions.assertEquals(3, pallet.layers.size());
        Assertions.assertTrue(pallet.layers.get(2) instanceof SimpleLayer);
        Assertions.assertEquals(
            List.of(cell(toPalletingIds(1L, 2L), 1)),
            ((SimpleLayer) pallet.layers.get(2)).cells
        );
    }

    @DisplayName("Добавить коробку. Не проходит по высоте")
    @Test
    void addCellHeightFail() {
        final T pallet = create();
        final SimpleLayer l1 = Mockito.mock(SimpleLayer.class);
        final SimpleLayer l2 = Mockito.mock(SimpleLayer.class);

        Mockito.when(l1.getHeight()).thenReturn(100);
        Mockito.when(l2.getHeight()).thenReturn(80);

        Mockito.when(l1.add(Mockito.any())).thenReturn(false);
        Mockito.when(l2.add(Mockito.any())).thenReturn(false);
        Mockito.when(l2.getSupportingSurfaceArea(Mockito.any())).thenAnswer((Answer<Integer>) invocation -> {
            Rect rect = invocation.getArgument(0);
            return rect.area();
        });

        pallet.layers.add(l1);
        pallet.layers.add(l2);

        final PackagingBlock block =
            new PackagingBlock(toPalletingIds(1L, 2L), PALLET_SIZE, 2, 1, 1, WeightClass.LIGHT, null, null, 1);

        Assertions.assertFalse(pallet.addCell(block, true, false, false));
        Assertions.assertEquals(2, pallet.layers.size());
    }

    @DisplayName("Список коробок пустой паллеты")
    @Test
    void getCellsEmpty() {
        Assertions.assertEquals(Collections.emptyList(), create().getCells());
    }

    @DisplayName("Список коробок")
    @Test
    void getCells() {
        final T pallet = create();
        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);

        final Cell c1 = cell(toPalletingIds(1L, 2L), 1);
        final Cell c2 = cell(toPalletingIds(3L), 1);
        final Cell c3 = cell(toPalletingIds(4L, 5L), 1);

        Mockito.when(l1.getAllCells()).thenReturn(List.of(c1, c2));
        Mockito.when(l2.getAllCells()).thenReturn(List.of(c3));

        pallet.layers.add(l1);
        pallet.layers.add(l2);
        Assertions.assertEquals(List.of(c1, c2, c3), pallet.getCells());
    }

    @DisplayName("Вес пустой паллеты")
    @Test
    void getWeightEmpty() {
        Assertions.assertEquals(0D, create().getWeight());
    }

    @DisplayName("Список коробок")
    @Test
    void getWeight() {
        final T pallet = create();
        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);

        final Cell c1 = cell(toPalletingIds(1L, 2L), 1);
        final Cell c2 = cell(toPalletingIds(3L), 2);
        final Cell c3 = cell(toPalletingIds(4L, 5L), 3);

        Mockito.when(l1.getAllCells()).thenReturn(List.of(c1, c2));
        Mockito.when(l2.getAllCells()).thenReturn(List.of(c3));

        pallet.layers.add(l1);
        pallet.layers.add(l2);
        Assertions.assertEquals(6, pallet.getWeight());
    }

    @DisplayName("Список item id пустой паллеты")
    @Test
    void getItemIdsEmpty() {
        Assertions.assertEquals(Collections.emptyList(), create().getItemIds());
    }

    @DisplayName("Список item id")
    @Test
    void getItemIds() {
        final T pallet = create();
        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);

        Mockito.when(l1.getAllItemIds()).thenReturn(List.of(ITEM_ID_1, ITEM_ID_2));
        Mockito.when(l2.getAllItemIds()).thenReturn(List.of(ITEM_ID_3));

        pallet.layers.add(l1);
        pallet.layers.add(l2);
        Assertions.assertEquals(List.of(ITEM_ID_1, ITEM_ID_2, ITEM_ID_3), pallet.getItemIds());
    }

    @DisplayName("Проверка на пустоту: да")
    @Test
    void isEmptyTrue() {
        Assertions.assertTrue(create().isEmpty());
    }

    @DisplayName("Проверка на пустоту: нет")
    @Test
    void isEmptyFalse() {
        final T pallet = create();
        pallet.layers.add(Mockito.mock(Layer.class));
        Assertions.assertFalse(pallet.isEmpty());
    }

    @DisplayName("Сделать последний слой завершающим для пустой паллеты")
    @Test
    void setLastLayerTopEmpty() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> create().setLastLayerTop()
        );
    }

    @DisplayName("Сделать последний слой завершающим")
    @Test
    void setLastLayerTop() {
        final T pallet = create();
        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);
        pallet.layers.add(l1);
        pallet.layers.add(l2);
        pallet.setLastLayerTop();
        Mockito.verify(l2).setTop(true);
        Mockito.verifyNoMoreInteractions(l1, l2);
    }

    @DisplayName("Удаление пустых слоёв")
    @Test
    void removeEmptyLayers() {
        final T pallet = create();
        final Layer l1 = Mockito.mock(Layer.class);
        final Layer l2 = Mockito.mock(Layer.class);

        Mockito.when(l1.isEmpty()).thenReturn(false);
        Mockito.when(l2.isEmpty()).thenReturn(true);

        pallet.layers.add(l1);
        pallet.layers.add(l2);

        pallet.removeEmptyLayers();

        Assertions.assertEquals(List.of(l1), pallet.layers);
    }

    abstract T create();

    private Cell cell(List<PallettingId> itemIds, int weight) {
        return new Cell(new Rect(Position.ZERO, PALLET_SIZE), 2, weight, WeightClass.LIGHT, itemIds);
    }

}
