package ru.yandex.market.delivery.mdbapp.configuration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.market.delivery.mdbapp.components.curator.managers.ZkEventIdManager;

@Profile("integration-test")
@Configuration
public class ZookeeperConfig {
    @Bean(initMethod = "start")
    public TestingServer testZK() throws Exception {
        return new TestingServer();
    }

    public static class ZkCleanListener extends AbstractTestExecutionListener {
        @Override
        public void beforeTestMethod(TestContext testContext) throws Exception {
            CuratorFramework curatorFramework = testContext.getApplicationContext().getBean(CuratorFramework.class);
            for (String c : curatorFramework.getChildren().forPath(ZkEventIdManager.LAST_EVENT_ID_PATH_BASE)) {
                curatorFramework.delete().forPath(ZkEventIdManager.LAST_EVENT_ID_PATH_BASE + "/" + c);
            }
        }
    }
}
