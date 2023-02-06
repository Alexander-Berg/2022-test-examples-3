package ru.yandex.market.clickhouse.dealer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.clickhouse.dealer.config.DealerClusterConfigParser;
import ru.yandex.market.clickhouse.dealer.config.DealerConfigParser;
import ru.yandex.market.clickhouse.dealer.config.DealerConfigurationService;
import ru.yandex.market.clickhouse.dealer.config.DealerGlobalConfig;
import ru.yandex.market.clickhouse.dealer.spring.DealerConfigurationSpringConfig;

@Configuration
public class DealerConfigurationSpringTestingConfig extends DealerConfigurationSpringConfig {
    private static final String CONFIGS_PATH = "market/infra/market-health/clickhouse-dealer-market-configs/";

    @Override
    public DealerGlobalConfig globalConfig(
        DealerClusterConfigParser clusterConfigParser,
        ComplexMonitoring monitoring,
        @Value("${dealer.clusters.config.dir}") String clusterConfigDir,
        @Value("${dealer.clickhouse.temp-database:}") String tempDatabase,
        @Value("${dealer.tm.queue-name:}") String tmQueueName,
        @Value("${dealer.config.defaultRotationPeriodDays:-1}") int defaultRotationPeriodDays,
        @Value("${dealer.config.onlyActiveParsing:true}") boolean defaultConfigFileParsingState
    ) {
        String clusterConfigDirPath = Paths.getSourcePath(CONFIGS_PATH + clusterConfigDir);
        return super.globalConfig(
            clusterConfigParser, monitoring, clusterConfigDirPath, tempDatabase, tmQueueName,
            defaultRotationPeriodDays, defaultConfigFileParsingState
        );
    }

    @Override
    public DealerConfigurationService configurationService(
        DealerConfigParser configParser,
        ComplexMonitoring monitoring,
        @Value("${dealer.config.dir}") String configDir,
        @Value("${dealer.config.whitelist:}") String whitelist,
        @Value("${dealer.thread-count:100}") int threadCount
    ) {
        String configDirPath = Paths.getSourcePath(CONFIGS_PATH + configDir);
        return super.configurationService(configParser, monitoring, configDirPath, whitelist, threadCount);
    }
}
