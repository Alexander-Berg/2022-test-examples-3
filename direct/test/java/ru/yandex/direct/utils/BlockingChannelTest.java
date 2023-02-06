package ru.yandex.direct.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class BlockingChannelTest {
    @Test
    public void testTakeAllInto() throws InterruptedException {
        List<String> sample = new ArrayList<>(Arrays.asList("a", "b", "c"));
        BlockingChannel<String> channel = new BlockingChannel<>(sample);
        List<String> list = new ArrayList<>();

        Assert.assertEquals(sample, channel.takeAllInto(list));
        Assert.assertEquals(sample, list);

        channel.put("d");
        sample.add("d");
        Assert.assertEquals(sample, channel.takeAllInto(list));
        Assert.assertEquals(sample, list);

        Assert.assertTrue(channel.isEmpty());

        list.clear();
        channel.close();
        Assert.assertEquals(Collections.emptyList(), channel.takeAllInto(list));
    }

    @Test
    public void testTakeBatchInto() throws InterruptedException {
        BlockingChannel<String> channel = new BlockingChannel<>(Arrays.asList("a", "b", "c"));
        List<String> list = new ArrayList<>();

        Assert.assertEquals(Arrays.asList("a", "b"), channel.takeBatchInto(list, 2));

        // повторный вызов no-op
        Assert.assertEquals(Arrays.asList("a", "b"), channel.takeBatchInto(list, 2));

        list.remove("b");
        Assert.assertEquals(Arrays.asList("a", "c"), channel.takeBatchInto(list, 2));

        list.remove("a");
        Assert.assertEquals(Collections.singletonList("c"), channel.takeBatchInto(list, 2));

        list.clear();
        channel.put("d");
        channel.close();
        Assert.assertEquals(Collections.singletonList("d"), channel.takeBatchInto(list, 1));
        Assert.assertTrue(channel.isEmpty());

        list.clear();
        Assert.assertEquals(Collections.emptyList(), channel.takeBatchInto(list, 1));
    }
}
