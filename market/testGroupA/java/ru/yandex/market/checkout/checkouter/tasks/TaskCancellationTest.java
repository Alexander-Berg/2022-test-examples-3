package ru.yandex.market.checkout.checkouter.tasks;

import java.time.Clock;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.common.tasks.AutoRegisteringEnableAwareTask;
import ru.yandex.market.checkout.common.tasks.CancellationToken;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.common.tasks.ZooTaskRunnable;
import ru.yandex.market.common.zk.ZooClient;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
@ContextHierarchy(
        @ContextConfiguration(name = "task", classes = TaskCancellationTest.TestConfig.class)
)
public class TaskCancellationTest extends AbstractWebTestBase {

    @Autowired
    @Qualifier("testTaskCancellation")
    public AutoRegisteringEnableAwareTask testTask;
    @Autowired
    private CuratorFramework curator;

    @BeforeEach
    public void setUp() throws Exception {
        trustMockConfigurer.mockWholeTrust();

        Stat stat = curator.checkExists().forPath(testTask.getNodePath());
        if (stat == null) {
            curator.create().creatingParentsIfNeeded().forPath(testTask.getNodePath(), new byte[0]);
        }
    }

    @Test
    public void cancelTaskTest() {
        testTask.enable();

        Assertions.assertTrue(testTask.isEnabled());
        testTask.runOnce();

        testTask.disable();
        Assertions.assertFalse(testTask.isEnabled());

        testTask.enable();
        Assertions.assertTrue(testTask.isEnabled());
    }

    @Test
    public void cancelTaskTestAllIfPreviouslyWasEnabled() {
        testTask.enable();

        Assertions.assertTrue(testTask.isEnabled());
        Assertions.assertFalse(testTask.isDisabledAll());

        testTask.disableAll();

        Assertions.assertFalse(testTask.isEnabled());
        Assertions.assertTrue(testTask.isDisabledAll());

        testTask.enableAll();

        Assertions.assertTrue(testTask.isEnabled());
        Assertions.assertFalse(testTask.isDisabledAll());

        testTask.disableAll();

        Assertions.assertFalse(testTask.isEnabled());
        Assertions.assertTrue(testTask.isDisabledAll());
    }

    @Test
    public void cancelTaskTestAllIfPreviouslyWasDisabled() {
        testTask.disable();

        Assertions.assertFalse(testTask.isEnabled());
        Assertions.assertFalse(testTask.isDisabledAll());

        testTask.disableAll();

        Assertions.assertFalse(testTask.isEnabled());
        Assertions.assertFalse(testTask.isDisabledAll());

        // Включаем, но она еще должна быть выключена
        testTask.enableAll();

        Assertions.assertFalse(testTask.isEnabled());
        Assertions.assertFalse(testTask.isDisabledAll());

        // А вот теперь включена
        testTask.enable();

        Assertions.assertTrue(testTask.isEnabled());
        Assertions.assertFalse(testTask.isDisabledAll());
    }

    @Test
    public void cancelConsumerTaskEnableTest() throws Exception {
        QueueConsumer<String> testQueueConsumer = testTask.buildEnableAwareQueueConsumer(message -> {
        }, 20, 2L, 2L);

        testTask.enable();

        testQueueConsumer.consumeMessage(" ");
    }

    @Test
    public void cancelConsumerTaskDisableTest() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            QueueConsumer<String> testQueueConsumer = testTask.buildEnableAwareQueueConsumer(message -> {
            }, 20, 2L, 2L);

            testTask.disable();

            testQueueConsumer.consumeMessage(" ");
        });
    }

    @SpringBootConfiguration
    @TestConfiguration
    static class TestConfig {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Nonnull
        @Bean(name = "testTaskCancellation")
        public AutoRegisteringEnableAwareTask testTaskCancellation(
                ZooClient zooClient,
                Clock clock,
                @Value("${defaultEnabledOnHost:true}") boolean enabledOnHost,
                @Value("${market.checkouter.task.health.precondition.enabled:false}") boolean healthCheckEnabled,
                @Value("${market.checkout.tasks.OrderEventPublisherTask.repeatSeconds:2}") long repeatPeriod,
                @Value("${market.checkout.tasks.OrderEventPublisherTask.postponeSeconds:2}") long postponePeriod) {
            AutoRegisteringEnableAwareTask zooTask = createTask(zooClient, clock, enabledOnHost, healthCheckEnabled);

            zooTask.setNodePath("/test-task");
            zooTask.setRunnable(new ZooTaskRunnable() {
                @Override
                public void run(ZooTask task1, CancellationToken cancellationRequest) {
                }

                @Nonnull
                @Override
                public String getName() {
                    return "TestTask";
                }
            });
            zooTask.setRepeatPeriod(repeatPeriod);
            zooTask.setPostponePeriod(postponePeriod);
            zooTask.setPeriodUnit(TimeUnit.SECONDS);
            zooTask.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));

            return zooTask;
        }

        @Nonnull
        private AutoRegisteringEnableAwareTask createTask(ZooClient zooClient,
                                                          Clock clock,
                                                          boolean enabledOnHost,
                                                          boolean healthCheckEnabled) {
            AutoRegisteringEnableAwareTask zooTask = new AutoRegisteringEnableAwareTask();
            zooTask.setZooClient(zooClient);
            zooTask.setClock(clock);
            zooTask.setCrucial(true);
            zooTask.setEnabledOnHost(enabledOnHost);
            zooTask.setHealthCheckEnabled(healthCheckEnabled);

            return zooTask;
        }
    }
}
