package ru.yandex.market.api.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Test;

import ru.yandex.market.api.util.concurrent.Slicer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class SlicerTest {

    @Test
    public void shouldSlice() throws Exception {
        List<Integer> collection = Lists.newArrayList(1, 2, 3, 4, 5);
        Collection<List<Integer>> slices = Slicer.slice(collection, 3);

        assertEquals(2, slices.size());
        Iterator<List<Integer>> iterator = slices.iterator();

        Collection<Integer> next = iterator.next();
        assertArrayEquals(new Integer[]{1, 2, 3}, next.toArray());

        next = iterator.next();
        assertArrayEquals(new Integer[]{4, 5}, next.toArray());
    }

    @Test
    public void primitiveInts() throws Exception {
        IntList collection = new IntArrayList(new int[]{1, 2, 3, 4, 5});
        Collection<IntList> slices = Slicer.slice(collection, 3);

        assertEquals(2, slices.size());
        Iterator<IntList> iterator = slices.iterator();

        IntList next = iterator.next();
        assertArrayEquals(new int[]{1, 2, 3}, next.toIntArray());

        next = iterator.next();
        assertArrayEquals(new int[]{4, 5}, next.toIntArray());
    }

    @Test
    public void primitiveLongs() throws Exception {
        LongList collection = new LongArrayList(new long[]{1, 2, 3, 4, 5});
        Collection<LongList> slices = Slicer.slice(collection, 3);

        assertEquals(2, slices.size());
        Iterator<LongList> iterator = slices.iterator();

        LongList next = iterator.next();
        assertArrayEquals(new long[]{1, 2, 3}, next.toLongArray());

        next = iterator.next();
        assertArrayEquals(new long[]{4, 5}, next.toLongArray());
    }

    @Test
    public void shouldSliceIntArray() throws Exception {
        int[] array = new int[] {1, 2, 3, 4, 5, 6, 7};

        List<int[]> result = Slicer.slice(array, 3);

        assertEquals(3, result.size());
        assertArrayEquals(new int[]{1, 2, 3}, result.get(0));
        assertArrayEquals(new int[]{4, 5, 6}, result.get(1));
        assertArrayEquals(new int[]{7}, result.get(2));
    }
}
