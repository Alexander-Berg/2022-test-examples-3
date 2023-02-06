package ru.yandex.mail.cerberus.worker.execution_test;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import ru.yandex.mail.cerberus.IdempotencyKey;
import ru.yandex.mail.cerberus.TaskType;
import ru.yandex.mail.cerberus.dao.task.TaskRepository;
import ru.yandex.mail.cerberus.dao.task.TaskStatus;
import ru.yandex.mail.cerberus.worker.api.SubmitResult;
import ru.yandex.mail.cerberus.worker.MockTask;
import ru.yandex.mail.cerberus.worker.TaskRegistry;
import ru.yandex.mail.cerberus.worker.api.TaskExecutionContext;
import ru.yandex.mail.cerberus.worker.api.TaskProcessor;
import ru.yandex.mail.cerberus.worker.api.exception.TaskTimeoutException;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.annotation.Nullable;
import javax.inject.Inject;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.mail.cerberus.worker.controller.TaskController.EMPTY_CONTEXT;
import static ru.yandex.mail.cerberus.worker.execution_test.TaskExecutionTest.DB_NAME;
import static ru.yandex.mail.cerberus.worker.execution_test.TaskExecutionTest.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.worker.execution_test.TaskExecutionTest.MIGRATIONS;
import static ru.yandex.mail.cerberus.worker.execution_test.TaskExecutionTest.TEST_ENV;

@Client(id = TasksClient.ID)
@Requires(env = TEST_ENV)
interface TasksClient {
    String ID = "tasks";

    @Post("/task/execute")
    CompletableFuture<SubmitResult> execute(@QueryValue TaskType type, @Nullable @QueryValue IdempotencyKey key,
                                            @Body String parameter);
}

@Value
@Introspected
@AllArgsConstructor(onConstructor_= @JsonCreator)
class TaskContext {
    long counter;

    TaskContext increment() {
        return new TaskContext(counter + 1);
    }
}

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
@MicronautTest(
    environments = { Environment.TEST, TEST_ENV },
    propertySources = "classpath:task_execution_test.yml",
    transactional = false
)
class TaskExecutionTest {
    static final String TEST_ENV = "taskExecutionTestEnv";
    static final String DB_NAME = "task_execution_test_db";
    static final String DB_NAME_PROPERTY = "test.database.name";
    static final String MIGRATIONS = "migrations";

    private static final String CRON_TASK_TYPE = "cron";
    private static final String EXPIRED_CRON_TASK_TYPE = "expired-cron";
    private static final String ONE_OFF_TASK_TYPE = "one-off";
    private static final String INTERRUPTING_TASK_TYPE = "interrupting";
    private static final String CRON_TASK_WITH_CONTEXT_TYPE = "context-cron";

    private static final long EXPIRED_TASK_SLEEP_TIME = Duration.ofSeconds(10).toMillis();
    private static final long AWAIT_TASK_TIMEOUT = Duration.ofSeconds(30).toMillis();

    @Factory
    @Requires(env = TEST_ENV)
    public static class MockFactory {
        private static TaskProcessor<Void> processorMock(boolean mockProcess) {
            @SuppressWarnings("unchecked")
            val processor = (TaskProcessor<Void>) mock(TaskProcessor.class);
            when(processor.contextType())
                .thenReturn(Void.class);
            if (mockProcess) {
                when(processor.process(any(), any()))
                    .thenReturn(Mono.empty());
            }
            return processor;
        }

        @MockTask(CRON_TASK_TYPE)
        public TaskProcessor<Void> cron() {
            val processor = processorMock(false);
            when(processor.process(any(), any()))
                .thenReturn(Mono.error(new RuntimeException()))
                .thenReturn(Mono.empty());
            return processor;
        }

        @MockTask(ONE_OFF_TASK_TYPE)
        public TaskProcessor<Void> oneOff() {
            return processorMock(true);
        }

        @MockTask(EXPIRED_CRON_TASK_TYPE)
        public TaskProcessor<Void> expiredCron() {
            val processor = processorMock(false);
            when(processor.process(any(), any()))
                .thenAnswer(args -> {
                    Thread.sleep(EXPIRED_TASK_SLEEP_TIME);
                    return Mono.empty();
                })
                .thenReturn(Mono.empty());
            return processor;
        }

