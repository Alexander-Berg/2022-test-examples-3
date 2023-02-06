package ru.yandex.search.mop.common.searchmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MetashardTest {
    @Test
    public void testShardsRangesEmpty() {
        List<Integer> shards = new ArrayList<>();
        Assert.assertEquals(0, Metashard.generateShardsRanges(shards).size());
    }

    @Test
    public void testShardsRangesOneElement() {
        List<Integer> shards = Collections.singletonList(8);
        List<ShardsRange> ranges = Metashard.generateShardsRanges(shards);
        Assert.assertEquals(1, ranges.size());
        Assert.assertEquals(new ShardsRange(8, 8), ranges.get(0));
    }

    @Test
    public void testShardsRanges() {
        List<Integer> shards = Arrays.asList(8, 1, 5, 3, 11, 9, 7, 2);
        List<ShardsRange> ranges = Metashard.generateShardsRanges(shards);
        Assert.assertEquals(4, ranges.size());
        Assert.assertEquals(new ShardsRange(1, 3), ranges.get(0));
        Assert.assertEquals(new ShardsRange(5, 5), ranges.get(1));
        Assert.assertEquals(new ShardsRange(7, 9), ranges.get(2));
        Assert.assertEquals(new ShardsRange(11, 11), ranges.get(3));
    }
}
