package ru.yandex.antifraud;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.util.Shard;
import ru.yandex.test.util.TestBase;


public class ShardTest extends TestBase {
    public ShardTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        Assert.assertEquals(1914, Shard.calcByTimestamp(Instant.ofEpochMilli(1634169600000L)));
        Assert.assertEquals(1999, Shard.calcByDays(-1));
    }
}
