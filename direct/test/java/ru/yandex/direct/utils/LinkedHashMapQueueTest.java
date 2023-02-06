package ru.yandex.direct.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class LinkedHashMapQueueTest {
    @Test
    public void testDropKey() {
        LinkedHashMapQueue<String, String> queue = new LinkedHashMapQueue<>(s -> s);
        queue.add("a");
        Assert.assertEquals(Collections.singletonList("a"), new ArrayList<>(queue));
        queue.dropKey("a");
        // удаление несуществующего ключа не должно бросать исключение
        queue.dropKey("b");
        Assert.assertEquals(Collections.emptyList(), new ArrayList<>(queue));
    }

    @Test
    public void testOrder() {
        LinkedHashMapQueue<String, String> queue = new LinkedHashMapQueue<>(s -> s);
        queue.add("a");
        queue.add("b");
        queue.add("a");
        Assert.assertEquals(Arrays.asList("a", "b"), new ArrayList<>(queue));
    }
}
