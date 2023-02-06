package ru.yandex.market.supercontroller.mbologs.conf;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import ru.yandex.market.application.properties.etcd.EtcdClient;
import ru.yandex.market.application.properties.etcd.EtcdClientContext;
import ru.yandex.market.application.properties.etcd.grpc.EtcdClientImpl;
import ru.yandex.market.application.properties.etcd.grpc.EtcdClientImplConfig;
import ru.yandex.market.application.properties.etcd.resources.RemoteEtcdResource;

import java.util.List;

/**
 * @author moskovkin
 */
@Configuration
public class MboLogsIntegrationPropertiesConfig {

    private static final String ETCD_PREFIX = "/datasources/development/yandex/market-datasources/";

    @Bean
    @Primary
    PropertyPlaceholderConfigurer placeholderConfigurer() {

        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        configurer.setLocations(
            new RemoteEtcdResource(ETCD_PREFIX + "datasources.properties", etcdClient()),
            new RemoteEtcdResource(ETCD_PREFIX + "mbo/mbo-search.properties", etcdClient()),
            new ClassPathResource("mbo-logs/integration/datasources.properties"),
            new ClassPathResource("mbo-logs/integration/mbo-logs-integration.properties")
        );
        return configurer;
    }

    @Bean
    EtcdClient etcdClient() {
        String username = EtcdClientContext.getUsername();
        String password = EtcdClientContext.getPassword();
        List<String> endpoints = EtcdClientContext.getEndpoints();
        String authority = EtcdClientContext.getAuthority();

        EtcdClientImplConfig config = new EtcdClientImplConfig()
            .setUsername(username)
            .setPassword(password)
            .setEndpoints(endpoints)
            .setAuthority(authority);
        return new EtcdClientImpl(config);
    }

}
