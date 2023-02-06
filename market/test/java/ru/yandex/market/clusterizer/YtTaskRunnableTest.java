package ru.yandex.market.clusterizer;

import org.junit.Test;

import static org.junit.Assert.*;

public class YtTaskRunnableTest {
    @Test
    public void oneEqTwo() {
        assertEquals(
            "  ",
            YtTaskRunnable.oneEqTwo("one", "two", new String[0])
        );
        assertEquals(
            " three.c1 = four.c1 ",
            YtTaskRunnable.oneEqTwo("three", "four", new String[]{"c1"})
        );
        assertEquals(
            " five.c1 = six.c1 AND five.c2 = six.c2 ",
            YtTaskRunnable.oneEqTwo("five", "six", new String[]{"c1", "c2"})
        );
    }
}
