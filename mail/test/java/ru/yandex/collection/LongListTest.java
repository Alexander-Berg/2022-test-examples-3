package ru.yandex.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class LongListTest extends TestBase {
    public LongListTest() {
        super(false, 0L);
    }

    private static void check(
        final List<Long> expected,
        final LongList actual)
    {
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(
            expected.toString(),
            actual.toString());
        Assert.assertEquals(
            expected.toString(),
            Arrays.toString(actual.toLongArray()));
        Assert.assertEquals(expected.hashCode(), actual.hashCode());
    }

    @Test
    public void testResize() {
        LongList list = new LongList();
        list.addLong(1);
        list.addLong(2);
        list.addLong(Long.MAX_VALUE);
        list.addLong(Long.MIN_VALUE);
        Assert.assertEquals((Long) 2L, list.remove(1));
        list.resize(4);
        list.addLong(Long.MAX_VALUE >> 3);
        list.addLong(2);
        List<Long> expected = Arrays.asList(
            (Long) 1L,
            (Long) Long.MAX_VALUE,
            (Long) Long.MIN_VALUE,
            (Long) 0L,
            (Long) Long.MAX_VALUE >> 3,
            (Long) 2L);
        check(expected, list);

        LongList newList = new LongList(4);
        newList.add(1L);
        newList.add(Long.MAX_VALUE);
        newList.add(Long.MIN_VALUE);
        newList.add(2L);
        newList.set(3, 0L);
        newList.add(Long.MAX_VALUE >> 3);
        newList.add(2L);
        check(list, newList);
    }

    @Test
    public void testGrow() {
        List<Long> expected = new ArrayList<>();
        LongList actual = new LongList();
        for (int i = 0; i < 1024; ++i) {
            expected.add((long) i);
            actual.add((long) i);
        }
        check(expected, actual);
    }
}

