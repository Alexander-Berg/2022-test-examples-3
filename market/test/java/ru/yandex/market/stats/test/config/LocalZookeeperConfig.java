package ru.yandex.market.stats.test.config;

import org.apache.curator.test.TestingServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author kormushin
 */
@Profile("integration-tests")
@Configuration
public class LocalZookeeperConfig {

    //-1 - random
    @Value("${zookeeper.port:-1}")
    private Integer zookeeperPort;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public TestingServer zookeeperServer() throws Exception {
        return new TestingServer(zookeeperPort);
    }
}
