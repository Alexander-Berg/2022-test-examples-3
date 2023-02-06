package ru.yandex.market.ir.nirvana.modelpublisher;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * @author inenakhov
 */
public class BatcherTest {
    @Test
    public void add() throws Exception {
        ArrayList<Integer> buffer = new ArrayList<>();
        Batcher<Integer> batcher = new Batcher<>(2, buffer::addAll);
        batcher.add(1);
        Assert.assertEquals(0, buffer.size());
        batcher.add(2);
        Assert.assertEquals(2, buffer.size());
        batcher.add(1);
        Assert.assertEquals(2, buffer.size());
        batcher.add(2);
        Assert.assertEquals(4, buffer.size());
        Assert.assertEquals(Lists.newArrayList(1, 2, 1, 2), buffer);
        batcher.add(1);
        batcher.flush();
        Assert.assertEquals(Lists.newArrayList(1, 2, 1, 2, 1), buffer);
    }
}