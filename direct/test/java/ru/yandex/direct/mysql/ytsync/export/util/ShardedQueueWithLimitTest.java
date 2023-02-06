package ru.yandex.direct.mysql.ytsync.export.util;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.mysql.ytsync.export.util.queue.ShardedQueueWithLimit;

import static java.util.Arrays.asList;

public class ShardedQueueWithLimitTest {
    private int fillQueue(ShardedQueueWithLimit queue) {
        queue.add("ppc:1", asList(
                new IdRange("1", "10", 2),
                new IdRange("11", "20", 4),
                new IdRange("21", "30", 7),
                new IdRange("31", "40", 10)));
        queue.add("ppc:2", asList(
                new IdRange("1", "100", 30),
                new IdRange("101", "200", 50),
                new IdRange("201", "300", 5)
        ));
        queue.add("ppc:3", asList(
                new IdRange("1", "1000", 1000),
                new IdRange("1001", "2000", 104)
        ));
        // Всего 9 диапазонов
        return 9;
    }

    @Test
    public void testLongMaxLimit() {
        ShardedQueueWithLimit queue = new ShardedQueueWithLimit(Long.MAX_VALUE);
        int chunksAdded = fillQueue(queue);
        for (int i = 0; i < chunksAdded; ++i) {
            Assert.assertEquals(chunksAdded - i, queue.getChunksCount());
            Assert.assertFalse(queue.isEmpty());
            Assert.assertNotNull(queue.poll());
        }
        // Больше изначально добавленных чанков извлечь нельзя
        Assert.assertEquals(0, queue.getChunksCount());
        Assert.assertTrue(queue.isEmpty());
        Assert.assertNull(queue.poll());
    }

    @Test
    public void testSmallLimit() {
        ShardedQueueWithLimit queue = new ShardedQueueWithLimit(5);
        fillQueue(queue);
        for (int i = 0; i < 5; ++i) {
            Assert.assertEquals(5 - i, queue.getChunksCount());
            Assert.assertFalse(queue.isEmpty());
            Assert.assertNotNull(queue.poll());
        }
        // Больше ограниченного кол-ва чанков извлечь нельзя
        Assert.assertEquals(0, queue.getChunksCount());
        Assert.assertTrue(queue.isEmpty());
        Assert.assertNull(queue.poll());
    }
}
