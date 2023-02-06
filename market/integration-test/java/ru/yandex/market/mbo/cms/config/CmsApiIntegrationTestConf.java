package ru.yandex.market.mbo.cms.config;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.application.properties.etcd.EtcdClient;
import ru.yandex.market.application.properties.etcd.resources.RemoteEtcdResource;
import ru.yandex.market.mbo.cms.api.config.CmsApiConf;
import ru.yandex.market.mbo.cms.api.servlets.bean.RequestHelper;
import ru.yandex.market.mbo.cms.config.configs.CmsIntegrationTestConf;

/**
 * @author commince
 */
@Configuration
@Import({CmsApiConf.class, CmsIntegrationTestConf.class, })
public class CmsApiIntegrationTestConf {

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

    @Bean
    public RequestHelper requestHelper() {
        return new RequestHelper();
    }
}
