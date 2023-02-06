package ru.yandex.market.stats.test.config;

import com.google.common.collect.ImmutableMap;
import org.apache.curator.test.TestingServer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;

public class LocalZookeeperInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>,
    ApplicationListener<ContextClosedEvent> {

    private TestingServer testingServer;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            testingServer = new TestingServer(-1);
            testingServer.start();

            ConfigurableEnvironment env = applicationContext.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("local-zookeeper",
                ImmutableMap.of("market.zookeeper.connectString", testingServer.getConnectString())
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (testingServer != null) {
            try {
                testingServer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
