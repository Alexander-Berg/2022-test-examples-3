package ru.yandex.market.checkout.common.tasks;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooKeeperMain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfigService;
import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfigsHolder;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.storage.ZooScriptExecutor;
import ru.yandex.market.common.zk.ZooClient;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AbstractZooTaskTest.TestConfiguration.class})
public abstract class AbstractZooTaskTest {

    public static final String ZOO_TASK_NAME = "TestTask";

    @Autowired
    protected TestableClock testableClock;
    @Autowired
    protected ZooTask zooTask;
    @Autowired
    protected ZooTaskConfigService zooTaskConfigService;

    @BeforeEach
    void configure() {
        zooTask.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));
    }

    @Configuration
    public static class TestConfiguration {

        @Bean(initMethod = "init")
        @DependsOn("testingServer")
        public ZooClient zooClient(TestingServer testingServer) {
            ZooClient zooClient = new ZooClient();
            zooClient.setConnectString(testingServer.getConnectString());
            zooClient.setConnectTimeout(1000);
            return zooClient;
        }

        @Bean
        public Clock testableClock() {
            return TestableClock.getInstance();
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public AutoRegisteringEnableAwareTask testTask(Clock clock,
                                                       ZooClient zooClient) {
            AutoRegisteringEnableAwareTask zooTask = new AutoRegisteringEnableAwareTask();
            zooTask.setClock(clock);
            zooTask.setZooClient(zooClient);
            zooTask.setNodePath("/some/path");
            zooTask.setRunnable(new ZooTaskRunnable() {
                @Override
                public void run(ZooTask task, CancellationToken cancellationToken) {
                    Slf4jZooTaskLog.putAttribute("key", "value");
                    Slf4jZooTaskLog.putAttribute("key2", "value2");
                }

                @Nonnull
                @Override
                public String getName() {
                    return ZOO_TASK_NAME;
                }
            });
            zooTask.setRepeatPeriod(1L);
            zooTask.setPostponePeriod(1L);
            zooTask.setPeriodUnit(TimeUnit.SECONDS);
            zooTask.setCrucial(false);
            zooTask.setFailPeriodFactor(2);
            zooTask.init();
            return zooTask;
        }

        @Bean(initMethod = "start", destroyMethod = "close")
        public TestingServer testingServer() throws Exception {
            return new TestingServer(false);
        }

        @Bean(initMethod = "start")
        public CuratorFramework curatorFramework(TestingServer testingServer) {
            return CuratorFrameworkFactory.newClient(testingServer.getConnectString(), new RetryOneTime(1000));
        }

        @Bean
        @DependsOn("testingServer")
        public ZooScriptExecutor zooScriptExecutor(CuratorFramework curatorFramework) throws Exception {
            String script = Joiner.on("\n").join(
                    "create /some ''",
                    "create /some/path ''",
                    "create /zoo-runtime-config-path {}"
            );

            ByteArrayResource resource = new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8));

            return new ZooScriptExecutor(new ZooKeeperMain(curatorFramework.getZookeeperClient().getZooKeeper()),
                    resource);
        }

        @Bean
        @DependsOn("testingServer")
        public ZooTaskConfigService zooTaskConfigService(
                CuratorFramework curatorFramework,
                ApplicationEventPublisher publisher,
                ZooTaskRegistry taskRegistry) {
            return new ZooTaskConfigsHolder(curatorFramework, "/zoo-runtime-config-path", publisher, taskRegistry);
        }

        @Bean
        public ZooTaskRegistry zooTaskRegistry(List<ZooTask> tasks) {
            return new ZooTaskRegistry(tasks);
        }
    }
}
