package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PalletingItem;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PalletingItemWithCount;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PalletingWay;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.BoxCombinationCoeff;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PackagingBlock;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size3D;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.WeightClass;

import static ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.PallettingIdConverter.toPalletingIds;

class ItemsPrepareToolTest {
    @DisplayName("Посчитать кол-во паллет: нет груза")
    @Test
    void predictPalletCountZero() {
        Assertions.assertEquals(
            0,
            new ItemsPrepareTool(null).predictPalletCount(Collections.emptyList())
        );
    }

    @DisplayName("Посчитать кол-во паллет: объём груза меньше 1 паллеты")
    @Test
    void predictPalletCountLessThenOnePallet() {
        Assertions.assertEquals(
            1,
            new ItemsPrepareTool(null).predictPalletCount(List.of(
                new RegisterUnit().setKorobyte(korobyte(1, 1, 1))
            ))
        );
    }

    @DisplayName("Посчитать кол-во паллет: из-за коэффициента заполнения будет больше, чем нужно")
    @Test
    void predictPalletCountMoreThenNeeded() {
        Assertions.assertEquals(
            3,
            new ItemsPrepareTool(null).predictPalletCount(List.of(
                new RegisterUnit().setKorobyte(korobyte(100, 80, 180)),
                new RegisterUnit().setKorobyte(korobyte(100, 80, 180))
            ))
        );
    }

    @DisplayName("Посчитать кол-во паллет")
    @Test
    void predictPalletCount() {
        Assertions.assertEquals(
            2,
            new ItemsPrepareTool(null).predictPalletCount(List.of(
                new RegisterUnit().setKorobyte(korobyte(100, 80, 150)),
                new RegisterUnit().setKorobyte(korobyte(100, 80, 150))
            ))
        );
    }

    @DisplayName("Классификация айтемов")
    @Test
    void classifyItem() {
        final List<Korobyte> regular = List.of(
            korobyte(100, 70, 50),
            korobyte(80, 100, 50)
        );
        final List<Korobyte> oversize = List.of(
            korobyte(100, 70, 200),
            korobyte(80, 100, 200),
            korobyte(130, 70, 50),
            korobyte(100, 90, 50),
            korobyte(70, 130, 50),
            korobyte(90, 100, 50)
        );
        final List<Korobyte> longThin = List.of(
            korobyte(130, 20, 30),
            korobyte(20, 130, 30)
        );

        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(new ModelParameters(
            null, false, 0, 0, false, 1D
        ));

        validateItemPalletingWay(itemsPrepareTool, regular, PalletingWay.REGULAR);
        validateItemPalletingWay(itemsPrepareTool, oversize, PalletingWay.OVERSIZE);
        validateItemPalletingWay(itemsPrepareTool, longThin, PalletingWay.LONG_THIN);
    }

    @DisplayName("Выделение длинных тонких айтемов с разными параметрами модели")
    @Test
    void classifyItemLongThin() {
        classifyItemLongThin(1D, List.of(
            Pair.of(korobyte(130, 20, 30), PalletingWay.LONG_THIN),
            Pair.of(korobyte(20, 130, 30), PalletingWay.LONG_THIN),
            Pair.of(korobyte(100, 20, 30), PalletingWay.REGULAR),
            Pair.of(korobyte(20, 100, 30), PalletingWay.REGULAR),
            Pair.of(korobyte(50, 20, 30), PalletingWay.REGULAR),
            Pair.of(korobyte(20, 50, 30), PalletingWay.REGULAR)
        ));
        classifyItemLongThin(0.7D, List.of(
            Pair.of(korobyte(130, 20, 30), PalletingWay.LONG_THIN),
            Pair.of(korobyte(20, 130, 30), PalletingWay.LONG_THIN),
            Pair.of(korobyte(100, 20, 20), PalletingWay.LONG_THIN),
            Pair.of(korobyte(20, 100, 20), PalletingWay.LONG_THIN),
            Pair.of(korobyte(50, 10, 15), PalletingWay.REGULAR),
            Pair.of(korobyte(10, 50, 15), PalletingWay.REGULAR)
        ));
        classifyItemLongThin(0.5D, List.of(
            Pair.of(korobyte(130, 20, 30), PalletingWay.LONG_THIN),
            Pair.of(korobyte(20, 130, 30), PalletingWay.LONG_THIN),
            Pair.of(korobyte(100, 20, 20), PalletingWay.LONG_THIN),
            Pair.of(korobyte(20, 100, 20), PalletingWay.LONG_THIN),
            Pair.of(korobyte(61, 10, 15), PalletingWay.LONG_THIN),
            Pair.of(korobyte(10, 61, 15), PalletingWay.LONG_THIN)
        ));
    }

