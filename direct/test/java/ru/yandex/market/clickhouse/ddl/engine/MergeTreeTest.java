package ru.yandex.market.clickhouse.ddl.engine;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 15/06/2018
 */
public class MergeTreeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSampleBy() {
        new MergeTree("toYYYYMM(date)", Arrays.asList("a", "b"), "c");
    }

    @Test
    public void testDdlWithoutSample() {
        MergeTree mergeTree = new MergeTree("toYYYYMM(date)", Arrays.asList("a", "b"));
        Assert.assertEquals(
            mergeTree.createEngineDDL(),
            "MergeTree() PARTITION BY toYYYYMM(date) ORDER BY (a, b) SETTINGS index_granularity = 8192"
        );
    }

    @Test
    public void testDdlWithSample() {
        MergeTree mergeTree = new MergeTree("toYYYYMM(date)", Arrays.asList("a", "b"), "a");
        Assert.assertEquals(
            mergeTree.createEngineDDL(),
            "MergeTree() PARTITION BY toYYYYMM(date) ORDER BY (a, b) SAMPLE BY a SETTINGS index_granularity = 8192"
        );
    }

    @Test
    public void testOldFormat() {
        MergeTree mergeTree = MergeTree.fromOldDefinition("date", "a", "b");
        Assert.assertEquals(
            mergeTree.createEngineDDL(),
            "MergeTree() PARTITION BY toYYYYMM(date) ORDER BY (a, b) SETTINGS index_granularity = 8192"
        );
    }

    @Test
    public void testContainsColumn() {
        MergeTree mergeTree = new MergeTree("toYYYYMM(aaa)", Arrays.asList("intHash32(bbb)", "ccc"));
        Assert.assertTrue(mergeTree.containsColumn("aaa"));
        Assert.assertTrue(mergeTree.containsColumn("bbb"));
        Assert.assertTrue(mergeTree.containsColumn("ccc"));

        Assert.assertFalse(mergeTree.containsColumn("a"));
        Assert.assertFalse(mergeTree.containsColumn("b"));
        Assert.assertFalse(mergeTree.containsColumn("c"));
    }
}