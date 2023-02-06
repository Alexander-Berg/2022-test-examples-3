package ru.yandex.market.stat.dicts.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.stat.metrics.MetricsConfig;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@Configuration
@Profile({"integration-tests"})
@Import({
        PropertiesDictionariesITestConfig.class,
        ParsersDictsConfig.class,
        YtDictionaryConfig.class,
        MetadataConfig.class,
        ZookeeperConfig.class,
        LoadersConfig.class,
        MarketHealthConfig.class,
        MetricsConfig.class,
        TvmConfig.class,
        AnaplanConfig.class,
        AnaplanLoadTasksConfig.class
})
@ComponentScan(value = {"ru.yandex.market.stat.dicts.services"})
public class DictionariesITestConfig {

}