        @MockTask(INTERRUPTING_TASK_TYPE)
        public TaskProcessor<Void> interrupting() {
            return processorMock(false);
        }

        @MockTask(CRON_TASK_WITH_CONTEXT_TYPE)
        public TaskProcessor<TaskContext> cronTaskWithContext() {
            @SuppressWarnings("unchecked")
            val processor = (TaskProcessor<TaskContext>) mock(TaskProcessor.class);
            when(processor.contextType())
                .thenReturn(TaskContext.class);
            when(processor.process(any(), any()))
                .thenAnswer(args -> {
                    final Optional<TaskContext> context = args.getArgument(0);
                    val result = context.map(TaskContext::increment)
                        .orElse(new TaskContext(1));
                    return Mono.just(result);
                });
            return processor;
        }
    }

    private static TaskType type(String typeId) {
        return new TaskType(typeId);
    }

    private TaskProcessor<?> findTaskProcessor(String typeId) {
        return taskRegistry.findTaskProcessor(type(typeId))
            .orElseThrow(() -> new AssertionError("Task not found"));
    }

    @Inject
    TasksClient tasksClient;

    @Inject
    TaskRegistry taskRegistry;

    @Inject
    TaskRepository taskRepository;

    private void waitForTaskUntilAsserted(ThrowingRunnable runnable) {
        Awaitility.waitAtMost(AWAIT_TASK_TIMEOUT, TimeUnit.MILLISECONDS)
            .untilAsserted(runnable);
    }

    private void assertExecutedTaskPresent(String type, TaskStatus status, Optional<String> context) {
        assertThat(taskRepository.findExecutedTasks(new TaskType(type)))
            .anySatisfy(info -> {
                assertThat(info.getStatus())
                    .isEqualTo(status);
                assertThat(info.getType())
                    .isEqualTo(new TaskType(type));
                assertThat(info.getContext())
                    .isEqualTo(context);
            });
    }

    private void assertExecutedTaskPresent(String type, TaskStatus status) {
        assertExecutedTaskPresent(type, status, Optional.empty());
    }

    private void assertExecutedTaskPresent(IdempotencyKey key, String type, TaskStatus status, Optional<String> context) {
        assertThat(taskRepository.findExecutedTasks(new TaskType(type)))
            .anySatisfy(info -> {
                assertThat(info.getIdempotencyKey())
                    .isEqualTo(key);
                assertThat(info.getType())
                    .isEqualTo(new TaskType(type));
                assertThat(info.getStatus())
                    .isEqualTo(status);
                assertThat(info.getContext())
                    .isEqualTo(context);
            });
    }

    private void assertExecutedTaskPresent(IdempotencyKey key, String type, TaskStatus status) {
        assertExecutedTaskPresent(key, type, status, Optional.empty());
    }

