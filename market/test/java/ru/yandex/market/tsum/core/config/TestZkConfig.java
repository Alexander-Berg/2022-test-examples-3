package ru.yandex.market.tsum.core.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.BindException;
import java.util.concurrent.TimeUnit;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.03.17
 */
@Configuration
public class TestZkConfig {
    @Bean
    public CuratorFramework curatorFramework() throws Exception {
        TestingServer server = testingServer();
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString("localhost:" + server.getPort())
            .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, (int) TimeUnit.SECONDS.toMillis(30)))
            .namespace("logshatter")
            .build();

        client.start();

        CuratorFrameworkState state = client.getState();
        if (!state.equals(CuratorFrameworkState.STARTED)) {
            throw new IllegalStateException("Curator framework hasn't started, current state: " + state);
        }

        return client;
    }

    @Bean
    public TestingServer testingServer() throws Exception {
        int tryNumber = 0;
        while (true) {
            try {
                return new TestingServer();
            } catch (BindException e) {
                if (tryNumber >= 3) {
                    throw e;
                }

                ++tryNumber;
            }
        }
    }
}
