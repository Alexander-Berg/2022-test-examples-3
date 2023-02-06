package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.ModelParameters;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Cell;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PackagingBlock;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PreviousCellPosition;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Rect;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.WeightClass;

import static ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.PallettingIdConverter.toPalletingIds;

class SimpleLayerTest {

    public static final Size LAYER_SIZE = new Size(100, 50);
    public static final ModelParameters MODEL_PARAMETERS =
        new ModelParameters(ModelParameters.SplitDirection.SHORT_SIDE_FIRST, true, 0, 0, true, 1D);
    public static final int HEIGHT = 20;
    private final SimpleLayer layer = new SimpleLayer(MODEL_PARAMETERS, null, LAYER_SIZE, HEIGHT, false);

    @DisplayName("Площадь опоры 0 - не пересекаются")
    @Test
    void getSupportingSurfaceArea0() {
        layer.cells.add(new Cell(new Rect(0, 0, 40, 20), HEIGHT, 1, WeightClass.MEDIUM, Collections.emptyList()));
        Assertions.assertEquals(0, layer.getSupportingSurfaceArea(new Rect(40, 20, 40, 20)));
    }


    @DisplayName("Площадь опоры - фигуры совпадают")
    @Test
    void getSupportingSurfaceArea() {
        layer.cells.add(new Cell(new Rect(40, 20, 40, 20), HEIGHT, 1, WeightClass.MEDIUM, Collections.emptyList()));
        Assertions.assertEquals(800, layer.getSupportingSurfaceArea(new Rect(40, 20, 40, 20)));
    }


    @DisplayName("Площадь опоры - частичное перекрытие")
    @Test
    void getSupportingSurfaceAreaPartialOverlap() {
        layer.cells.add(new Cell(new Rect(40, 20, 40, 20), HEIGHT, 1, WeightClass.MEDIUM, Collections.emptyList()));
        Assertions.assertEquals(400, layer.getSupportingSurfaceArea(new Rect(40, 20, 20, 40)));
    }

    @DisplayName("Дефрагментация: удаление слишком маленьких свободных мест")
    @Test
    void removeTooSmallFreeRects() {
        final Rect bigFreeRect = new Rect(0, 10, 100, 50);
        final Rect smallFreeRect = new Rect(98, 0, Layer.HORISONTAL_UNIT_SIZE, Layer.HORISONTAL_UNIT_SIZE);

        layer.freeRects.clear();
        layer.freeRects.add(bigFreeRect);
        layer.freeRects.add(smallFreeRect);

        layer.removeTooSmallFreeRects();

        Assertions.assertEquals(List.of(bigFreeRect), layer.freeRects);
    }

    @DisplayName("Дефрагментация: склейка областей по вертикали")
    @Test
    void concatFreeRectVertically() {
        layer.freeRects.clear();
        layer.freeRects.add(new Rect(70, 0, 30, 10));
        layer.freeRects.add(new Rect(70, 10, 30, 10));
        layer.freeRects.add(new Rect(70, 20, 30, 10));
        layer.freeRects.add(new Rect(0, 30, 100, 20));

        layer.defrag();

        Assertions.assertEquals(
            List.of(
                new Rect(0, 30, 100, 20),
                new Rect(70, 0, 30, 30)
            ),
            layer.freeRects
        );
    }


    @DisplayName("Дефрагментация: склейка областей по горизонтали")
    @Test
    void concatFreeRectHorisontal() {
        layer.freeRects.clear();
        layer.freeRects.add(new Rect(0, 40, 30, 10));
        layer.freeRects.add(new Rect(30, 40, 30, 10));
        layer.freeRects.add(new Rect(60, 40, 30, 10));
        layer.freeRects.add(new Rect(90, 0, 10, 50));

        layer.defrag();

        Assertions.assertEquals(
            List.of(
                new Rect(90, 0, 10, 50),
                new Rect(0, 40, 90, 10)
            ),
            layer.freeRects
        );
    }

    @DisplayName("Нет доступных областей 1")
    @Test
    void getAvailableLocationsAllBusy1() {
        layer.freeRects.clear();
        final PackagingBlock block = new PackagingBlock(
            Collections.emptyList(),
            new Size(10, 10),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );

        final List<PlacementLocation> availablePlacementLocations = layer
            .getAvailablePlacementLocations(block, false)
            .collect(Collectors.toList());

        Assertions.assertEquals(Collections.emptyList(), availablePlacementLocations);
    }

    @DisplayName("Нет доступных областей 2")
    @Test
    void getAvailableLocationsAllBusy2() {
        layer.freeRects.clear();
        layer.freeRects.add(new Rect(95, 0, 5, 45));
        layer.freeRects.add(new Rect(0, 45, 100, 5));

        final PackagingBlock block = new PackagingBlock(
            Collections.emptyList(),
            new Size(10, 10),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );

        final List<PlacementLocation> availablePlacementLocations = layer
            .getAvailablePlacementLocations(block, false)
            .collect(Collectors.toList());

        Assertions.assertEquals(Collections.emptyList(), availablePlacementLocations);
    }

