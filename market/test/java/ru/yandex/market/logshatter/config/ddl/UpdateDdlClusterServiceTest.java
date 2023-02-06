package ru.yandex.market.logshatter.config.ddl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.engine.DistributedEngine;
import ru.yandex.market.health.configs.clickhouse.config.ClusterConnectionConfig;
import ru.yandex.market.health.configs.clickhouse.service.ClickHouseClusterConfigurationService;
import ru.yandex.market.health.configs.clickhouse.service.ClusterBalancedDataSourceFactory;
import ru.yandex.market.health.configs.clickhouse.service.ClusterDataSourceFactory;
import ru.yandex.market.health.configs.clickhouse.spring.ClickHouseClusterSpringConfig;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.parser.TableDescription;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UpdateDdlClusterServiceTest .TestConfig.class})
@TestPropertySource(locations = "classpath:test.properties")
public class UpdateDdlClusterServiceTest {
    private static final String MDB_CLUSTER_NAME = "mdbqigl4m89hc106eli4";
    private static final String HEALTH_CLUSTER_NAME = "market_health";
    @Autowired
    ClickHouseClusterConfigurationService clickHouseClusterConfigurationService;
    @Autowired
    ClusterConnectionConfig clusterConnectionConfig;
    @Autowired
    ClusterBalancedDataSourceFactory balancedDataSourceFactory;
    @Autowired
    ClusterDataSourceFactory dataSourceFactory;
    @Autowired
    UpdateDdlClusterService updateDdlClusterService;

    /**
     * Has been created for local test and should be ignored on not local environments (testing, prestable, production).
     * For checking DDL update operations used real clusters existing at the time of creating this test.
     * See configs resources/clusterConfig/testRealHealthCluster.json and resources/clusterConfig/testRealMdbCluster.json
     * and modify in for your purposes.
     *
     * @throws InterruptedException thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted.
     */
    @Test
    public void test() throws InterruptedException {
        Thread.sleep(3000);
        Map<String, List<LogShatterConfig>> thirdPartyClusterConfigsMap = new HashMap<>();

        String healthDatabaseName = "market";

        String databaseName = "wms";
        String localTableName = "test_distr_lr";
        String tableName = "test";
        String distributedTableName = "test_distr";
        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("test1", ColumnType.String),
                new Column("test2", ColumnType.Int32)
            ));

        String clickHouseZookeeperTablePrefix = "/clickhouse/tables/{shard}";
        ClickHouseTableDefinition localTableDefinition = new ClickHouseTableDefinitionImpl(
            databaseName,
            localTableName,
            tableDescription.getColumns(),
            tableDescription.getEngine().replicated(databaseName, localTableName, clickHouseZookeeperTablePrefix)
        );

        ClickHouseTableDefinition distributedTableDefinition = new ClickHouseTableDefinitionImpl(
            databaseName,
            distributedTableName,
            tableDescription.getColumns(),
            new DistributedEngine(MDB_CLUSTER_NAME, databaseName, localTableName, "rand()"
            )
        );

        ClickHouseTableDefinition healthLocalTableDefinition = new ClickHouseTableDefinitionImpl(
            healthDatabaseName,
            localTableName,
            tableDescription.getColumns(),
            tableDescription.getEngine().replicated(healthDatabaseName, localTableName, clickHouseZookeeperTablePrefix)
        );
        ClickHouseTableDefinition healthDistributedTableDefinition = new ClickHouseTableDefinitionImpl(
            healthDatabaseName,
            distributedTableName,
            tableDescription.getColumns(),
            new DistributedEngine(HEALTH_CLUSTER_NAME, healthDatabaseName, localTableName, "rand()"
            )
        );

        ClickHouseTableDefinition notDistributedTableDefinition = new ClickHouseTableDefinitionImpl(
            databaseName,
            tableName,
            tableDescription.getColumns(),
            tableDescription.getEngine()
        );

        LogShatterConfig configForNotDistributedTable = LogShatterConfig.newBuilder()
            .setConfigId("test_config_1")
            .setDataClickHouseTable(notDistributedTableDefinition)
            .setDistributedClickHouseTable(null)
            .setClickHouseClusterId(MDB_CLUSTER_NAME)
            .build();

        LogShatterConfig configForDistributedTable = LogShatterConfig.newBuilder()
            .setConfigId("test_config_1")
            .setDataClickHouseTable(localTableDefinition)
            .setDistributedClickHouseTable(distributedTableDefinition)
            .setClickHouseClusterId(MDB_CLUSTER_NAME)
            .build();

        LogShatterConfig configForDistributedHealthTable = LogShatterConfig.newBuilder()
            .setConfigId("test_config_2")
            .setDataClickHouseTable(healthLocalTableDefinition)
            .setDistributedClickHouseTable(healthDistributedTableDefinition)
            .setClickHouseClusterId(HEALTH_CLUSTER_NAME)
            .build();

        thirdPartyClusterConfigsMap.put(MDB_CLUSTER_NAME, Arrays.asList(configForNotDistributedTable,
            configForDistributedTable));
        thirdPartyClusterConfigsMap.put(HEALTH_CLUSTER_NAME, Arrays.asList(configForDistributedHealthTable));

        updateDdlClusterService.updateDdlOnClusters(thirdPartyClusterConfigsMap);

        Thread.sleep(1000);
        updateDdlClusterService.updateDdlOnClusters(thirdPartyClusterConfigsMap);
    }

    @Configuration
    @EnableScheduling
    @Import(ClickHouseClusterSpringConfig.class)
    static class TestConfig {
        @Bean
        public UpdateDdlClusterService updateDdlClusterService(
            ClusterBalancedDataSourceFactory clusterBalancedDataSourceFactory,
            ClusterDataSourceFactory clusterDataSourceFactory,
            ClickHouseClusterConfigurationService clusterConfigurationService
        ) {
            return new UpdateDdlClusterService(clusterBalancedDataSourceFactory, clusterDataSourceFactory,
                clusterConfigurationService);
        }

    }

}
