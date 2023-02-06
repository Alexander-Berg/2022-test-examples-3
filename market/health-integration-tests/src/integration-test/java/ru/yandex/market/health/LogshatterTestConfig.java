package ru.yandex.market.health;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlServiceOld;
import ru.yandex.market.health.configs.clickhouse.config.ClusterConnectionConfig;
import ru.yandex.market.health.configs.clickhouse.parser.ClickHouseClusterParser;
import ru.yandex.market.health.configs.clickhouse.service.ClickHouseClusterConfigurationService;
import ru.yandex.market.health.configs.clickhouse.service.ClusterBalancedDataSourceFactory;
import ru.yandex.market.health.configs.clickhouse.service.ClusterDataSourceFactory;
import ru.yandex.market.health.configs.client.vault.HealthVaultService;
import ru.yandex.market.health.configs.logshatter.EntityConverter;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;
import ru.yandex.market.logshatter.config.ddl.UpdateDdlClusterService;
import ru.yandex.market.logshatter.config.ddl.shard.UpdateHostDDLTaskFactory;

@Configuration
@EnableScheduling
@Import({TestsCommonConfig.class, ClusterBalancedDataSourceFactory.class, ClusterDataSourceFactory.class})
public class LogshatterTestConfig {

    public static final String LOGSHATTER_CONF_PATH_PROP_NAME = "logshatter.conf.dir";

    @Value("${database.name}")
    private String databaseName;

    @Value("${logshatter.zookeeper-prefix}")
    private String zookeeperPrefix;

    @Value("${logshatter.zookeeper-quorum}")
    private String zookeeperQuorum;

    @Value("${logshatter.default-sources}")
    private String defaultSources;

    @Value("${" + LOGSHATTER_CONF_PATH_PROP_NAME + "}")
    private String logshatterConfDir;

    @Value("${mdb.clickhouse.conf.dir:}")
    private String clickHouseConfigDir;

    @Value("${mdb.clickhouse.period-between-config-updates-minutes:60}")
    private int updatePeriodInMinutes;

    @Autowired
    private ClickhouseTemplate clickhouseTemplate;

    @Autowired
    private ClickHouseSource clickHouseSource;

    @Autowired
    private LogShatterMonitoring monitoring;

    @Autowired
    private ClusterBalancedDataSourceFactory clusterBalancedDataSourceFactory;

    @Autowired
    private ClusterDataSourceFactory clusterDataSourceFactory;

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @Bean
    ClickHouseDdlServiceOld clickhouseDdlService() {
        ClickHouseDdlServiceOld clickhouseDdlService = new ClickHouseDdlServiceOld();
        clickhouseDdlService.setClickHouseSource(clickHouseSource);
        clickhouseDdlService.setClickhouseTemplate(clickhouseTemplate);

        return clickhouseDdlService;
    }

    @Bean
    public UpdateDDLService updateDDLService() {
        ClickHouseDdlServiceOld clickhouseDdlService = clickhouseDdlService();

        UpdateHostDDLTaskFactory updateHostDDLTaskFactory =
            new UpdateHostDDLTaskFactory(clickhouseDdlService, clickhouseTemplate);

        return new UpdateDDLService(clickHouseSource, updateHostDDLTaskFactory, clickhouseDdlService, 42,
            600, 10);
    }

    @Bean
    public ClusterConnectionConfig clusterConnectionConfig(
        @Value("${mdb.clickhouse.socket-timeout-seconds:180}") int socketTimeoutSeconds,
        @Value("${mdb.clickhouse.keep-alive-timeout-seconds:60}") int keepAliveTimeoutSeconds,
        @Value("${mdb.clickhouse.connection-timeout-millis:15000}") int connectionTimeoutMillis,
        @Value("${mdb.clickhouse.host-actualization-seconds:100}") int mdbHostActualizationDelayInSeconds) {
        return new ClusterConnectionConfig(
            socketTimeoutSeconds,
            keepAliveTimeoutSeconds,
            connectionTimeoutMillis,
            mdbHostActualizationDelayInSeconds);
    }

    @Bean
    public ClickHouseClusterParser clickHouseClusterParser() {
        return new ClickHouseClusterParser(new EmbeddedValueResolver(beanFactory));
    }

    @Bean
    public HealthVaultService mockVaultService() {
        return Mockito.mock(HealthVaultService.class);
    }

    @Bean
    @Lazy
    public ClickHouseClusterConfigurationService clusterConfigurationService() {
        return new ClickHouseClusterConfigurationService(
            clickHouseConfigDir,
            updatePeriodInMinutes,
            clickHouseClusterParser(),
            mockVaultService());
    }

    @Bean
    @Lazy
    public UpdateDdlClusterService updateDdlClusterService() {
        return new UpdateDdlClusterService(clusterBalancedDataSourceFactory, clusterDataSourceFactory,
            clusterConfigurationService());
    }

    @Bean
    public LogshatterConfigDao logshatterConfigDao() {
        return Mockito.mock(LogshatterConfigDao.class);
    }

    @Bean
    public EntityConverter entityConverter() {
        return Mockito.mock(EntityConverter.class);
    }

    @Bean
    @Lazy
    public ConfigurationService configurationService() {
        return getConfigurationService();
    }

    @Bean
    @Lazy
    public ConfigurationService configurationServiceWithNewConfigLoading() {
        ConfigurationService configurationService = getConfigurationService();
        configurationService.setUseNewConfigLoading(true);
        configurationService.setConfigDao(logshatterConfigDao());
        configurationService.setEntityConverter(entityConverter());
        configurationService.setClusterConfigurationService(clusterConfigurationService());

        return configurationService;
    }

    private ConfigurationService getConfigurationService() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigDir(logshatterConfDir);
        configurationService.setLogshatterZookeeperPrefix(zookeeperPrefix);
        configurationService.setZookeeperQuorum(zookeeperQuorum);
        configurationService.setDefaultClickHouseDatabase(databaseName);
        configurationService.setDefaultSource(defaultSources);
        configurationService.setMonitoring(monitoring);
        configurationService.setUpdateDDLService(updateDDLService());
        configurationService.setUserAgentDetector(new FakeUserAgentDetector());
        configurationService.setUpdateDdlTimeIntervalSeconds(600);
        configurationService.setUpdateDdlClusterService(updateDdlClusterService());
        configurationService.setEnableExternalClickHouseClusterConfigs(true);
        return configurationService;
    }
}
