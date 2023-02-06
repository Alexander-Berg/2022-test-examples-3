package ru.yandex.direct.mysql.ytsync.export.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.mysql.ytsync.export.util.queue.ShardedQueue;
import ru.yandex.direct.mysql.ytsync.export.util.queue.ShardedQueueValue;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ShardedQueueTest {
    private ShardedQueue<Long> queue;

    @Before
    public void setup() {
        queue = new ShardedQueue<>();
        queue.add("ppc:1", Arrays.asList(1L, 4L, 7L, 9L));
        queue.add("ppc:2", Arrays.asList(2L, 5L, 8L));
        queue.add("ppc:3", Arrays.asList(3L, 6L));
    }

    @Test
    public void emptyQueueIsEmpty() {
        ShardedQueue<Long> emptyQueue = new ShardedQueue<>();
        MatcherAssert.assertThat(emptyQueue.poll(), is(nullValue()));
    }

    @Test
    public void neverReleasedInOrder() {
        List<String> dbNames = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        ShardedQueueValue<Long> element;
        while ((element = queue.poll()) != null) {
            dbNames.add(element.getDbName());
            values.add(element.getValue());
        }
        MatcherAssert.assertThat(dbNames, is(Arrays.asList("ppc:1", "ppc:2", "ppc:3", "ppc:1", "ppc:2", "ppc:3", "ppc:1", "ppc:2", "ppc:1")));
        MatcherAssert.assertThat(values, is(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)));
    }

    @Test
    public void refill() {
        ShardedQueueValue<Long> element;
        while ((element = queue.poll()) != null) {
            // просто освобождаем очередь
            element.release();
        }
        // Добавляем данных в шарды
        queue.add("ppc:1", Arrays.asList(11L, 13L, 15L));
        queue.add("ppc:2", Arrays.asList(12L, 14L));
        List<String> dbNames = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        while ((element = queue.poll()) != null) {
            dbNames.add(element.getDbName());
            values.add(element.getValue());
        }
        MatcherAssert.assertThat(dbNames, is(Arrays.asList("ppc:1", "ppc:2", "ppc:1", "ppc:2", "ppc:1")));
        MatcherAssert.assertThat(values, is(Arrays.asList(11L, 12L, 13L, 14L, 15L)));
    }

    @Test
    public void lowerShardPreferred() {
        List<String> dbNames = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        ShardedQueueValue<Long> element;
        while ((element = queue.poll()) != null) {
            dbNames.add(element.getDbName());
            values.add(element.getValue());
            element.release();
        }
        MatcherAssert.assertThat(dbNames, is(Arrays.asList("ppc:1", "ppc:1", "ppc:1", "ppc:1", "ppc:2", "ppc:2", "ppc:2", "ppc:3", "ppc:3")));
        MatcherAssert.assertThat(values, is(Arrays.asList(1L, 4L, 7L, 9L, 2L, 5L, 8L, 3L, 6L)));
    }

    @Test
    public void crossClose() {
        List<String> shards = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        ShardedQueueValue<Long> element;
        ShardedQueueValue<Long> prev = null;
        while ((element = queue.poll()) != null) {
            shards.add(element.getDbName());
            values.add(element.getValue());
            if (prev != null) {
                prev.release();
            }
            prev = element;
        }
        if (prev != null) {
            prev.release();
        }
        MatcherAssert.assertThat(shards, is(Arrays.asList("ppc:1", "ppc:2", "ppc:1", "ppc:2", "ppc:1", "ppc:2", "ppc:1", "ppc:3", "ppc:3")));
        MatcherAssert.assertThat(values, is(Arrays.asList(1L, 2L, 4L, 5L, 7L, 8L, 9L, 3L, 6L)));
    }
}
