package ru.yandex.market.mbo.utils;

import org.junit.Test;
import ru.yandex.bolts.collection.Cf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class SearchableCacheTest {
    private List<Item> items = Arrays.asList(
        new Item(1, "a", 100),
        new Item(2, "b", 100),
        new Item(3, "b", 100),
        new Item(4, "aabbcc", 101),
        new Item(5, "d", 102)
        );

    private SearchableCache<Integer, Item> cache =
        new SearchableCache<>(items, i -> i.id, Item.ID, Item.NAME, Item.VALUE, Item.LETTERS);

    @Test
    public void testSimpleCase() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Collections.singletonList(0))
            .setFullScanFactor(1.0));
        assertEquals(0, result.size());
    }

    @Test
    public void testSearchForSeveralIds() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Arrays.asList(1, 2))
            .setFullScanFactor(1.0));
        assertEquals(2, result.size());
        assertEquals(Cf.set(1, 2), getIds(result));
    }

    @Test
    public void testMultipleItems() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Arrays.asList(1, 2, 3))
            .setFilter(Item.NAME, Arrays.asList("b", "c"))
            .setFullScanFactor(1.0));
        assertEquals(2, result.size());
        assertEquals(Cf.set(2, 3), getIds(result));
    }

    @Test
    public void testNoConditions() {
        List<Item> result = cache.search(cache.createRequest()
            .setFullScanFactor(1.0));
        assertEquals(5, result.size());
        assertEquals(Cf.set(1, 2, 3, 4, 5), getIds(result));
    }

    @Test
    public void testNoIntersection() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Arrays.asList(1, 2))
            .setFilter(Item.NAME, Collections.singleton("c"))
            .setFullScanFactor(1.0));
        assertEquals(0, result.size());
    }

    @Test
    public void testNoResultsForOneQuery() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Arrays.asList(1, 2))
            .setFilter(Item.NAME, Collections.singleton("absent value"))
            .setFullScanFactor(1.0));
        assertEquals(0, result.size());
    }

    @Test
    public void testMultipleValues() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.LETTERS, Collections.singletonList("a"))
            .setFullScanFactor(1.0));
        assertEquals(2, result.size());
        assertEquals(Cf.set(1, 4), getIds(result));
    }

    @Test
    public void testFullScan() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Arrays.asList(1, 2, 3))
            .setFilter(Item.NAME, Arrays.asList("b", "c"))
            .setFullScanFactor(0.0)); // NOTE 0: full scan will be triggered always
        assertEquals(2, result.size());
        assertEquals(Cf.set(2, 3), getIds(result));
    }

    @Test
    public void testPredicates() {
        List<Item> result = cache.search(cache.createRequest()
            .setFilter(Item.ID, Arrays.asList(1, 2, 3, 4, 5))
            .addPredicate(i -> i.name.startsWith("a"))
            .setFullScanFactor(1.0));
        assertEquals(2, result.size());
        assertEquals(Cf.set(1, 4), getIds(result));
    }

    private Set<Integer> getIds(List<Item> items) {
        return items.stream().map(i -> i.id).collect(Collectors.toSet());
    }

    static class Item {
        static final SearchableCache.Field<Item> ID = i -> i.id;
        static final SearchableCache.Field<Item> NAME = i -> i.name;
        static final SearchableCache.Field<Item> VALUE = i -> i.value;
        static final SearchableCache.Field<Item> LETTERS =
            i -> Stream.of(i.name.split("")).collect(Collectors.toList());


        final int id;
        final String name;
        final int value;

        Item(int id, String name, int value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }
    }
}
