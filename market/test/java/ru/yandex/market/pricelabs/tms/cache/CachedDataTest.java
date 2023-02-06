package ru.yandex.market.pricelabs.tms.cache;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.types.FilterCategoryType;
import ru.yandex.market.pricelabs.model.types.FilterClass;
import ru.yandex.market.pricelabs.model.types.FilterOfferType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategy;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.filter;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

class CachedDataTest {

    @Test
    void testEmpty() {
        var cached = CachedData.EMPTY;
        assertEquals(0, cached.getFilterMap().size());
    }

    @Test
    void testConfigured() {
        var f1 = filter(1);
        var f2 = filter(2);

        var sh1 = shop(1);
        var sh2 = shop(2);

        var a1 = autostrategy(1, 1);
        var a2 = autostrategy(2, 1);

        var cached = new CachedData(
                new CachedDataPerShop(sh1, List.of(f1, f2), List.of(a1, a2), true, Set.of()));

        var ff = cached.getFilterMap();
        assertEquals(2, ff.size());
        assertEquals(f1, ff.get(1));
        assertEquals(f2, ff.get(2));

        Int2DoubleMap r1Map = new Int2DoubleArrayMap();
        r1Map.put(1, 1.1);
        r1Map.put(2, 1.2);

        Int2DoubleMap r2Map = new Int2DoubleArrayMap();
        r2Map.put(1, 1.3);

        var autostrategies = cached.getAutostrategyMap();
        assertEquals(a1, autostrategies.get(1));
        assertEquals(a2, autostrategies.get(2));

        assertTrue(cached.isHasAutostrategiesInHistory());

        assertEquals(sh1, cached.getShop());
    }

    @Test
    void testInitFilters() {
        var f1 = filter(1, f -> f.setCategory_type(FilterCategoryType.ID));
        var f2 = filter(2, f -> {
            f.setCategory_type(FilterCategoryType.ID);
            f.setCategory("cat1");
        });
        var f3 = filter(3, f -> {
            f.setCategory_type(FilterCategoryType.NAME);
            f.setCategories_by_id(Set.of(1L, 2L));
        });
        var f4 = filter(4, f -> f.setCategory("cat1"));
        var f5 = filter(5, f -> f.setCategories_by_id(Set.of()));
        var f6 = filter(6, f -> f.setCategories_by_id(Set.of(99L)));

        var filters = List.of(f1, f2, f3, f4, f5, f6);

        var cached = new CachedData(
                new CachedDataPerShop(shop(1), filters, List.of(), false, Set.of()));

        var ff = cached.getFilterMap();
        assertEquals(6, ff.size());

        filters.forEach(f -> {
            f.setOffer_type(FilterOfferType.ALL);
            f.setFilter_class(FilterClass.DEFAULT);
        });

        assertEquals(f1, ff.get(1));
        assertEquals(f2, ff.get(2));
        assertEquals(f3, ff.get(3));

        f4.setCategory_type(FilterCategoryType.NAME);
        assertEquals(f4, ff.get(4));

        f5.setCategory_type(FilterCategoryType.NAME);
        assertEquals(f5, ff.get(5));

        f6.setCategory_type(FilterCategoryType.ID);
        assertEquals(f6, ff.get(6));

        assertFalse(cached.isHasAutostrategiesInHistory());
    }

    private static Collection<Filter> deepCopy(List<Filter> filters) {
        return filters.stream()
                .map(YTreeDeepCopier::deepCopyOf)
                .collect(Collectors.toList());
    }

}
