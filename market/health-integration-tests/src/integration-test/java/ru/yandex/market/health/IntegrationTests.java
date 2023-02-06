package ru.yandex.market.health;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.DdlQuery;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.EntityConverter;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.config.LogSource;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntegrationTests extends IntegrationTestsBase {
    private static final String CONFIG_ID_1 = "config_id_1";
    private static final String CONFIG_ID_2 = "config_id_2";
    private static final String CONFIG_ID_3 = "config_id_3";
    private static final String TEST_CLUSTER_NAME = "test_shard_localhost";

    @Test
    public void test1Logshatter() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        initLogshatterContextAndTables(context);
        LogShatterMonitoring monitoring = context.getBean(LogShatterMonitoring.class);
        ComplicatedMonitoring.Result result = monitoring.getOverallResult();
        Assert.assertFalse(result.getMessage(), result.getStatus() == MonitoringStatus.CRITICAL);

        repeatedDdlUpdate(context, "configurationService");
    }

    @Test
    public void testLogshatterAndClusterConfigs() throws Exception {
        removeLogshatterLeaderZkNode();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LogshatterTestConfig.class);
        LogshatterConfigDao configDao = context.getBean(LogshatterConfigDao.class);
        EntityConverter entityConverter = context.getBean(EntityConverter.class);
        List<LogshatterConfigEntity> logshatterConfigEntities = buildLogshatterConfigEntitiesForPositiveTest();
        List<LogShatterConfig> logShatterConfigs = buildLogshatterConfigsForPositiveTest();
        when(configDao.getActiveConfigs()).thenReturn(logshatterConfigEntities);
        when(entityConverter.entityToConfig(eq(logshatterConfigEntities.get(0).getCurrentVersion()), eq("test_shard_localhost")))
            .thenReturn(logShatterConfigs.get(0));
        when(entityConverter.entityToConfig(eq(logshatterConfigEntities.get(1).getCurrentVersion()), eq("test_shard_localhost")))
            .thenReturn(logShatterConfigs.get(1));
        when(entityConverter.entityToConfig(eq(logshatterConfigEntities.get(2).getCurrentVersion()), isNull()))
            .thenReturn(logShatterConfigs.get(2));

        // Ждём окончания накатывания DDL
        context.getBean("configurationServiceWithNewConfigLoading", ru.yandex.market.logshatter.config.ConfigurationService.class).getDDLWorker().awaitStatus();

        checkUpdateDDLMonitoringStatus(context, MonitoringStatus.OK, "DDL update");
        checkUpdateDDLMonitoringStatus(context, MonitoringStatus.OK, "External DDL update");
        repeatedDdlUpdate(context, "configurationServiceWithNewConfigLoading");

        dropDatabases(context.getBean(ClickhouseTemplate.class));
    }

    // Проверяем что повторное применение DDL не накатывает ничего по второму разу.
    private void repeatedDdlUpdate(AnnotationConfigApplicationContext context, String beanName) {
        List<DdlQuery> queries = context.getBean(UpdateDDLService.class)
            .updateDDL(context.getBean(beanName, ru.yandex.market.logshatter.config.ConfigurationService.class).getConfigs())
            .stream()
            .flatMap(ddl -> Stream.concat(ddl.getUpdates().stream(), ddl.getManualUpdates().stream()))
            .collect(Collectors.toList());
        if (!queries.isEmpty()) {
            Assert.fail(String.format("Some queries were applied twice.\n" +
                    "\n" +
                    "This test launched %s twice. During the second launch we expect no mutating queries to run\n" +
                    "because the first launch should have ran them all already.\n" +
                    "\n" +
                    "This is most likely a query normalization issue. Logshatter runs SHOW CREATE TABLE and\n" +
                    "compares the result with the query that it would run if the table didn't exist.\n" +
                    "SHOW CREATE TABLE returns a normalized query with parentheses and capital letters in all the\n" +
                    "right places. Logshatter doesn't know anything about the normalization rules, so sometimes\n" +
                    "it's necessary to manually edit configs and TableDefinition's to make sure that that\n" +
                    "Logshatter generates normalized queries.\n" +
                    "\n" +
                    "This command will show what queries Logshatter decided to run and why:\n" +
                    "  ./gradlew :health-integration-tests:integrationTest 2>&1 | grep 'Planned DDL'\n" +
                    "\n" +
                    "Queries that were applied twice:\n%s" +
                    "\n",
                UpdateDDLService.class.getSimpleName(),
                queries.stream()
                    .map(DdlQuery::getQueryString)
                    .collect(Collectors.joining("\n  ", "  ", ""))
            ));
        }
    }

    /**
     * There are 2 mocked correct configs (one has localhost cluster config (testCluster.json) and other one without clusterId)
     * and one incorrect cluster config with wrong host cluster config (testWrongCluster.json).
     * As result is expected 2 tables in database created based on correct configs.
     * So failed cluster should not interrupt or block other cluster ddl updates.
     * MonitoringStatus should set to CRITICAL for external DDL update unit and OK for internal update unit.
     * @throws Exception
     */
    @Test
    public void testLogshatterAndCusterConfigsWithWrongHost() throws Exception {
        removeLogshatterLeaderZkNode();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LogshatterTestConfig.class);
        LogshatterConfigDao configDao = context.getBean(LogshatterConfigDao.class);
        EntityConverter entityConverter = context.getBean(EntityConverter.class);
        List<LogshatterConfigEntity> logshatterConfigEntities = buildLogshatterConfigEntitiesForNegativeTest();
        List<LogShatterConfig> logShatterConfigs = buildLogshatterConfigsForNegativeTest();
        when(configDao.getActiveConfigs()).thenReturn(logshatterConfigEntities);
        when(entityConverter.entityToConfig(eq(logshatterConfigEntities.get(0).getCurrentVersion()), eq("test_shard_localhost")))
            .thenReturn(logShatterConfigs.get(0));
        when(entityConverter.entityToConfig(eq(logshatterConfigEntities.get(1).getCurrentVersion()), isNull()))
            .thenReturn(logShatterConfigs.get(1));
        when(entityConverter.entityToConfig(eq(logshatterConfigEntities.get(2).getCurrentVersion()), isNull()))
            .thenReturn(logShatterConfigs.get(2));

        // Ждём окончания накатывания DDL
        try {
            context.getBean("configurationServiceWithNewConfigLoading", ru.yandex.market.logshatter.config.ConfigurationService.class).getDDLWorker().awaitStatus();
        } catch (Exception skip) {
        }

        checkUpdateDDLMonitoringStatus(context, MonitoringStatus.OK, "DDL update");
        checkUpdateDDLMonitoringStatus(context, MonitoringStatus.CRITICAL, "External DDL update");

        // засыпаем, чтобы успела создаться необходимая база с таблицами в ClickHouse
        Thread.sleep(3000);

        int actualCount = context.getBean(ClickhouseTemplate.class).queryForInt(
            "SELECT count() " +
                "FROM system.tables " +
                "WHERE database = 'market' and name LIKE 'neg_test_cluster%'");
        Assert.assertEquals(2, actualCount);
    }

    private void checkUpdateDDLMonitoringStatus(AnnotationConfigApplicationContext context,
                                                MonitoringStatus expectedStatus,
                                                String monitoringUnitName) {
        LogShatterMonitoring bean = context.getBean(LogShatterMonitoring.class);
        MonitoringUnit ddlUpdateUnit = bean.getHostCritical().getOrAddUnit(monitoringUnitName);
        Assert.assertEquals(expectedStatus, ddlUpdateUnit.getStatus());
    }

    @Test
    public void test2Clickphite() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        initLogshatterContextAndTablesMinConfig(context);
        context = createClickphiteContext();

        ConfigurationService configurationService = context.getBean(ConfigurationService.class);
        configurationService.getConfiguration();

        ComplicatedMonitoring monitoring = context.getBean(ComplicatedMonitoring.class);
        ComplicatedMonitoring.Result result = monitoring.getResult();
        Assert.assertEquals(result.getMessage(), MonitoringStatus.OK, result.getStatus());

        dropDatabases(context.getBean(ClickhouseTemplate.class));
    }

    private List<LogShatterConfig> buildLogshatterConfigsForPositiveTest() {
        String databaseName = "market";

        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("test1", ColumnType.String),
                new Column("test2", ColumnType.String)
            ));

        ClickHouseTableDefinition notDistributedTableDefinition1 = getClickHouseTableDefinition(databaseName, "test_cluster_1", tableDescription);
        ClickHouseTableDefinition notDistributedTableDefinition2 = getClickHouseTableDefinition(databaseName, "test_cluster_2", tableDescription);
        ClickHouseTableDefinition notDistributedTableDefinition3 = getClickHouseTableDefinition(databaseName, "test_cluster_3", tableDescription);
        ClickHouseTableDefinition notDistributedTableDefinition4 = getClickHouseTableDefinition(databaseName, "test_cluster_4", tableDescription);

        LogShatterConfig configForNotDistributedTable1;
        LogShatterConfig configForNotDistributedTable2;
        LogShatterConfig configForNotDistributedTable3;
        LogShatterConfig configForNotDistributedTable4;

        try {
            configForNotDistributedTable1 = getLogShatterConfig(notDistributedTableDefinition1, TEST_CLUSTER_NAME);
            configForNotDistributedTable2 = getLogShatterConfig(notDistributedTableDefinition2, TEST_CLUSTER_NAME);
            configForNotDistributedTable3 = getLogShatterConfig(notDistributedTableDefinition3, null);
            configForNotDistributedTable4 = getLogShatterConfig(notDistributedTableDefinition4, "wrong_cluster");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Arrays.asList(configForNotDistributedTable1, configForNotDistributedTable2, configForNotDistributedTable3, configForNotDistributedTable4);
    }

    private List<LogShatterConfig> buildLogshatterConfigsForNegativeTest() {
        String databaseName = "market";

        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("test1", ColumnType.String),
                new Column("test2", ColumnType.String)
            ));

        ClickHouseTableDefinition notDistributedTableDefinition1 = getClickHouseTableDefinition(databaseName, "neg_test_cluster_1", tableDescription);
        ClickHouseTableDefinition notDistributedTableDefinition2 = getClickHouseTableDefinition(databaseName, "neg_test_cluster_2", tableDescription);
        ClickHouseTableDefinition notDistributedTableDefinition3 = getClickHouseTableDefinition(databaseName, "neg_test_cluster_3", tableDescription);

        LogShatterConfig configForNotDistributedTable1;
        LogShatterConfig configForNotDistributedTable2;
        LogShatterConfig configForNotDistributedTable3;

        try {
            configForNotDistributedTable1 = getLogShatterConfig(notDistributedTableDefinition1, TEST_CLUSTER_NAME);
            configForNotDistributedTable2 = getLogShatterConfig(notDistributedTableDefinition2, null);
            configForNotDistributedTable3 = getLogShatterConfig(notDistributedTableDefinition3, "wrong_cluster");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Arrays.asList(configForNotDistributedTable1, configForNotDistributedTable2, configForNotDistributedTable3);
    }

    private ClickHouseTableDefinition getClickHouseTableDefinition(String databaseName,
                                                                   String tableName,
                                                                   TableDescription tableDescription) {
        return new ClickHouseTableDefinitionImpl(
            databaseName,
            tableName,
            tableDescription.getColumns(),
            tableDescription.getEngine()
        );
    }

    private LogShatterConfig getLogShatterConfig(ClickHouseTableDefinition tableDefinition, String clusterId) throws ConfigValidationException {
        LogShatterConfig configForNotDistributedTable1;
        configForNotDistributedTable1 = LogShatterConfig.newBuilder()
            .setSources(Collections.singletonList(LogSource.create("logbroker://test--market-log")))
            .setConfigId("test_id")
            .setDataClickHouseTable(tableDefinition)
            .setDistributedClickHouseTable(null)
            .setClickHouseClusterId(clusterId)
            .build();
        return configForNotDistributedTable1;
    }

    private List<LogshatterConfigEntity> buildLogshatterConfigEntitiesForPositiveTest() {
        LogshatterConfigVersionEntity versionWithClusterId1 = getLogshatterConfigVersionEntity(CONFIG_ID_1, TEST_CLUSTER_NAME);
        LogshatterConfigVersionEntity versionWithClusterId2 = getLogshatterConfigVersionEntity(CONFIG_ID_2, TEST_CLUSTER_NAME);
        LogshatterConfigVersionEntity versionWithoutClusterId = getLogshatterConfigVersionEntity(CONFIG_ID_3, null);
        LogshatterConfigEntity configWithClusterId1 = getLogshatterConfigEntity(versionWithClusterId1, CONFIG_ID_1);
        LogshatterConfigEntity configWithClusterId2 = getLogshatterConfigEntity(versionWithClusterId2, CONFIG_ID_2);
        LogshatterConfigEntity configWithClusterId3 = getLogshatterConfigEntity(versionWithoutClusterId, CONFIG_ID_3);
        return Arrays.asList(configWithClusterId1, configWithClusterId2, configWithClusterId3);
    }

    private List<LogshatterConfigEntity> buildLogshatterConfigEntitiesForNegativeTest() {
        LogshatterConfigVersionEntity versionWithClusterId1 = getLogshatterConfigVersionEntity(CONFIG_ID_1, TEST_CLUSTER_NAME);
        LogshatterConfigVersionEntity versionWithoutClusterId = getLogshatterConfigVersionEntity(CONFIG_ID_2, null);
        LogshatterConfigVersionEntity versionWithClusterId3 = getLogshatterConfigVersionEntity(CONFIG_ID_3, "wrong_cluster");
        LogshatterConfigEntity configWithClusterId1 = getLogshatterConfigEntity(versionWithClusterId1, CONFIG_ID_1);
        LogshatterConfigEntity configWithClusterId2 = getLogshatterConfigEntity(versionWithoutClusterId, CONFIG_ID_2);
        LogshatterConfigEntity configWithClusterId3 = getLogshatterConfigEntity(versionWithClusterId3, CONFIG_ID_3);
        return Arrays.asList(configWithClusterId1, configWithClusterId2, configWithClusterId3);
    }

    @NotNull
    private LogshatterConfigEntity getLogshatterConfigEntity(LogshatterConfigVersionEntity versionWithClusterId1, String configId) {
        return new LogshatterConfigEntity(
            configId,
            null,
            "Test",
            null,
            Instant.now(),
            Instant.now(),
            versionWithClusterId1,
            null
        );
    }

    @NotNull
    private LogshatterConfigVersionEntity getLogshatterConfigVersionEntity(String configId, String clusterName) {
        return new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id(configId, 0L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            null,
            null,
            null,
            null,
            null,
            clusterName,
            null,
            null,
            null,
            null,
            null
        );
    }
}
