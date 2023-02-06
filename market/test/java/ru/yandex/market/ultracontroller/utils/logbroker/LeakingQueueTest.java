package ru.yandex.market.ultracontroller.utils.logbroker;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class LeakingQueueTest {
    @Test
    public void testLeakingQueue() {
        ArrayList<String> thrownOut = new ArrayList<>();
        ArrayList<String> delivered = new ArrayList<>();
        LeakingQueue<String> queue = new LeakingQueue<>(2, thrownOut::add);
        queue.add("A");
        queue.add("B");
        queue.add("C");
        for (String s = queue.poll(); s != null; s = queue.poll()) {
            delivered.add(s);
        }

        Assert.assertArrayEquals(new String[]{"A"}, thrownOut.toArray());
        Assert.assertArrayEquals(new String[]{"B", "C"}, delivered.toArray());
    }
}
