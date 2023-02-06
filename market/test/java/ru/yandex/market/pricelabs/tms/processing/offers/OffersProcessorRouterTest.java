package ru.yandex.market.pricelabs.tms.processing.offers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.types.AutostrategyType;
import ru.yandex.market.pricelabs.model.types.FilterType;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesVendorContextSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffersProcessorRouterTest {

    private static final int SHOP_ID = 1;
    private int id = 0;

    @Test
    void testBetterAutostrategy() {
        var auto1 = autostrategy(10);
        var auto2 = autostrategy(20);
        assertTrue(AutostrategiesVendorContextSource.isBetterVendorStrategy(auto1, auto2));
        assertFalse(AutostrategiesVendorContextSource.isBetterVendorStrategy(auto2, auto1));
    }

    @Test
    void testCompressDifferentSettings() {
        var b1 = block(10, Set.of(10), Set.of(10));
        var b2 = block(11, Set.of(10), Set.of(10));
        var b3 = block(12, Set.of(10), Set.of(10));

        // Не могут быть сжаты, т.к. настройки АС отличаются
        assertEquals(toAutostrategy(b3, b2, b1), compress(b1, b2, b3).getRight());

        // Результат всегда стабильный
        assertEquals(toAutostrategy(b3, b2, b1), compress(b3, b1, b2).getRight());
    }

    @Test
    void testCompressDifferentShops() {
        var b1 = block(10, Set.of(10), Set.of(10));
        var b2 = block(10, Set.of(11), Set.of(10));
        var b3 = block(10, Set.of(12), Set.of(10));

        // Не могут быть сжаты, т.к. фильтр по магазинам отличается
        assertEquals(toAutostrategy(b3, b2, b1), compress(b1, b2, b3).getRight());

        // Результат всегда стабильный
        assertEquals(toAutostrategy(b3, b2, b1), compress(b3, b1, b2).getRight());
    }

    @Test
    void testCompressModels() {
        var b1 = block(10, Set.of(10), Set.of(10));
        var b2 = block(10, Set.of(10), Set.of(11));
        var b3 = block(10, Set.of(10), Set.of(12));

        // Сжаты
        assertEquals(Pair.of(toModels(Set.of(10, 11, 12)), toAutostrategy(b3)), compress(b1, b2, b3));

        // Результат всегда стабильный
        assertEquals(Pair.of(toModels(Set.of(10, 11, 12)), toAutostrategy(b3)), compress(b3, b1, b2));
    }

    @Test
    void testCompressMixedModels() {
        var b1 = block(10, Set.of(10), Set.of(10)); // 1
        var b2 = block(11, Set.of(10), Set.of(11)); // 2
        var b3 = block(10, Set.of(10), Set.of(12)); // 1
        var b4 = block(10, Set.of(1), Set.of(13)); // 3
        var b5 = block(10, Set.of(), Set.of(14)); // 4
        var b6 = block(10, Set.of(10), Set.of(15)); // 1
        var b7 = block(10, Set.of(), Set.of(16)); // 4

        // Сжаты
        assertEquals(Pair.of(
                toModels(Set.of(14, 16), Set.of(10, 12, 15), Set.of(13), Set.of(11)), toAutostrategy(b7, b6, b4, b2)),
                compress(b1, b2, b3, b4, b5, b6, b7));

        // Результат всегда стабильный
        assertEquals(Pair.of(
                toModels(Set.of(14, 16), Set.of(10, 12, 15), Set.of(13), Set.of(11)), toAutostrategy(b7, b6, b4, b2)),
                compress(b1, b7, b3, b2, b4, b6, b5));
    }

    @SafeVarargs
    private List<Set<Integer>> toModels(Set<Integer>... models) {
        return List.of(models);
    }

    private List<Autostrategy> toAutostrategy(Block... blocks) {
        return Stream.of(blocks).map(Block::getAutostrategy).collect(Collectors.toList());
    }

    private Pair<List<Set<Integer>>, List<Autostrategy>> compress(Block... blocks) {
        var autostrategies = Stream.of(blocks).map(Block::getAutostrategy).collect(Collectors.toList());
        var filters = Stream.of(blocks).map(Block::getFilter).collect(Collectors.toList());

        var ret = AutostrategiesVendorContextSource.compressAutostrategies(autostrategies, filters);

        var models = ret.stream()
                .map(AutostrategiesVendorContextSource.VendorAutostrategyGroup::getModels).collect(Collectors.toList());
        var autos = ret.stream()
                .map(AutostrategiesVendorContextSource.VendorAutostrategyGroup::getAutostrategy)
                .collect(Collectors.toList());
        return Pair.of(models, autos);
    }

    private Block block(long maxBid, Set<Integer> shops, Set<Integer> models) {
        var auto = autostrategy(maxBid);
        var filter = filter(auto, shops, models);
        return new Block(auto, filter);
    }

    private Autostrategy autostrategy(long maxBid) {
        return TmsTestUtils.autostrategy(++id, SHOP_ID, auto -> {
            auto.setType(AutostrategyType.VPOS);
            auto.setFilter_type(FilterType.VENDOR);
            auto.getVposSettings().setMax_bid(maxBid);
        });
    }

    private Filter filter(Autostrategy auto, Set<Integer> shops, Set<Integer> models) {
        return TmsTestUtils.filter(++id, filter -> {
            filter.setShop_id(SHOP_ID);
            auto.setFilter_id(filter.getFilter_id());
            filter.setShops(shops);
            filter.setModels(models);
        });
    }

    @Value
    private static class Block {
        Autostrategy autostrategy;
        Filter filter;
    }
}
