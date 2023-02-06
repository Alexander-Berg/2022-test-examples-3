package ru.yandex.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class IntListTest extends TestBase {
    public IntListTest() {
        super(false, 0L);
    }

    private static void check(
        final List<Integer> expected,
        final IntList actual)
    {
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(
            expected.toString(),
            actual.toString());
        Assert.assertEquals(
            expected.toString(),
            Arrays.toString(actual.toIntArray()));
        Assert.assertEquals(expected.hashCode(), actual.hashCode());
    }

    @Test
    public void testResize() {
        IntList list = new IntList();
        list.addInt(1);
        list.addInt(2);
        list.addInt(Integer.MAX_VALUE);
        list.addInt(Integer.MIN_VALUE);
        Assert.assertEquals((Integer) 2, list.remove(1));
        list.resize(4);
        list.addInt(Integer.MAX_VALUE >> 3);
        list.addInt(2);
        List<Integer> expected = Arrays.asList(
            (Integer) 1,
            (Integer) Integer.MAX_VALUE,
            (Integer) Integer.MIN_VALUE,
            (Integer) 0,
            (Integer) Integer.MAX_VALUE >> 3,
            (Integer) 2);
        check(expected, list);

        IntList newList = new IntList(4);
        newList.add(1);
        newList.add(Integer.MAX_VALUE);
        newList.add(Integer.MIN_VALUE);
        newList.add(2);
        newList.set(3, 0);
        newList.add(Integer.MAX_VALUE >> 3);
        newList.add(2);
        check(list, newList);
    }

    @Test
    public void testGrow() {
        List<Integer> expected = new ArrayList<>();
        IntList actual = new IntList();
        for (int i = 0; i < 1024; ++i) {
            expected.add(i);
            actual.add(i);
        }
        check(expected, actual);
    }
}

