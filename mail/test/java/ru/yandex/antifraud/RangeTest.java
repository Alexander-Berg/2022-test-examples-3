package ru.yandex.antifraud;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.util.Range;
import ru.yandex.test.util.TestBase;

public class RangeTest extends TestBase {
    public RangeTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        final Range<Integer> union = new Range<>(Integer::compare);

        union.unionWith(1, 2);
        Assert.assertEquals(Arrays.asList(1, 2), union.bounds());

        union.unionWith(100, 1000);
        Assert.assertEquals(Arrays.asList(1, 2, 100, 1000), union.bounds());

        union.unionWith(1, 5);
        Assert.assertEquals(Arrays.asList(1, 5, 100, 1000), union.bounds());

        union.unionWith(4, 5);
        Assert.assertEquals(Arrays.asList(1, 5, 100, 1000), union.bounds());

        union.unionWith(6, 99);
        Assert.assertEquals(Arrays.asList(1, 5, 6, 99, 100, 1000), union.bounds());

        union.unionWith(5, 1000);
        Assert.assertEquals(Arrays.asList(1, 1000), union.bounds());
    }
}
