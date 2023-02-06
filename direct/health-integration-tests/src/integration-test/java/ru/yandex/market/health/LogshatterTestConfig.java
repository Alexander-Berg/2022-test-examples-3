package ru.yandex.market.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlServiceOld;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;
import ru.yandex.market.logshatter.config.ddl.shard.UpdateHostDDLTaskFactory;
import ru.yandex.market.logshatter.useragent.FakeUserAgentDetector;

@Configuration
@Import({TestsCommonConfig.class})
public class LogshatterTestConfig {

    @Value("${database.name}")
    private String databaseName;

    @Value("${logshatter.zookeeper-prefix}")
    private String zookeeperPrefix;

    @Value("${logshatter.zookeeper-quorum}")
    private String zookeeperQuorum;

    @Value("${logshatter.default-sources}")
    private String defaultSources;

    @Value("${logshatter.conf.dir}")
    private String logshatterConfDir;

    @Autowired
    private ClickhouseTemplate clickhouseTemplate;

    @Autowired
    private ClickHouseSource clickHouseSource;

    @Autowired
    private LogShatterMonitoring monitoring;

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

        return new UpdateDDLService(clickHouseSource, updateHostDDLTaskFactory, clickhouseDdlService, 42);
    }


    @Bean
    public ConfigurationService configurationService() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigDir(logshatterConfDir);
        configurationService.setLogshatterZookeeperPrefix(zookeeperPrefix);
        configurationService.setZookeeperQuorum(zookeeperQuorum);
        configurationService.setDefaultClickHouseDatabase(databaseName);
        configurationService.setDefaultSource(defaultSources);
        configurationService.setMonitoring(monitoring);
        configurationService.setClickhouseDdlService(clickhouseDdlService());
        configurationService.setUpdateDDLService(updateDDLService());
        configurationService.setUserAgentDetector(new FakeUserAgentDetector());

        return configurationService;
    }
}
