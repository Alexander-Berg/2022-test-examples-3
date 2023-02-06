package ru.yandex.market.logshatter.config;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.StringValueResolver;

import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlServiceOld;
import ru.yandex.market.health.configs.clickhouse.parser.ClickHouseClusterParser;
import ru.yandex.market.health.configs.clickhouse.service.ClickHouseClusterConfigurationService;
import ru.yandex.market.health.configs.client.vault.HealthVaultService;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.EntityConverter;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 24.05.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServiceTest {
    private static final String PROJECT_ID = "testProject";
    private static final String CONFIG_TITLE = "test_title";
    private static final String ACTIVATED_USER = "testUser";
    private static final String CLUSTER_NAME = "test_health";
    public static final int DATA_ROTATION_DAYS = 30;
    private static final String FAILED_TO_LOAD_CONFIG = "Failed to load configs: %s. " +
        "Search log for message 'Failed to load config <configId>/<configVersion> from mongo.'";
    private static final String FAILED_TO_FIND_CLUSTER_CONFIG = " Failed to find clickhouse cluster config for: %s. " +
        "Search log for message 'Failed to find clickHouse cluster config by cluster id'.";
    private static final String MDB_CLUSTER = "mdb_cluster";
    private static final String MDB_OTHER_CLUSTER = "mdb_other_cluster";

    @Mock
    StringValueResolver resolver;
    @Spy
    private ConfigurationService configurationService = new ConfigurationService();
    @Mock
    private LogshatterConfigDao configDaoMock;
    @Mock
    private HealthVaultService healthVaultServiceMock;
    @Mock
    private EntityConverter entityConverterMock;
    @Mock
    private UpdateDDLService updateDDLServiceMock;
    @Spy
    private final ClickHouseClusterConfigurationService clusterConfigurationService =
        new ClickHouseClusterConfigurationService(
            "cluster/conf.d", 10, new ClickHouseClusterParser(resolver), healthVaultServiceMock);

    @Captor
    ArgumentCaptor<List<VersionedConfigEntity.VersionEntity.Id>> failedConfigsCaptor;

    @Test(expected = IllegalStateException.class)
    public void checkTableNameFailsOnWrongTable() {
        ConfigurationService.checkTableName("torrent-client-perf");
    }

    @Test
    public void checkTableNameSuccessOnGoodTable() {
        ConfigurationService.checkTableName("torrent_client_perf");
        ConfigurationService.checkTableName("market.torrent_client_perf");
    }

    @Test
    public void readAndValidateConfigurationFromMongoWhenClusterConfigNotFound() {
        setConfigurationServiceFields();

        setMockBehavior(false);
        doReturn(MDB_CLUSTER).when(clusterConfigurationService).getClickHouseClusterName(eq(MDB_CLUSTER));

        List<LogShatterConfig> logShatterConfigs = configurationService.readAndValidateConfigurationFromMongo();
        assertEquals(2, logShatterConfigs.size());

        verify(configurationService).isMonitoringOk(failedConfigsCaptor.capture(), eq(FAILED_TO_LOAD_CONFIG));
        List<VersionedConfigEntity.VersionEntity.Id> actualFailedToLoadConfigs = failedConfigsCaptor.getValue();
        assertTrue(actualFailedToLoadConfigs.isEmpty());

        verify(configurationService).isMonitoringOk(failedConfigsCaptor.capture(), eq(FAILED_TO_FIND_CLUSTER_CONFIG));
        List<VersionedConfigEntity.VersionEntity.Id> actualNotFoundClusterConfigs = failedConfigsCaptor.getValue();
        assertEquals(1, actualNotFoundClusterConfigs.size());
    }

    @Test
    public void readAndValidateConfigurationFromMongoWhenClusterConfigsExists() {
        setConfigurationServiceFields();

        setMockBehavior(false);
        doReturn(MDB_CLUSTER).when(clusterConfigurationService).getClickHouseClusterName(anyString());

        List<LogShatterConfig> logShatterConfigs = configurationService.readAndValidateConfigurationFromMongo();
        assertEquals(3, logShatterConfigs.size());

        verify(configurationService).isMonitoringOk(failedConfigsCaptor.capture(), eq(FAILED_TO_FIND_CLUSTER_CONFIG));
        List<VersionedConfigEntity.VersionEntity.Id> actualNotFoundClusterConfigs = failedConfigsCaptor.getValue();
        assertTrue(actualNotFoundClusterConfigs.isEmpty());
    }

    @Test
    public void readAndValidateConfigurationFromMongoWhenAllConfigsFailed() {
        setConfigurationServiceFields();

        setMockBehavior(true);

        List<LogShatterConfig> logShatterConfigs = configurationService.readAndValidateConfigurationFromMongo();
        assertEquals(0, logShatterConfigs.size());

        verify(configurationService).isMonitoringOk(failedConfigsCaptor.capture(), eq(FAILED_TO_LOAD_CONFIG));
        List<VersionedConfigEntity.VersionEntity.Id> actualFailedToLoadConfigs = failedConfigsCaptor.getValue();
        assertEquals(1, actualFailedToLoadConfigs.size());

        String failedToLoadConfigMessage = "Failed to load configs: test_clicks/1. Search log for message 'Failed to " +
            "load config <configId>/<configVersion> from mongo.'" + FAILED_TO_FIND_CLUSTER_CONFIG;

        verify(configurationService).isMonitoringOk(failedConfigsCaptor.capture(), eq(failedToLoadConfigMessage));
        List<VersionedConfigEntity.VersionEntity.Id> actualNotFoundClusterConfigs = failedConfigsCaptor.getValue();
        assertEquals(2, actualNotFoundClusterConfigs.size());
    }

    private void setMockBehavior(boolean withoutClusterId) {
        when(configDaoMock.getActiveConfigs()).thenReturn(buildInitialLogshatterConfigEntities());
        when(updateDDLServiceMock.getClickhouseDdlService()).thenReturn(buildDdlService());
        try {
            if (withoutClusterId) {
                doThrow(RuntimeException.class).when(entityConverterMock)
                    .entityToConfig(any(LogshatterConfigVersionEntity.class), any(), eq(CLUSTER_NAME));
            } else {
                when(entityConverterMock.entityToConfig(any(LogshatterConfigVersionEntity.class),
                    any(), eq(CLUSTER_NAME)))
                    .thenReturn(buildLogshatterConfig().get(0));
            }
        } catch (Exception ignore) {
        }
    }

    @Test
    public void isMonitoringOkTest() {
        boolean isMonitoringOk = configurationService.isMonitoringOk(Collections.emptyList(), FAILED_TO_LOAD_CONFIG);
        assertTrue(isMonitoringOk);

        isMonitoringOk =
            configurationService.isMonitoringOk(Collections.singletonList(buildConfigCurrentVersions().get(0).getId()),
                FAILED_TO_LOAD_CONFIG);
        assertFalse(isMonitoringOk);
    }


    private void setConfigurationServiceFields() {
        configurationService.setConfigDao(configDaoMock);
        configurationService.setClusterConfigurationService(clusterConfigurationService);
        configurationService.setEntityConverter(entityConverterMock);
        configurationService.setUpdateDDLService(updateDDLServiceMock);
        configurationService.setEnableClusterConfigsLoadingMonitoring(true);
        configurationService.setEnableExternalClickHouseClusterConfigs(true);
    }

    private List<LogShatterConfig> buildLogshatterConfig() {
        return Arrays.asList(
            LogShatterConfig.newBuilder().setConfigId("1").setDataRotationDays(DATA_ROTATION_DAYS).build(),
            LogShatterConfig.newBuilder().setConfigId("2").setDataRotationDays(DATA_ROTATION_DAYS).build()
        );
    }

    private List<LogshatterConfigEntity> buildInitialLogshatterConfigEntities() {
        List<LogshatterConfigVersionEntity> configVersions = buildConfigCurrentVersions();
        return Arrays.asList(
            new LogshatterConfigEntity("test_offers", PROJECT_ID, CONFIG_TITLE, null, Instant.now(), Instant.now(),
                configVersions.get(0), ACTIVATED_USER),
            new LogshatterConfigEntity("test_orders", PROJECT_ID, CONFIG_TITLE, null, Instant.now(), Instant.now(),
                configVersions.get(1), ACTIVATED_USER),
            new LogshatterConfigEntity("test_clicks", PROJECT_ID, CONFIG_TITLE, null, Instant.now(), Instant.now(),
                configVersions.get(2), ACTIVATED_USER)
        );
    }

    private List<LogshatterConfigVersionEntity> buildConfigCurrentVersions() {
        return Arrays.asList(
            new LogshatterConfigVersionEntity(new VersionedConfigEntity.VersionEntity.Id("test_offers", 1L),
                VersionedConfigSource.UI, VersionStatus.PUBLIC, null, null, null, null, DATA_ROTATION_DAYS,
                MDB_CLUSTER, null, null, null, null, null),
            new LogshatterConfigVersionEntity(new VersionedConfigEntity.VersionEntity.Id("test_orders", 1L),
                VersionedConfigSource.UI, VersionStatus.PUBLIC, null, null, null, null, DATA_ROTATION_DAYS,
                MDB_OTHER_CLUSTER, null, null, null, null, null),
            new LogshatterConfigVersionEntity(new VersionedConfigEntity.VersionEntity.Id("test_clicks", 1L),
                VersionedConfigSource.UI, VersionStatus.PUBLIC, null, null, null, null, DATA_ROTATION_DAYS, null,
                null, null, null, null, null)
        );
    }

    private ClickHouseDdlServiceOld buildDdlService() {
        ClickHouseSource source = new ClickHouseSource();
        source.setCluster(CLUSTER_NAME);
        ClickHouseDdlServiceOld clickHouseDdlService = new ClickHouseDdlServiceOld();
        clickHouseDdlService.setClickHouseSource(source);
        return clickHouseDdlService;
    }

}