    @DisplayName("Поиск доступных областей")
    @Test
    void getAvailableLocations() {
        layer.freeRects.clear();
        layer.freeRects.add(new Rect(90, 0, 10, 45));
        layer.freeRects.add(new Rect(0, 45, 100, 5));

        final PackagingBlock block = new PackagingBlock(
            Collections.emptyList(),
            new Size(10, 10),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );

        final List<Rect> availablePlacementLocations = layer
            .getAvailablePlacementLocations(block, false)
            .map(l -> l.rect)
            .collect(Collectors.toList());

        Assertions.assertEquals(
            List.of(new Rect(90, 0, 10, 45)),
            availablePlacementLocations
        );
    }

    @DisplayName("Поиск доступных областей - нет достаточной площади опоры")
    @Test
    void getAvailableLocationsNoSurface() {
        layer.cells.add(new Cell(
            new Rect(0, 0, 100, 5),
            HEIGHT,
            1,
            WeightClass.MEDIUM,
            Collections.emptyList()
        ));
        layer.freeRects.clear();

        SimpleLayer layer1 = new SimpleLayer(MODEL_PARAMETERS, layer, LAYER_SIZE, HEIGHT, false);
        layer1.freeRects.clear();
        layer1.freeRects.add(new Rect(90, 0, 10, 45));
        layer1.freeRects.add(new Rect(0, 45, 100, 5));

        final PackagingBlock block = new PackagingBlock(
            Collections.emptyList(),
            new Size(10, 10),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );

        final List<Rect> availablePlacementLocations = layer1
            .getAvailablePlacementLocations(block, false)
            .map(l -> l.rect)
            .collect(Collectors.toList());

        Assertions.assertEquals(
            Collections.emptyList(),
            availablePlacementLocations
        );
    }

    @DisplayName("Поиск доступных областей площадь опоры достаточна")
    @Test
    void getAvailableLocationsSurfaceOk() {
        layer.cells.add(new Cell(
            new Rect(0, 0, 100, 9),
            HEIGHT,
            1,
            WeightClass.MEDIUM,
            Collections.emptyList()
        ));
        layer.freeRects.clear();

        SimpleLayer layer1 = new SimpleLayer(MODEL_PARAMETERS, layer, LAYER_SIZE, HEIGHT, false);
        layer1.freeRects.clear();
        layer1.freeRects.add(new Rect(90, 0, 10, 45));
        layer1.freeRects.add(new Rect(0, 45, 100, 5));

        final PackagingBlock block = new PackagingBlock(
            Collections.emptyList(),
            new Size(10, 10),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );

        final List<Rect> availablePlacementLocations = layer1
            .getAvailablePlacementLocations(block, false)
            .map(l -> l.rect)
            .collect(Collectors.toList());

        Assertions.assertEquals(
            List.of(new Rect(90, 0, 10, 45)),
            availablePlacementLocations
        );
    }

    @DisplayName("Добавление блока больше размеров слоя")
    @Test
    void addTooBigBlock() {
        final boolean added = layer.add(new PackagingBlock(
            Collections.emptyList(),
            new Size(100, 100),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        ));
        Assertions.assertFalse(added);
        Assertions.assertEquals(Collections.emptyList(), layer.cells);
    }

    @DisplayName("Добавление блока в занятый слой")
    @Test
    void addNoPlace() {
        layer.freeRects.clear();

        final boolean added = layer.add(new PackagingBlock(
            Collections.emptyList(),
            new Size(100, 50),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        ));

        Assertions.assertFalse(added);
        Assertions.assertEquals(Collections.emptyList(), layer.cells);
    }

    @DisplayName("Добавление блока другой высоты")
    @Test
    void addDifferentHeight() {
        final boolean added = layer.add(new PackagingBlock(
            Collections.emptyList(),
            new Size(100, 50),
            2 * HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        ));

        Assertions.assertFalse(added);
        Assertions.assertEquals(Collections.emptyList(), layer.cells);
    }

    @DisplayName("Добавление блока")
    @Test
    void add() {
        final boolean added = layer.add(new PackagingBlock(
            toPalletingIds(1L),
            new Size(100, 50),
            HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        ));

        Assertions.assertTrue(added);
        Assertions.assertEquals(
            List.of(
                new Cell(new Rect(0, 0, 100, 50), HEIGHT, 1, WeightClass.MEDIUM, toPalletingIds(1L))
            ),
            layer.cells
        );
    }

