package ru.yandex.market.clickhouse.dealer.config;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 07/06/2018
 */
public class DealerConfigTest {
    private static final String PASSWORD = "secret42";
    private DealerGlobalConfig emptyConfig = DealerGlobalConfig.newBuilder().build();

    @Test
    public void testNoPasswordLogging() {
        DealerConfig config = DealerConfig.newBuilder()
            .withGlobalConfig(emptyConfig)
            .withClickHouseTmCluster("cluster", "user", PASSWORD)
            .withOrderBy("a")
            .withPartitionBy("a")
            .withTableName("db.table")
            .withColumns(Collections.emptyList())
            .build();

        Assert.assertFalse(config.toString().contains(PASSWORD));
        Assert.assertFalse(config.getClickHouseCluster().toString().contains(PASSWORD));
    }

    @Test
    public void testNoDbaasTokenLogging() {
        DealerConfig config = DealerConfig.newBuilder()
            .withGlobalConfig(emptyConfig)
            .withClickHouseDbaasCluster("cluster", "user", PASSWORD, PASSWORD)
            .withOrderBy("a")
            .withPartitionBy("a")
            .withTableName("db.table")
            .withColumns(Collections.emptyList())
            .build();

        Assert.assertFalse(config.toString().contains(PASSWORD));
        Assert.assertFalse(config.getClickHouseCluster().toString().contains(PASSWORD));
    }
}