    @DisplayName("Поиск плоских телевизоров")
    @Test
    void classifyItemOversizeBunch() {
        classifyItemOversizeBunch(List.of(
            Pair.of(korobyte(130, 20, 100), PalletingWay.OVERSIZE_BUNCH),
            Pair.of(korobyte(20, 130, 100), PalletingWay.OVERSIZE_BUNCH),
            Pair.of(korobyte(100, 20, 30), PalletingWay.REGULAR),
            Pair.of(korobyte(20, 100, 30), PalletingWay.REGULAR)
        ));
    }

    private void classifyItemOversizeBunch(List<Pair<Korobyte, PalletingWay>> data) {
        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(new ModelParameters(
            null, false, 0, 0, false, 1.0
        ));
        validateItemPalletingWay(itemsPrepareTool, data);
    }

    private void classifyItemLongThin(double longThinMinRatio, List<Pair<Korobyte, PalletingWay>> data) {
        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(new ModelParameters(
            null, false, 0, 0, false, longThinMinRatio
        ));
        validateItemPalletingWay(itemsPrepareTool, data);
    }

    private void validateItemPalletingWay(
        ItemsPrepareTool itemsPrepareTool,
        List<Korobyte> korobytes,
        PalletingWay expectedPalletingWay
    ) {
        validateItemPalletingWay(
            itemsPrepareTool,
            korobytes
                .stream()
                .map(x -> Pair.of(x, expectedPalletingWay))
                .collect(Collectors.toList())
        );
    }

    private void validateItemPalletingWay(
        ItemsPrepareTool itemsPrepareTool,
        List<Pair<Korobyte, PalletingWay>> data
    ) {
        data.forEach(d -> {
            Assertions.assertEquals(
                d.getRight(),
                itemsPrepareTool.classifyItem(new RegisterUnit().setKorobyte(d.getLeft()))
            );
        });
    }

    @DisplayName("Группировка коробок по размеру с учётом погрешности ")
    @Test
    void fuzzyGroupBySize3D() {
        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(null);

        final PalletingItem u1 = new RegisterUnit().setKorobyte(korobyte(100, 50, 40));
        final PalletingItem u2 = new RegisterUnit().setKorobyte(korobyte(98, 51, 41));
        final PalletingItem u3 = new RegisterUnit().setKorobyte(korobyte(100, 50, 2));
        final PalletingItem u4 = new RegisterUnit().setKorobyte(korobyte(101, 49, 40));

        final Map<Size3D, List<PalletingItem>> itemsBySizes =
            itemsPrepareTool.fuzzyGroupBySize3D(List.of(u1, u2, u3, u4));

        Assertions.assertEquals(
            Map.of(
                new Size3D(100, 50, 2), List.of(u3),
                new Size3D(101, 51, 41), List.of(u1, u2, u4)
            ),
            itemsBySizes
        );
    }

    @DisplayName("Комбинировать коробки в блоки одинаковой высоты")
    @Test
    void detectDimensionDependencies() {
        final int skipTopCountForCombination = 0;
        final int boxCombinationMaxMultiplier = 10;
        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(new ModelParameters(
            null, false, skipTopCountForCombination, boxCombinationMaxMultiplier, false, 1D
        ));

        final Size3D s1 = new Size3D(50, 50, 50);
        final Size3D s2 = new Size3D(30, 40, 30);
        final Size3D s3 = new Size3D(30, 40, 20);
        final Size3D s4 = new Size3D(10, 10, 10);
        final Map<Size3D, BoxCombinationCoeff> coeffMap =
            itemsPrepareTool.detectDimensionDependencies(List.of(s1, s2, s3, s4));

        Assertions.assertEquals(
            Map.of(
                s1, new BoxCombinationCoeff().setEtalon(s1).setEtalonCount(2).setCount(2),
                s2, new BoxCombinationCoeff().setEtalon(s2).setEtalonCount(1).setCount(1),
                s3, new BoxCombinationCoeff().setEtalon(s1).setEtalonCount(2).setCount(5), // Блоки высоты 100, как у
                // 2 больших коробок
                s4, new BoxCombinationCoeff().setEtalon(s1).setEtalonCount(2).setCount(10) // Блоки высоты 100, как у
                // 2 больших коробок
            ),
            coeffMap
        );
    }

