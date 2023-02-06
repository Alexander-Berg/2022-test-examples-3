package ru.yandex.mail.cerberus.worker.registry_test;

import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.TaskType;
import ru.yandex.mail.cerberus.worker.MockTask;
import ru.yandex.mail.cerberus.worker.TaskRegistry;
import ru.yandex.mail.cerberus.worker.api.TaskConfiguration;
import ru.yandex.mail.cerberus.worker.api.TaskProcessor;
import ru.yandex.mail.cerberus.worker.executer.CronTaskSpawner;
import ru.yandex.mail.cerberus.worker.executer.Monitor;
import ru.yandex.mail.cerberus.worker.executer.Recycler;
import ru.yandex.mail.cerberus.worker.executer.Worker;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Optional;

import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static ru.yandex.mail.cerberus.worker.registry_test.TaskRegistryTest.TEST_ENV;

@MicronautTest(transactional = false,
               environments = {Environment.TEST, TEST_ENV},
               propertySources = "classpath:task_registry_test.yml")
class TaskRegistryTest {
    static final String TEST_ENV = "taskRegistryTestEnv";
    private static final String CRON_TASK_ID = "cron";
    private static final String ONE_OFF_TASK_ID = "one-off";
    private static final String UNREGISTERED_TASK_ID = "unregistered";

    private static final TaskConfiguration CRON_TASK_CONFIG =
        new TaskConfiguration(type(CRON_TASK_ID), ofMinutes(5), Optional.of(ofMinutes(10)));
    private static final TaskConfiguration ONE_OFF_TASK_CONFIG =
        new TaskConfiguration(type(ONE_OFF_TASK_ID), ofMinutes(5), Optional.empty());

    @Factory
    @Requires(env = TEST_ENV)
    public static class MockFactory {
        @MockTask(CRON_TASK_ID)
        public TaskProcessor<?> cron() {
            return mock(TaskProcessor.class);
        }

        @MockTask(ONE_OFF_TASK_ID)
        public TaskProcessor<?> oneOff() {
            return mock(TaskProcessor.class);
        }

        @MockTask(UNREGISTERED_TASK_ID)
        public TaskProcessor<?> unregisteredTask() {
            return mock(TaskProcessor.class);
        }

        @Bean
        @Singleton
        @Replaces(Worker.class)
        public Worker workerMock() {
            return mock(Worker.class);
        }

        @Bean
        @Singleton
        @Replaces(Recycler.class)
        public Recycler recyclerMock() {
            return mock(Recycler.class);
        }

        @Bean
        @Singleton
        @Replaces(Monitor.class)
        public Monitor monitorMock() {
            return mock(Monitor.class);
        }

        @Bean
        @Singleton
        @Replaces(CronTaskSpawner.class)
        public CronTaskSpawner cronTaskSpawnerMock() {
            return mock(CronTaskSpawner.class);
        }
    }

    @Inject
    BeanLocator beanLocator;

    @Inject
    TaskRegistry taskRegistry;

    private TaskProcessor findTaskProcessor(String name) {
        return beanLocator.getBean(TaskProcessor.class, Qualifiers.byName(name));
    }

    private static TaskType type(String id) {
        return new TaskType(id);
    }

    @Test
    @DisplayName("Verify that 'TaskRegistry' register all the configured task beans")
    void findTest() {
        val cronTaskProcessor = findTaskProcessor("cron");
        val oneOffTaskProcessor = findTaskProcessor("one-off");

        assertThat(taskRegistry.findTaskProcessor(type(CRON_TASK_ID)))
            .containsSame(cronTaskProcessor);
        assertThat(taskRegistry.findTaskProcessor(type(ONE_OFF_TASK_ID)))
            .containsSame(oneOffTaskProcessor);
        assertThat(taskRegistry.findTaskProcessor(type(UNREGISTERED_TASK_ID)))
            .isEmpty();
        assertThat(taskRegistry.findTaskProcessor(type("blah")))
            .isEmpty();

        assertThat(taskRegistry.findTaskConfiguration(type(CRON_TASK_ID)))
            .contains(CRON_TASK_CONFIG);
        assertThat(taskRegistry.findTaskConfiguration(type(ONE_OFF_TASK_ID)))
            .contains(ONE_OFF_TASK_CONFIG);
        assertThat(taskRegistry.findTaskConfiguration(type(UNREGISTERED_TASK_ID)))
            .isEmpty();

        assertThat(taskRegistry.findTaskRecord(type(CRON_TASK_ID)))
            .hasValueSatisfying(record -> {
                assertThat(record.getConfiguration()).isEqualTo(CRON_TASK_CONFIG);
                assertThat(record.getProcessor()).isSameAs(cronTaskProcessor);
            });
        assertThat(taskRegistry.findTaskRecord(type(ONE_OFF_TASK_ID)))
            .hasValueSatisfying(record -> {
                assertThat(record.getConfiguration()).isEqualTo(ONE_OFF_TASK_CONFIG);
                assertThat(record.getProcessor()).isSameAs(oneOffTaskProcessor);
            });
        assertThat(taskRegistry.findTaskRecord(type(UNREGISTERED_TASK_ID)))
            .isEmpty();
        assertThat(taskRegistry.findTaskRecord(type("unknown")))
            .isEmpty();
    }
}
