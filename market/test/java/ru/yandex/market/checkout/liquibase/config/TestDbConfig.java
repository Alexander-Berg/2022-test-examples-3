package ru.yandex.market.checkout.liquibase.config;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.annotation.Nonnull;

import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeperMain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;

import ru.yandex.market.checkout.common.util.ZooPropertiesSetter;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
@Configuration
public class TestDbConfig {

    @Nonnull
    @Bean
    public PropertyPlaceholderConfigurer propertyConfigurer(@Nonnull
                                                            @Value("classpath:context/app-test-context.properties")
                                                                    Resource testProperties) {
        PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();

        propertyPlaceholderConfigurer.setLocations(testProperties);
        propertyPlaceholderConfigurer.setLocalOverride(true);

        return propertyPlaceholderConfigurer;
    }

    // Test DB provider
    @Nonnull
    @Bean
    public PreparedDbProvider databaseProvider() throws IOException {
        return PreparedDbProvider.forPreparer(
                (ds) -> {
                },
                List.of(
                        builder -> {
                            builder.setPGStartupWait(Duration.ofMinutes(1));
                            builder.setServerConfig("unix_socket_directories", "");
                        }
                )
        );
    }

    @Nonnull
    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curator(
            @Value("${market.checkout.zookeeper.connectString}") String connectString,
            @Value("${market.checkout.zookeeper.connectTimeout}") int connectTimeout
    ) {
        return CuratorFrameworkFactory.newClient(connectString, new ExponentialBackoffRetry(connectTimeout, 3));
    }

    // Zookeeper
    @Nonnull
    @Bean
    public ZooPropertiesSetter zooPropertiesSetter() {
        return new ZooPropertiesSetter();
    }

    @Nonnull
    @Bean(initMethod = "start", destroyMethod = "close")
    @DependsOn("zooPropertiesSetter")
    public TestingServer testZK() throws Exception {
        return new TestingServer(false);
    }

    @Nonnull
    @Bean
    public ZooKeeperMain zookeeperMain(@Nonnull
                                       @Value("#{curator.zookeeperClient.zooKeeper}")
                                               ZooKeeper zooKeeper) {
        return new ZooKeeperMain(zooKeeper);
    }
}