    @DisplayName("Добавление блока другой высоты в верхний слой")
    @Test
    void addDifferentHeightBlockOnTop() {
        layer.setTop(true);
        final boolean added = layer.add(new PackagingBlock(
            toPalletingIds(1L),
            new Size(100, 50),
            2 * HEIGHT,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        ));

        Assertions.assertTrue(added);
        Assertions.assertEquals(
            List.of(
                new Cell(new Rect(0, 0, 100, 50), 2 * HEIGHT, 1, WeightClass.MEDIUM, toPalletingIds(1L))
            ),
            layer.cells
        );
    }

    @DisplayName("Добавление негабаритного блока")
    @Test
    void addTallBlockOnGroundLayer() {
        final SimpleLayer highLayer = new SimpleLayer(
            MODEL_PARAMETERS, null, LAYER_SIZE, HEIGHT, true
        );

        final boolean added = highLayer.add(new PackagingBlock(
            toPalletingIds(1L),
            new Size(100, 50),
            220, // Холодильник
            100,
            100,
            WeightClass.HEAVY,
            null,
            null,
            1
        ));

        Assertions.assertTrue(added);
        Assertions.assertEquals(
            List.of(
                new Cell(new Rect(0, 0, 100, 50), 220, 100, WeightClass.HEAVY, toPalletingIds(1L))
            ),
            highLayer.cells
        );
    }

    @DisplayName("Получить id всех товаров в слое")
    @Test
    void getItemIds() {
        layer.cells.add(new Cell(new Rect(0, 0, 1, 1), 1, 1, WeightClass.MEDIUM, toPalletingIds(1L, 2L)));
        layer.cells.add(new Cell(new Rect(0, 0, 1, 1), 1, 1, WeightClass.MEDIUM, toPalletingIds(3L)));
        Assertions.assertEquals(
            toPalletingIds(1L, 2L, 3L),
            layer.getAllItemIds()
        );
    }

    @DisplayName("Создание виртуальных паллет возможно только в нулевом слое")
    @Test
    void createAdditionalLayersInGroundLayerOnly() {
        final SimpleLayer layer1 = new SimpleLayer(
            MODEL_PARAMETERS,
            this.layer,
            LAYER_SIZE,
            HEIGHT,
            false
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> layer1.createAdditionalLayers("1")
        );
    }

    @DisplayName("Создание виртуальных паллет возможно только в нулевом слое")
    @Test
    void createAdditionalLayers() {
        layer.freeRects.clear();
        final Rect freeRect1 = new Rect(100, 0, 20, 80);
        final Rect freeRect2 = new Rect(0, 60, 100, 20);
        layer.freeRects.add(freeRect1);
        layer.freeRects.add(freeRect2);

        final Layer multiLayer = layer.createAdditionalLayers("1");
        Assertions.assertTrue(multiLayer instanceof MultiLayer);
        Assertions.assertSame(((MultiLayer) multiLayer).getMain(), layer); // check the same object
        Assertions.assertEquals(
            List.of(
                new RegionOnPallet("1-1", layer, freeRect1.getPosition(), freeRect1.getSize(), MODEL_PARAMETERS),
                new RegionOnPallet("1-2", layer, freeRect2.getPosition(), freeRect2.getSize(), MODEL_PARAMETERS)
            ),
            ((MultiLayer) multiLayer).getAdditional()
        );
    }

    @DisplayName("Ранжирование свободного места для коробки")
    @Test
    void freeSpaceRating() {
        final Rect rect1 = new Rect(0, 3, 10, 10);
        final Rect rect2 = new Rect(0, 0, 10, 11);

        // Best fit
        Assertions.assertEquals(50, layer.rating(rect1, blockOfSize(10, 10)), "Best fit");
        Assertions.assertEquals(50, layer.rating(rect1, blockOfSize(9, 9)), "Best fit delta");

        // Identical boxes
        layer.previousCellPosition = new PreviousCellPosition(new Rect(0, 0, 10, 3));
        Assertions.assertEquals(25, layer.rating(rect1, blockOfSize(10, 3)), "Neighbour");


        // Across the pallet (can be turned off using model params)
        Assertions.assertEquals(10, layer.rating(rect2, blockOfSize(4, 9)), "Across pallet");

        // Across rect
        Assertions.assertEquals(5, layer.rating(rect2, blockOfSize(9, 4)), "Across rect");

        // One free rect is greather then other
        Assertions.assertEquals(2, layer.rating(rect2.flipRect(), blockOfSize(10, 2)), "Across rect");
    }

    private PackagingBlock blockOfSize(int length, int width) {
        return new PackagingBlock(
            Collections.emptyList(),
            new Size(length, width),
            1,
            1,
            1,
            WeightClass.MEDIUM,
            null,
            null,
            1
        );
    }
}
