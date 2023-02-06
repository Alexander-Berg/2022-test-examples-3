package ru.yandex.market.loyalty.core.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CoreCollectionUtilsTest {
    @Test
    public void shouldChunkArray() {
        List<Pair<Integer, Integer>> numbers = CoreCollectionUtils.splitIntoRanges(100, 0, 10);
        assertEquals(10, numbers.size());
        assertEquals(100, numbers.stream().mapToInt(p -> p.getRight() - p.getLeft()).sum());
    }

    @Test
    public void shouldChunkZeroSizedArray() {
        List<Pair<Integer, Integer>> numbers = CoreCollectionUtils.splitIntoRanges(0, 0, 10);
        assertEquals(0, numbers.size());
        assertEquals(0, numbers.stream().mapToInt(p -> p.getRight() - p.getLeft()).sum());
    }

    @Test
    public void shouldChunkUnaligned() {
        List<Pair<Integer, Integer>> numbers = CoreCollectionUtils.splitIntoRanges(99, 1, 7);
        assertEquals(15, numbers.size());
        assertEquals(98, numbers.stream().mapToInt(p -> p.getRight() - p.getLeft()).sum());
    }

    @Test
    public void arrayConcatTest() {
        assertArrayEquals(
                new Integer[]{1, 2, 2, 3},
                CoreCollectionUtils.arrayConcat(new Integer[]{1, 2}, new Integer[]{2, 3})
        );
    }
}