    @Test
    @DisplayName("Verify that cron task scheduled and executed right after startup")
    void testCronTaskAfterStartup() {
        val cronTaskProcessor = findTaskProcessor(CRON_TASK_TYPE);
        verify(cronTaskProcessor, timeout(AWAIT_TASK_TIMEOUT).atLeastOnce())
            .process(any(), any());
        waitForTaskUntilAsserted(() -> assertExecutedTaskPresent(CRON_TASK_TYPE, TaskStatus.SUCCESS));
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that one-off task executed after submission")
    void testOneOffTaskExecution() {
        val key = IdempotencyKey.random();
        val oneOffTaskProcessor = findTaskProcessor(ONE_OFF_TASK_TYPE);
        val submitResult = tasksClient.execute(new TaskType(ONE_OFF_TASK_TYPE), key, EMPTY_CONTEXT).get();
        assertThat(submitResult.getIdempotencyKey())
            .isEqualTo(key);

        verify(oneOffTaskProcessor, timeout(AWAIT_TASK_TIMEOUT).times(1))
            .process(any(), any());
        waitForTaskUntilAsserted(() -> assertExecutedTaskPresent(key, ONE_OFF_TASK_TYPE, TaskStatus.SUCCESS));
    }

    @Test
    @DisplayName("Verify that cron task will start after a failure according to its schedule rescheduled")
    void testFailedCronTask() {
        val cronTaskProcessor = findTaskProcessor(CRON_TASK_TYPE);
        verify(cronTaskProcessor, timeout(AWAIT_TASK_TIMEOUT).atLeast(2))
            .process(any(), any());

        waitForTaskUntilAsserted(() -> {
            assertExecutedTaskPresent(CRON_TASK_TYPE, TaskStatus.FAILED);
            assertExecutedTaskPresent(CRON_TASK_TYPE, TaskStatus.SUCCESS);
        });
    }

    @Test
    @DisplayName("Verify that cron task will start after a timeout according to its schedule")
    void testExpiredCronTask() {
        val expiredCronTaskProcessor = findTaskProcessor(EXPIRED_CRON_TASK_TYPE);
        verify(expiredCronTaskProcessor, timeout(AWAIT_TASK_TIMEOUT * 2).atLeast(2))
            .process(any(), any());

        waitForTaskUntilAsserted(() -> {
            assertExecutedTaskPresent(EXPIRED_CRON_TASK_TYPE, TaskStatus.TIMEOUT);
            assertExecutedTaskPresent(EXPIRED_CRON_TASK_TYPE, TaskStatus.SUCCESS);
        });
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that task execution context interrupts task after timeout")
    void testTimeoutCheck() {
        val taskException = new AtomicReference<Exception>(null);
        val key = IdempotencyKey.random();

        val processor = findTaskProcessor(INTERRUPTING_TASK_TYPE);
        when(processor.process(any(), any()))
            .thenAnswer(args -> {
                try {
                    Thread.sleep(1000);
                    final TaskExecutionContext context = args.getArgument(1);
                    context.setInterruptionPoint();
                    return Mono.empty();
                } catch (Exception e) {
                    taskException.set(e);
                    return Mono.error(e);
                }
            })
            .thenReturn(Mono.empty());

        tasksClient.execute(new TaskType(INTERRUPTING_TASK_TYPE), key, EMPTY_CONTEXT).get();

        verify(processor, timeout(AWAIT_TASK_TIMEOUT).atLeastOnce())
            .process(any(), any());

        waitForTaskUntilAsserted(() -> {
            assertExecutedTaskPresent(key, INTERRUPTING_TASK_TYPE, TaskStatus.TIMEOUT);

            assertThat(taskException)
                .doesNotHaveValue(null)
                .matches(e -> e.get() instanceof TaskTimeoutException);
        });
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that multiple one-off tasks simultaneous execution supported")
    void testMultipleOneOffs() {
        val tasksCount = 3;
        val oneOffTaskProcessor = findTaskProcessor(ONE_OFF_TASK_TYPE);

        val keys = IntStreamEx.range(0, tasksCount)
            .mapToObj(i -> IdempotencyKey.random())
            .toImmutableList();

        val futures = StreamEx.of(keys)
            .map(key -> tasksClient.execute(new TaskType(ONE_OFF_TASK_TYPE), key, EMPTY_CONTEXT))
            .map(future -> future.thenApply(SubmitResult::getIdempotencyKey))
            .toImmutableList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();

        verify(oneOffTaskProcessor, timeout(AWAIT_TASK_TIMEOUT).atLeast(tasksCount))
            .process(any(), any());

        waitForTaskUntilAsserted(() -> {
            keys.forEach(key -> assertExecutedTaskPresent(key, ONE_OFF_TASK_TYPE, TaskStatus.SUCCESS));
        });
    }

    @Test
    @DisplayName("Verify that cron task context passing trough the tasks executions")
    void contextPassingTest() {
        @SuppressWarnings("unchecked")
        val cronWithContextProcessor = (TaskProcessor<TaskContext>) findTaskProcessor(CRON_TASK_WITH_CONTEXT_TYPE);
        verify(cronWithContextProcessor, timeout(AWAIT_TASK_TIMEOUT).atLeastOnce())
            .process(eq(Optional.of(new TaskContext(2))), any());
    }
}
