package ru.yandex.market.mbo.audit.config;

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
public class IntegrationTestPropertyConfiguration {

    private static final String ETCD_DATASOURCES = "/datasources/development/yandex/market-datasources/" +
        "datasources.properties";

    @Bean
    PropertyPlaceholderConfigurer placeholderConfigurer(EtcdClient etcdClient) {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setSystemPropertiesMode(2);
        configurer.setLocations(
            new RemoteEtcdResource(ETCD_DATASOURCES, etcdClient),
            new ClassPathResource("/mbo-audit/integration-test.properties")
        );
        return configurer;
    }

    @Bean
    EtcdTnsnamesPostProcessor etcdTnsnamesPostProcessor() {
        return new EtcdTnsnamesPostProcessor();
    }
}
