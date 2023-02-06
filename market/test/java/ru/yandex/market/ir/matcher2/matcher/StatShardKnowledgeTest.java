package ru.yandex.market.ir.matcher2.matcher;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.ir.io.StatShardKnowledge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class StatShardKnowledgeTest {
    private StatShardKnowledge statShardKnowledge;

    @Before
    public void setUp() {
        statShardKnowledge = new StatShardKnowledge(true, 1, shardId -> Set.of(18540110, 91491, 90796));
        statShardKnowledge.reloadShardCategoryIds();
    }

    @Test
    public void isCategoryInShardTest() {
        assertTrue(statShardKnowledge.isCategoryInShard(18540110));
        assertTrue(statShardKnowledge.isCategoryInShard(91491));
        assertTrue(statShardKnowledge.isCategoryInShard(90796));
        assertFalse(statShardKnowledge.isCategoryInShard(91259));
        assertFalse(statShardKnowledge.isCategoryInShard(91529));
    }

    @Test
    public void isFileInShardTest() {
        assertTrue(statShardKnowledge.isFileInShard("category_91491.pb"));
        assertFalse(statShardKnowledge.isFileInShard("test.pb"));
        assertFalse(statShardKnowledge.isFileInShard("parameter_987654321.pb"));
    }
}
