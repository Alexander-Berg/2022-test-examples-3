package ru.yandex.market.tsum.core;

import org.apache.curator.test.TestingServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.net.BindException;
import java.util.Properties;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/11/16
 */
@Configuration
public class TestZooKeeper {

    @Bean(destroyMethod = "stop")
    public TestingServer zkTestingServer() throws Exception {
        int tryNumber = 0;
        while (true) {
            try {
                TestingServer server = new TestingServer();
                System.out.println("Server started");
                return server;
            } catch (BindException e) {
                if (tryNumber >= 10) {
                    throw e;
                }
                ++tryNumber;
            }
        }
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(TestingServer testingServer) throws Exception {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();

        properties.setProperty("tsum.zookeeper.quorum", testingServer.getConnectString());

        configurer.setProperties(properties);
        configurer.setLocalOverride(true);

        return configurer;
    }
}
