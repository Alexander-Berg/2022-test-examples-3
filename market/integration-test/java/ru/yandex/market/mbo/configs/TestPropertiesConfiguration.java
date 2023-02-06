package ru.yandex.market.mbo.configs;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import ru.yandex.market.application.properties.etcd.EtcdClient;
import ru.yandex.market.application.properties.etcd.EtcdClientConfiguration;
import ru.yandex.market.application.properties.etcd.resources.RemoteEtcdResource;

/**
 * @author s-ermakov
 */
@Configuration
@Import(EtcdClientConfiguration.class)
public class TestPropertiesConfiguration {

    private static final String ETCD_DATASOURCES = "/datasources/development/yandex/market-datasources/" +
        "datasources.properties";

    private static final String SEARCH_DATASOURCES = "/datasources/development/yandex/market-datasources/" +
        "mbo/mbo-search.properties";

    @Bean
    PropertyPlaceholderConfigurer placeholderConfigurer(EtcdClient etcdClient) {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(
            new RemoteEtcdResource(ETCD_DATASOURCES, etcdClient),
            new RemoteEtcdResource(SEARCH_DATASOURCES, etcdClient),
            new ClassPathResource("/mbo-core/test-db.properties"),
            new ClassPathResource("/mbo-core/test-saas.properties"),
            new ClassPathResource("/mbo-core/test-common.properties"),
            new ClassPathResource("/mbo-core/test-model-storage.properties"),
            new ClassPathResource("/mbo-core/custom-configs/yt/common-yt-test.properties")
        );
        return configurer;
    }
}
