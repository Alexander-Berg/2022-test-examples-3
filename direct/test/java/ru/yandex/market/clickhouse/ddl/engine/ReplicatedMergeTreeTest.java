package ru.yandex.market.clickhouse.ddl.engine;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/06/2018
 */
public class ReplicatedMergeTreeTest {
    @Test
    public void testDdl() {
        ReplicatedMergeTree replicatedMergeTree = new ReplicatedMergeTree(
            new MergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), "host", 8192),
            "/clickhouse/tables/{shard}/db.table_lr", "{replica}"
        );

        Assert.assertEquals(
            "ReplicatedMergeTree('/clickhouse/tables/{shard}/db.table_lr', '{replica}') " +
                "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SAMPLE BY host SETTINGS index_granularity = 8192",
            replicatedMergeTree.createEngineDDL()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSingleQuote1() {
        new ReplicatedMergeTree(
            new MergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), "host", 8192),
            "'/clickhouse/tables/{shard}/db.table_lr'", "{replica}"
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSingleQuote2() {
        new ReplicatedMergeTree(
            new MergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), "host", 8192),
            "/clickhouse/tables/{shard}/db.table_lr", "'{replica}'"
        );
    }
}
