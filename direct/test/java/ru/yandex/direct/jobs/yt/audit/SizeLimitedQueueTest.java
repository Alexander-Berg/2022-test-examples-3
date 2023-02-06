package ru.yandex.direct.jobs.yt.audit;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeLimitedQueueTest {
    @Test
    void testQueue() {
        SizeLimitedQueue<Integer> queue = new SizeLimitedQueue<>(5);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.add(4);
        queue.add(5);
        queue.add(6);
        assertEquals(asList(2, 3, 4, 5, 6), new ArrayList<>(queue));
        queue.addFirst(1);
        assertEquals(asList(1, 2, 3, 4, 5), new ArrayList<>(queue));
        queue.pollFirst();
        queue.addFirst(100);
        assertEquals(asList(100, 2, 3, 4, 5), new ArrayList<>(queue));
    }

    @Test
    void testAddAll() {
        SizeLimitedQueue<Integer> queue = new SizeLimitedQueue<>(5);
        queue.addAll(asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertEquals(asList(5, 6, 7, 8, 9), new ArrayList<>(queue));
    }
}
