package ru.yandex.market.checkout.common.tasks;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.common.zk.ZooClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextHierarchy(
        @ContextConfiguration(name = "task", classes = ZooTaskEnvTest.ZooTaskEnvConfig.class)
)
public class ZooTaskEnvTest extends AbstractWebTestBase {

    @Autowired
    @Qualifier("testZooTask")
    public AutoRegisteringEnableAwareTask testTask;
    @Autowired
    @Qualifier("activeZooTask")
    public AutoRegisteringEnableAwareTask activeTask;

    @Override
    protected void enableAllTasks() {
    }

    @Test
    public void shouldNotRunAnotherEnvTask() {
        assertFalse(testTask.isStarted());
    }

    @Test
    public void shouldCanRunActiveEnvTask() {
        assertTrue(activeTask.isStarted());
    }

    @SpringBootConfiguration
    static class ZooTaskEnvConfig {

        @Nonnull
        @Autowired
        private ZooClient zooClient;

        @Nonnull
        @Autowired
        private ZooTaskRegistry registry;

        @Bean
        public static ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP zooTaskConfigServiceBFPP() {
            return new ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP("zooMigratorSpring");
        }

        @Bean(name = "testZooTask", initMethod = "start", destroyMethod = "stop")
        public AutoRegisteringEnableAwareTask testZooTask() {
            return createTask("testZooTask",
                    "/testpath/test-task1",
                    EnvironmentType.PRESTABLE);
        }

        @Bean(name = "activeZooTask", initMethod = "start", destroyMethod = "stop")
        public AutoRegisteringEnableAwareTask activeZooTask() {
            return createTask("activeZooTask",
                    "/testpath/test-task2",
                    EnvironmentType.getActive());
        }

        @Nonnull
        private AutoRegisteringEnableAwareTask createTask(String name,
                                                          String nodePath,
                                                          EnvironmentType environmentType) {
            AutoRegisteringEnableAwareTask zooTask = new AutoRegisteringEnableAwareTask();
            zooTask.setNodePath(nodePath);
            zooTask.setZooClient(zooClient);
            zooTask.setRepeatPeriod(1000);
            zooTask.setPostponePeriod(1000);
            zooTask.setPeriodUnit(TimeUnit.MILLISECONDS);
            zooTask.setCrucial(false);
            zooTask.setFailPeriodFactor(2);
            zooTask.setRunnable(new ZooTaskRunnable() {
                @Override
                public void run(ZooTask task, CancellationToken cancellationRequest) {

                }

                @Nonnull
                @Override
                public String getName() {
                    return name;
                }
            });
            if (environmentType != null) {
                zooTask.setPermittedEnvironmentTypes(EnumSet.of(environmentType));
            }
            return zooTask;
        }

    }

}