    @DisplayName("Комбинировать коробки в блоки одинаковой высоты: ограничение по кол-ву коробок в блоке")
    @Test
    void detectDimensionDependenciesHeightLimit() {
        final int skipTopCountForCombination = 0;
        final int boxCombinationMaxMultiplier = 4;
        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(new ModelParameters(
            null, false, skipTopCountForCombination, boxCombinationMaxMultiplier, false, 1D
        ));

        final Size3D s1 = new Size3D(50, 50, 50);
        final Size3D s2 = new Size3D(30, 40, 30);
        final Size3D s3 = new Size3D(30, 40, 20);
        final Size3D s4 = new Size3D(10, 10, 10);
        final Map<Size3D, BoxCombinationCoeff> coeffMap =
            itemsPrepareTool.detectDimensionDependencies(List.of(s1, s2, s3, s4));

        Assertions.assertEquals(
            Map.of(
                s1, new BoxCombinationCoeff().setEtalon(s1).setEtalonCount(1).setCount(1),
                s2, new BoxCombinationCoeff().setEtalon(s2).setEtalonCount(2).setCount(2),
                s3, new BoxCombinationCoeff().setEtalon(s2).setEtalonCount(2).setCount(3), // Блоки высоты 100, как у
                // 2 больших коробок
                s4, new BoxCombinationCoeff().setEtalon(s4).setEtalonCount(1).setCount(1) // Блоки высоты 100, как у
                // 2 больших коробок
            ),
            coeffMap
        );
    }

    @DisplayName("Комбинировать коробки в блоки одинаковой высоты, пропустить самую большую коробку")
    @Test
    void detectDimensionDependenciesSkipTallest() {
        final int skipTopCountForCombination = 1;
        final int boxCombinationMaxMultiplier = 10;
        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(new ModelParameters(
            null, false, skipTopCountForCombination, boxCombinationMaxMultiplier, false, 1D
        ));

        final Size3D s1 = new Size3D(50, 50, 50);
        final Size3D s2 = new Size3D(30, 40, 30);
        final Size3D s3 = new Size3D(30, 40, 20);
        final Size3D s4 = new Size3D(10, 10, 10);
        final Map<Size3D, BoxCombinationCoeff> coeffMap =
            itemsPrepareTool.detectDimensionDependencies(List.of(s1, s2, s3, s4));

        Assertions.assertEquals(
            Map.of(
                // Не страшно, что мы выкинули 1-ю строку. Если мы не посчитали коэффициент для какого-то размера
                // коробок,
                // значит будет взята 1
                s2, new BoxCombinationCoeff().setEtalon(s2).setEtalonCount(2).setCount(2),
                s3, new BoxCombinationCoeff().setEtalon(s2).setEtalonCount(2).setCount(3), // Блоки высоты 100, как у
                // 2 больших коробок
                s4, new BoxCombinationCoeff().setEtalon(s2).setEtalonCount(2).setCount(6) // Блоки высоты 100, как у
                // 2 больших коробок
            ),
            coeffMap
        );
    }

    @DisplayName("Разложить коробки по блокам одинаковой высоты")
    @Test
    void toPackagingBlocks() {
        final RegisterUnit item = new RegisterUnit().setKorobyte(korobyte(100, 10, 5));
        List<PalletingItemWithCount> items = times(item, 10).collect(Collectors.toList());
        BoxCombinationCoeff coeff = new BoxCombinationCoeff()
            .setEtalon(new Size3D(10, 10, 20))
            .setEtalonCount(1)
            .setCount(4);

        final ItemsPrepareTool itemsPrepareTool = new ItemsPrepareTool(null);
        final List<PackagingBlock> blocks = itemsPrepareTool.toPackagingBlocks(1D, items, coeff);

        final Size size = new Size(100, 10);
        Assertions.assertEquals(
            List.of(
                new PackagingBlock(
                    toPalletingIds(CountType.UNDEFINED, 1L, 2L, 3L, 4L),
                    size,
                    20,
                    4,
                    1,
                    WeightClass.MEDIUM,
                    null,
                    null,
                    4
                ),
                new PackagingBlock(
                    toPalletingIds(CountType.UNDEFINED, 5L, 6L, 7L, 8L),
                    size,
                    20,
                    4,
                    1,
                    WeightClass.MEDIUM,
                    null,
                    null,
                    4
                ),
                new PackagingBlock(
                    toPalletingIds(CountType.UNDEFINED, 9L), size, 5, 1, 1, WeightClass.MEDIUM, null, null, 1
                ),
                new PackagingBlock(
                    toPalletingIds(CountType.UNDEFINED, 10L), size, 5, 1, 1, WeightClass.MEDIUM, null, null, 1
                )
            ),
            blocks
        );
    }

    private Stream<PalletingItemWithCount> times(RegisterUnit unit, int times) {
        return IntStream.range(0, times)
            .mapToObj(i -> PalletingItemWithCount.ofItem(
                new RegisterUnit().setType(UnitType.ITEM).setKorobyte(unit.getKorobyte()).setId((long) (i + 1)),
                0,
                CountType.UNDEFINED
            ));
    }

    private Korobyte korobyte(int length, int width, int height) {
        return new Korobyte().setLength(length).setWidth(width).setHeight(height).setWeightGross(BigDecimal.ONE);
    }

}
