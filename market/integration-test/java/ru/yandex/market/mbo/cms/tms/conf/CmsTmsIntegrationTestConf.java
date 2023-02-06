package ru.yandex.market.mbo.cms.tms.conf;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.application.properties.etcd.EtcdClient;
import ru.yandex.market.application.properties.etcd.resources.RemoteEtcdResource;
import ru.yandex.market.mbo.cms.config.configs.CmsIntegrationTestConf;
import ru.yandex.market.mbo.cms.tms.config.CmsTmsConfig;

/**
 * @author commince
 * @since 23.03.2018
 */
@Configuration
@Import({CmsTmsConfig.class, CmsIntegrationTestConf.class, })
public class CmsTmsIntegrationTestConf {

    private static final String ETCD_DATASOURCES = "/datasources/development/yandex/market-datasources/" +
            "datasources.properties";

    @Bean
    PropertyPlaceholderConfigurer propertyConfigurer(EtcdClient etcdClient) {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(
                new RemoteEtcdResource(ETCD_DATASOURCES, etcdClient),
                new ClassPathResource("/test.properties")
        );
        return configurer;
    }
}
