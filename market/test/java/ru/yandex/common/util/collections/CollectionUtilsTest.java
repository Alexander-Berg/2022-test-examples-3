package ru.yandex.common.util.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import junit.framework.TestCase;

import ru.yandex.common.util.Builder;
import ru.yandex.common.util.functional.Comparators;
import ru.yandex.common.util.functional.Function;
import ru.yandex.common.util.functional.Monoids;

import static org.apache.commons.collections.ListUtils.unmodifiableList;
import static ru.yandex.common.util.collections.CollectionFactory.list;

/**
 * Date: Feb 26, 2009
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class CollectionUtilsTest extends TestCase {

    public void testSplit() {
        final List<Long> longs = CollectionFactory.list(1L, 2L, 3L, 4L, 5L);

        final List<List<Long>> byOne = CollectionUtils.split(longs, 1);
        assertEquals(5, byOne.size());
        assertEquals(Long.valueOf(1L), byOne.get(0).get(0));
        assertEquals(Long.valueOf(5L), byOne.get(4).get(0));

        final List<List<Long>> byTwo = CollectionUtils.split(longs, 2);
        assertEquals(3, byTwo.size());
        assertEquals(CollectionFactory.list(1L, 2L), byTwo.get(0));
        assertEquals(CollectionFactory.list(3L, 4L), byTwo.get(1));
        assertEquals(CollectionFactory.list(5L), byTwo.get(2));

        final List<List<Long>> byThree = CollectionUtils.split(longs, 3);
        assertEquals(2, byThree.size());
        assertEquals(CollectionFactory.list(1L, 2L, 3L), byThree.get(0));
        assertEquals(CollectionFactory.list(4L, 5L), byThree.get(1));

        final List<List<Long>> byFour = CollectionUtils.split(longs, 4);
        assertEquals(2, byFour.size());
        assertEquals(CollectionFactory.list(1L, 2L, 3L, 4L), byFour.get(0));
        assertEquals(CollectionFactory.list(5L), byFour.get(1));

        final List<List<Long>> byFive = CollectionUtils.split(longs, 5);
        assertEquals(1, byFive.size());
        assertEquals(longs, byFive.get(0));

        final List<List<Long>> bySix = CollectionUtils.split(longs, 6);
        assertEquals(1, bySix.size());
        assertEquals(longs, bySix.get(0));
    }

    public void testSplitCollection() {
        final Collection<Long> longs = CollectionFactory.list(1L, 2L, 3L, 4L, 5L);

        final Collection<Collection<Long>> byOne = CollectionUtils.split(longs, 1);
        assertEquals(5, byOne.size());
        assertEquals(Long.valueOf(1L), list(list(byOne).get(0)).get(0));
        assertEquals(Long.valueOf(5L), list(list(byOne).get(4)).get(0));

        final Collection<Collection<Long>> byTwo = CollectionUtils.split(longs, 2);
        assertEquals(3, byTwo.size());
        assertEquals(CollectionFactory.list(1L, 2L), Cf.list(byTwo).get(0));
        assertEquals(CollectionFactory.list(3L, 4L), list(byTwo).get(1));
        assertEquals(CollectionFactory.list(5L), list(byTwo).get(2));

        final Collection<Collection<Long>> byThree = CollectionUtils.split(longs, 3);
        assertEquals(2, byThree.size());
        assertEquals(CollectionFactory.list(1L, 2L, 3L), list(byThree).get(0));
        assertEquals(CollectionFactory.list(4L, 5L), list(byThree).get(1));

        final Collection<Collection<Long>> byFour = CollectionUtils.split(longs, 4);
        assertEquals(2, byFour.size());
        assertEquals(CollectionFactory.list(1L, 2L, 3L, 4L), list(byFour).get(0));
        assertEquals(CollectionFactory.list(5L), list(byFour).get(1));

        final Collection<Collection<Long>> byFive = CollectionUtils.split(longs, 5);
        assertEquals(1, byFive.size());
        assertEquals(longs, list(byFive).get(0));

        final Collection<Collection<Long>> bySix = CollectionUtils.split(longs, 6);
        assertEquals(1, bySix.size());
        assertEquals(longs, list(bySix).get(0));
    }


    public void testMethodListVarags() {
        final List<String> i = CollectionFactory.list("a", "b", "c");
        assertNotNull(i);
        assertEquals("Expected list with three elements", 3, i.size());
    }

    public void testMethodListVaragsWithEmpty() {
        final List<String> usingEmpty = CollectionFactory.list();
        assertNotNull(usingEmpty);
        assertEquals("Expected list with zero elements", 0, usingEmpty.size());
    }

    public void testMethodListVaragsWithNull() {
        try {
            final List<Object> objects = CollectionFactory.list((Object[]) null);
            assertNotNull(objects);
            fail("Method should never return null");
        } catch (NullPointerException expected) {
            // ignore
        }
    }

    public void testMethodListIterable() {
        final List<String> i = CollectionFactory.list("a", "b", "c");
        final List<String> usingIterable = CollectionFactory.list(i);
        assertNotNull(usingIterable);
        assertEquals("Expected list with three elements", 3, usingIterable.size());
    }

    public void testMethodListIterableWithNull() {
        try {
            final List<Object> objects = CollectionFactory.list((Iterable<Object>) null);
            assertNotNull(objects);
            fail("Method should never return null");
        } catch (NullPointerException expected) {
            // ignore
        }
    }

    public void testMethodListWithMixedParams() {
        final List<String> i = CollectionFactory.list("a", "b", "c");
        final List<Object> mix = CollectionFactory.list(i, "a");
        assertNotNull(mix);
        assertEquals("Expected list with 2 elements - iterable und string", 2, mix.size());
    }

    public void testReverse() {
        final List<String> orig = Collections.unmodifiableList(CollectionFactory.list("a", "b", "c", "d"));
        final List<String> copy = CollectionFactory.newList(orig);
        final List<String> reversed = CollectionUtils.reversed(copy);

        assertEquals(orig, copy);
        assertEquals(CollectionFactory.list("d", "c", "b", "a"), reversed);
    }

    public void testAddAllSafe() {
        assertFalse(CollectionUtils.addAllSafe(null, null));
        assertFalse(CollectionUtils.addAllSafe(null, ImmutableSet.of()));
        assertFalse(CollectionUtils.addAllSafe(ImmutableSet.of(), null));

        Set<Long> collection = Sets.newHashSet(3L, 4L, 5L);
        assertTrue(CollectionUtils.addAllSafe(collection, ImmutableSet.of(1L, 2L, 3L)));
        assertTrue(collection.containsAll(ImmutableSet.of(1L, 2L, 3L, 4L, 5L)));

    }

    public void testRemoveSublist() {
        final List<Integer> orig = unmodifiableList(list(1, 2, 3, 4, 5));
        final int from = 1;
        final int count = 3;

        final List<Integer> cutted = CollectionUtils.removeSublist(orig, from, count);

        assertEquals(list(1, 5), cutted);
    }

    public void testZipMapFromPair() throws Exception {
        final List<Pair<Integer, String>> values = Cf.list(Pair.of(1000, "hfd"), Pair.of(3, "c"), Pair.of(100, "asd"), Pair.of(4, "d"));
        final Map<Integer, String> expected = CollectionFactory.newUnorderedMap();
        for (final Pair<Integer, String> value : values) {
            expected.put(value.first, value.second);
        }
        assertEquals(expected, CollectionUtils.zipMap(values));
    }

    public void testZipMapWithBuilder() throws Exception {
        final List<Pair<Integer, String>> values = Cf.list(Pair.of(1000, "hfd"), Pair.of(3, "c"), Pair.of(100, "asd"), Pair.of(4, "d"));
        final Map<Integer, String> expected = CollectionFactory.newUnorderedMap();
        for (final Pair<Integer, String> value : values) {
            expected.put(value.first, value.second);
        }
        assertEquals(expected, CollectionUtils.zipMap(values, new Builder<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> build() {
                return Cf.newUnorderedMap(values.size());
            }
        }));
    }

    public void testZipMapOrder() throws Exception {
        final List<Pair<Integer, String>> values = Cf.list(Pair.of(1000, "hfd"), Pair.of(3, "c"), Pair.of(100, "asd"), Pair.of(4, "d"));
        final Map<Integer, String> expected = CollectionFactory.newLinkedMap();
        for (final Pair<Integer, String> value : values) {
            expected.put(value.first, value.second);
        }

        final Map<Integer, String> actual = CollectionUtils.zipMap(values, new Builder<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> build() {
                return Cf.newLinkedMap(values.size());
            }
        });

        assertEquals(expected.size(), actual.size());

        final Iterator<Map.Entry<Integer, String>> expectedIterator = expected.entrySet().iterator();
        final Iterator<Map.Entry<Integer, String>> actualIterator = actual.entrySet().iterator();

        while (expectedIterator.hasNext()) {
            assertEquals(expectedIterator.next(), actualIterator.next());
        }

    }

    public void testTakeWithOutOfBounds() {
        final List<Integer> a = Cf.list(1, 2, 3, 4, 5);
        assertEquals(Cf.list(4, 5), Cu.take(a, 3, 10));
    }

    public void testCycle() {
        assertEquals(Cf.list(1, 2, 3, 1, 2), Cu.take(5, Cu.cycle(Cf.list(1, 2, 3))));
    }

    public void testCycleEmpty() throws Exception {
        final Iterator<Object> cycled = Cu.cycle(Collections.emptyList()).iterator();
        assertFalse(cycled.hasNext());
    }

    public void testCycleAndRemoveTillEmptyAndThenSomeMore() throws Exception {
        final Iterator<Integer> cycled = Cu.cycle(Cf.list(1, 2, 3, 4, 5)).iterator();
        while (cycled.hasNext()) {
            cycled.next();
            cycled.remove();
        }
    }

    public void testMapIterableBatch() {
        final List<Integer> in = Cf.list(1, 2, 3, 4, 5);

        Function<Integer, String> f1 = new Function<Integer, String>() {
            @Override
            public String apply(final Integer arg) {
                return arg.toString();
            }
        };

        Function<String, String> f2 = new Function<String, String>() {
            @Override
            public String apply(final String arg) {
                return "=" + arg;
            }
        };

        final List<String> out = Cf.list("=1", "=2", "=3", "=4", "=5");
        final Function<Integer, String> then = f1.then(f2);

        assertEquals(out, Cf.list(Cu.mapIterable(in, then)));
        assertEquals(out, Cf.list(Cu.mapIterable(in, then, 3)));
        assertEquals(out, Cf.list(Cu.mapIterable(in, then, 1024)));
    }

    public void testMapIterableWithBatchSize() throws Exception {

        final List<Integer> requests = Cf.list(0, 1, 2, 3, 4);
        final List<Long> results = Cf.list(3373663862L, 3373663863L, 3374679859L, 3373696705L, 83474464L);
        final List<List<Integer>> expected = Cf.list(Cf.list(0, 1, 2), Cf.list(3, 4), Cf.<Integer>newArrayList());
        final List<List<Integer>> trace = Cf.newArrayList();
        final boolean showSteps = System.currentTimeMillis() < 0; // to debug that test

        final Function<Integer, Long> ff = new Function<Integer, Long>() {
            @Override
            public Long apply(final Integer arg) {
                final List<Integer> params = Cf.newArrayList();
                params.add(arg);
                trace.add(params);
                if (showSteps) {
                    System.out.println("params = " + params);
                }
                return results.get(arg);
            }

            @Override
            public List<Long> map(final Iterable<? extends Integer> args) {
                final List<Long> out = Cf.newArrayList();
                final List<Integer> params = Cf.newArrayList();
                for (final Integer arg : args) {
                    params.add(arg);
                    out.add(results.get(arg));
                }
                if (showSteps) {
                    System.out.println("params = " + params);
                }
                trace.add(params);
                return out;
            }
        };

        final Iterable<Long> map = Cu.mapIterable(requests, ff, 3);
        for (final Long item : map) {
            if (showSteps) {
                System.out.println("loaded item = " + item);
            }
        }
        assertEquals(expected, trace);
    }

    public void testMapIterableWithWrongBatchSize() throws Exception {
        try {
            Cu.mapIterable(null, null, 0);
            fail("should report when we have batch size < 1");
        } catch (IllegalArgumentException e) {

        }
    }

    public void testMapNullableCollection() {
        List<Object> emptyResult = CollectionUtils.mapNullableCollection(
            ImmutableSet.of(),
            java.util.function.Function.identity(),
            Collectors.toList()
        );
        assertTrue(emptyResult.isEmpty());

        List<Object> nullResult = CollectionUtils.mapNullableCollection(
            null,
            java.util.function.Function.identity(),
            Collectors.toList()
        );
        assertNull(nullResult);


        Set<Integer> mappedResult = CollectionUtils.mapNullableCollection(
            ImmutableSet.of(1, 2, 3, 4, 5),
            v -> v + 5,
            Collectors.toSet()
        );
        assertTrue(mappedResult.containsAll(ImmutableSet.of(6, 7, 8, 9, 10)));
    }

    public void testFoldL() throws Exception {
        assertEquals(10, Cu.foldL(Monoids.INT_SUM, Cf.list(1, 2, 3, 4)).intValue());
        assertEquals("abcde", Cu.foldL(Monoids.joinWith(""), Cf.list("a", "b", "c", "d", "e")));
    }

    public void testFoldR() throws Exception {
        assertEquals(10, Cu.foldR(Monoids.INT_SUM, Cf.list(1, 2, 3, 4)).intValue());
        assertEquals("edcba", Cu.foldR(Monoids.joinWith(""), Cf.list("a", "b", "c", "d", "e")));
    }

    public void testIterableOfOne() throws Exception {
        final Iterable<String> iterable = Cu.iterable("a");
        final Iterator<String> firstIter = iterable.iterator();
        assertTrue(firstIter.hasNext());
        assertEquals("a", firstIter.next());
        assertFalse(firstIter.hasNext());

        final Iterator<String> secondIter = iterable.iterator();
        assertTrue(secondIter.hasNext());
        assertEquals("a", secondIter.next());
        assertFalse(secondIter.hasNext());
    }

    public void testIterableOfTwo() throws Exception {
        final Iterable<String> iterable = Cu.iterable("a", "b");
        final Iterator<String> firstIter = iterable.iterator();
        assertTrue(firstIter.hasNext());
        assertEquals("a", firstIter.next());
        assertTrue(firstIter.hasNext());
        assertEquals("b", firstIter.next());
        assertFalse(firstIter.hasNext());

        final Iterator<String> secondIter = iterable.iterator();
        assertTrue(secondIter.hasNext());
        assertEquals("a", secondIter.next());
        assertTrue(secondIter.hasNext());
        assertEquals("b", secondIter.next());
        assertFalse(secondIter.hasNext());
    }

    public void testIterableOfTwoWithSameElements() throws Exception {
        final String a = new String("a");
        final Iterable<String> iterable = Cu.iterable(a, a);
        final Iterator<String> firstIter = iterable.iterator();
        assertTrue(firstIter.hasNext());
        assertEquals("a", firstIter.next());
        assertTrue(firstIter.hasNext());
        assertEquals("a", firstIter.next());
        assertFalse(firstIter.hasNext());

    }

    public void testToArray() throws Exception {
        assertTrue(Arrays.equals(new String[]{"a", "b"}, Cu.toArray(Cf.list("a", "b"))));
        assertTrue(Arrays.equals(new String[]{}, Cu.toArray(Cf.list())));
    }

    public void testOrderingAsc() throws Exception {
        assertEquals(Cf.list(1, 2, 3, 0), CollectionUtils.orderingAsc(Cf.list("99", "123", "1233", "21313131"), Comparators.<String>naturalOrder()));
        assertEquals(Cf.list(3, 2, 4, 1, 0), CollectionUtils.orderingAsc(Cf.list(42, 37, 16, 4, 20), Comparators.<Integer>naturalOrder()));
    }

    public void testNullIfEmpty() {
        assertNull(CollectionUtils.nullIfEmpty(Cf.set()));
        assertNull(CollectionUtils.nullIfEmpty(Cf.list()));
        assertNull(CollectionUtils.nullIfEmpty(Cf.newHashMap()));
        assertEquals(Cf.list(1, 2), CollectionUtils.nullIfEmpty(Cf.list(1, 2)));
    }
}
