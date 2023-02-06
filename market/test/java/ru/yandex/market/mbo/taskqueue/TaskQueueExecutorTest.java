package ru.yandex.market.mbo.taskqueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.concurrent.Executors;
import ru.yandex.common.util.concurrent.StoppableExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author yuramalinov
 * @created 23.03.19
 */
@SuppressWarnings("checkstyle:MagicNumber")
// NOTE: no Transactional, for manual control over transactions
public class TaskQueueExecutorTest extends BaseTaskQueueTest {
    private static final int SLOT1 = 1;
    private static final int SLOT2 = 2;
    private static final long WAIT_SECONDS = 5;
    private static final long DEFAULT_TIMEOUT = 50;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TaskQueueRepository taskQueueRepositorySpied;

    private TaskQueueRepository taskQueueRepository;
    private TaskQueueHandlerRegistry handlerRegistry;
    private TaskQueueRegistrator taskQueueRegistrator;
    private TaskQueueExecutor taskQueueExecutor;
    private TestTaskHandler taskHandler;
    private StoppableExecutorService executorService;
    private List<Runnable> shutdown;
    private ObjectMapper objectMapper;

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

    @Before
    public void setup() {
        taskQueueRepository = Mockito.spy(taskQueueRepositorySpied);

        taskHandler = new TestTaskHandler();
        objectMapper = new ObjectMapper();
        handlerRegistry = TaskQueueHandlerRegistry.newBuilder()
            .addGroup(1, taskHandler)
            .buildRegistry();
        taskQueueRegistrator = new TaskQueueRegistrator(taskQueueRepository, objectMapper);
        taskQueueExecutor = createTaskQueueExecutor();
        executorService = Executors.newFixedThreadPool(4);
        shutdown = new ArrayList<>();
    }

    private TaskQueueExecutor createTaskQueueExecutor() {
        return createTaskQueueExecutor(DEFAULT_TIMEOUT);
    }

    private TaskQueueExecutor createTaskQueueExecutor(long timeout) {
        return createTaskQueueExecutor(timeout, handlerRegistry);
    }

    private TaskQueueExecutor createTaskQueueExecutor(TaskQueueHandlerRegistry handlerRegistry) {
        return createTaskQueueExecutor(DEFAULT_TIMEOUT, handlerRegistry);
    }

    private TaskQueueExecutor createTaskQueueExecutor(long timeout, TaskQueueHandlerRegistry handlerRegistry) {
        TaskQueueExecutor taskExecutor = new TaskQueueExecutor(
            handlerRegistry, transactionTemplate, taskQueueRepository, objectMapper);
        taskExecutor.setTaskWaitTimeoutMs(timeout);
        return taskExecutor;
    }

    @After
    public void shutdown() throws InterruptedException {
        taskQueueExecutor.shutdown();
        shutdown.forEach(Runnable::run);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // manual cleanup
        if (taskQueueRepository.findAll(true).size() != taskQueueRepository.findAll().size()) {
            throw new RuntimeException("Some tasks are still locked. " +
                "This indicates problem in test and will corrupt further tests");
        }
        taskQueueRepository.deleteAll();
    }

    @Test
    public void testItHandlesTaskEventually() {
        taskQueueExecutor.init();

        assertThat(taskHandler.results).isEmpty();
        // in case of usage - it won't see task from not yet committed transaction
        transactionTemplate.execute(status ->
            taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test")));

        await5s().until(() -> !taskHandler.results.isEmpty());
        assertThat(taskHandler.results.get(SLOT1)).isEqualTo("test");
        List<TaskRecord> allTasks = taskQueueRepository.findAll();
        assertThat(allTasks).hasSize(1);
        TaskRecord taskRecord = allTasks.get(0);
        assertThat(taskRecord.getTaskState()).isEqualTo(TaskRecord.TaskState.DONE);
    }

    @Test
    public void testTaskRecordCanBeFetchedById() {
        taskQueueExecutor.init();

        assertThat(taskHandler.results).isEmpty();
        // in case of usage - it won't see task from not yet committed transaction
        CountDownLatch latch = new CountDownLatch(1);
        taskHandler.latches.put(SLOT1, latch);
        Long taskId = transactionTemplate.execute(status ->
            taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test")));
        TaskRecord task = taskQueueRepository.findById(taskId);
        assertThat(task.getTaskState()).isEqualTo(TaskRecord.TaskState.ACTIVE);
        assertThat(task.getTaskType()).isEqualTo("TestTaskHandlerBase.Task");
        latch.countDown();

        await5s().until(() -> !taskHandler.results.isEmpty());
        assertThat(taskHandler.results.get(SLOT1)).isEqualTo("test");
        task = taskQueueRepository.findById(taskId);
        assertThat(task.getTaskState()).isEqualTo(TaskRecord.TaskState.DONE);
        assertThat(task.getTaskResult()).isEqualTo('"' + TestTaskHandler.RESULT + '"');
    }

    @Test
    public void testItDoesntTakeTaskFromUncommittedTransaction() {
        assertThat(taskHandler.results).isEmpty();

        CountDownLatch addedTask = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        // in case of usage - it won't see task from not yet committed transaction
        executorService.submit(() -> {
            transactionTemplate.execute(status -> {
                taskQueueRegistrator.registerTask(new TestTaskHandler.Task(1, "test"));
                addedTask.countDown();
                awaitLatch(allowFinish);
                return null;
            });
        });
        awaitLatch(addedTask);

        taskQueueExecutor.processNextStep();
        assertThat(taskHandler.results).isEmpty();

        // Now transaction will commit in some future, await processing
        allowFinish.countDown();

        await5s().until(() -> {
            taskQueueExecutor.processNextStep();
            return !taskHandler.results.isEmpty();
        });
        assertThat(taskHandler.results.get(1)).isEqualTo("test");
    }

    @Test
    public void testItCorrectlyFailsTask() {
        taskHandler.runsToFail.put(SLOT1, 10); // fail 10 runs
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 5)); // 5 retries
        taskQueueExecutor.init(); // run threads

        await5s().until(() -> {
            TaskRecord task = taskQueueRepository.findAll().get(0);
            return task.getTaskState() == TaskRecord.TaskState.FAILED;
        });

        TaskRecord task = taskQueueRepository.findAll().get(0);
        assertThat(task.getAttempts()).isEqualTo(5);
    }

    @Test
    public void testItCorrectlyFailsWithNullTask() {
        taskHandler.failWithException = new NullPointerException();
        taskHandler.runsToFail.put(SLOT1, 10); // fail 10 runs
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 5)); // 5 retries
        taskQueueExecutor.init(); // run threads

        await5s().until(() -> {
            TaskRecord task = taskQueueRepository.findAll().get(0);
            return task.getTaskState() == TaskRecord.TaskState.FAILED;
        });

        TaskRecord task = taskQueueRepository.findAll().get(0);
        assertThat(task.getAttempts()).isEqualTo(5);
    }

    @Test
    public void testItAllowsTaskToRetry() {
        taskHandler.runsToFail.put(SLOT1, 2); // fail 2 runs
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 5)); // 5 retries
        taskQueueExecutor.init(); // run threads

        await5s().until(() -> {
            TaskRecord task = taskQueueRepository.findAll().get(0);
            return task.getTaskState() == TaskRecord.TaskState.DONE;
        });

        TaskRecord task = taskQueueRepository.findAll().get(0);
        assertThat(task.getAttempts()).isEqualTo(2);
    }

    @Test
    public void testItShouldNotTakeTwoTasksWithSameLockConcurrently() throws InterruptedException {
        // We need two taskExecutors for this test
        TaskQueueExecutor taskExecutor1 = createTaskQueueExecutor();
        TaskQueueExecutor taskExecutor2 = createTaskQueueExecutor();

        shutdown.add(taskExecutor1::shutdown);
        shutdown.add(taskExecutor2::shutdown);

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test1"), "lock");
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT2, "test2"), "lock");

        CountDownLatch taskOneIsWorking = new CountDownLatch(1);
        taskHandler.latches.put(SLOT1, taskOneIsWorking);

        taskExecutor1.init();
        taskExecutor2.init();

        // Execution order is predefined: first registered runs first (at least for first attempt)
        await5s().until(() -> taskHandler.isRunning.getOrDefault(SLOT1, false));
        Thread.sleep(500); // have to wait to check that second didn't started
        assertThat(taskHandler.isRunning.getOrDefault(SLOT2, false)).isFalse();
        assertThat(taskHandler.results.get(SLOT2)).isNull();

        taskOneIsWorking.countDown(); // Release it and wait for second to be running
        await5s().until(() -> "test2".equals(taskHandler.results.get(SLOT2)));
    }

    @Test
    public void testItShouldCreateTaskWithLockWhileAnotherTaskHoldsLock() {
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test1"), "lock");

        CountDownLatch taskOneIsWorking = new CountDownLatch(1);
        taskHandler.latches.put(SLOT1, taskOneIsWorking);

        TaskQueueExecutor taskExecutor = createTaskQueueExecutor();
        shutdown.add(taskExecutor::shutdown);
        taskExecutor.init();
        await5s().until(() -> taskHandler.isRunning.getOrDefault(SLOT1, false));

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT2, "test2"), "lock");

        taskOneIsWorking.countDown();

        await5s().until(() -> "test1".equals(taskHandler.results.get(SLOT1)));
    }

    @Test
    public void testItShouldTakeTwoTasksWithoutLockConcurrently() {
        // We need two taskExecutors for this test
        TaskQueueExecutor taskExecutor1 = createTaskQueueExecutor();
        TaskQueueExecutor taskExecutor2 = createTaskQueueExecutor();

        shutdown.add(taskExecutor1::shutdown);
        shutdown.add(taskExecutor2::shutdown);

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test1"));
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT2, "test2"));

        CountDownLatch taskOneIsWorking = new CountDownLatch(1);
        CountDownLatch taskTwoIsWorking = new CountDownLatch(1);
        taskHandler.latches.put(SLOT1, taskOneIsWorking);
        taskHandler.latches.put(SLOT2, taskTwoIsWorking);

        taskExecutor1.init();
        taskExecutor2.init();

        // Should be working simultaneously
        await5s().until(() -> taskHandler.isRunning.getOrDefault(SLOT1, false));
        await5s().until(() -> taskHandler.isRunning.getOrDefault(SLOT2, false));

        // Release them
        taskOneIsWorking.countDown();
        taskTwoIsWorking.countDown();

        await5s().until(() -> "test1".equals(taskHandler.results.get(SLOT1)));
        await5s().until(() -> "test2".equals(taskHandler.results.get(SLOT2)));
    }

    @Test
    public void testOnTaskExceptionTakeABreak() throws InterruptedException {
        AtomicBoolean called = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            called.set(true);
            throw new RuntimeException("Something went wrong");
        }).when(taskQueueRepository).findNextTask(Mockito.anyBoolean(), Mockito.anyList());

        TaskQueueExecutor taskExecutor = createTaskQueueExecutor(10_000); // Large timeout
        shutdown.add(taskExecutor::shutdown);

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test"));
        taskExecutor.init(); // Run the loop
        await5s().until(called::get); // Wait until first call
        Thread.sleep(100); // A bit of delay, to be sure no more consequent calls are performed
        Mockito.verify(taskQueueRepository, Mockito.times(1)).findNextTask(Mockito.anyBoolean(), Mockito.anyList());
    }

    @Test
    public void testFindNextTaskFailure() {
        Mockito.doThrow(new RuntimeException("Something went wrong"))
            .when(taskQueueRepository).findNextTask(Mockito.anyBoolean(), Mockito.anyList());

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test"));
        Assertions.assertThatThrownBy(() -> taskQueueExecutor.processNextStep())
            .hasMessageContaining("Failed to fetch next task");
    }

    @Test
    public void testMarkDoneFailure() {
        Mockito.doThrow(new RuntimeException("Something went wrong"))
            .when(taskQueueRepository).markDone(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test"));
        Assertions.assertThatThrownBy(() -> taskQueueExecutor.processNextStep()).hasMessageContaining("markDone");
    }

    @Test
    public void testMarkRetryFailure() {
        Mockito.doThrow(new RuntimeException("Something went wrong"))
            .when(taskQueueRepository)
            .markRetry(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 2));
        taskHandler.runsToFail.put(SLOT1, 1);
        Assertions.assertThatThrownBy(() -> taskQueueExecutor.processNextStep())
            .hasMessageContaining("Can't process exception for task");
    }

    @Test
    public void testMarkFailedFailure() {
        Mockito.doThrow(new RuntimeException("Something went wrong"))
            .when(taskQueueRepository)
            .markFailed(Mockito.anyLong(), Mockito.anyString());

        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 0));
        taskHandler.runsToFail.put(SLOT1, 2);
        Assertions.assertThatThrownBy(() -> taskQueueExecutor.processNextStep())
            .hasMessageContaining("Can't process exception for task");
    }

    @Test
    public void testTaskWithUnknownTypeAreIgnored() {
        taskQueueRegistrator.registerTask(new UnknownTask(1));

        Assertions.assertThatCode(() -> taskQueueExecutor.processNextStep())
            .doesNotThrowAnyException();
        Mockito.verify(taskQueueRepository, Mockito.never())
            .markFailed(Mockito.anyLong(), Mockito.any());
        Mockito.verify(taskQueueRepository, Mockito.never())
            .markDone(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.verify(taskQueueRepository, Mockito.never())
            .markRetry(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void testGroups() {
        TestTaskHandler2 taskHandler2 = new TestTaskHandler2();
        TaskQueueHandlerRegistry registry = TaskQueueHandlerRegistry.newBuilder()
            .addGroup(1, taskHandler)
            .addGroup(1, taskHandler2)
            .buildRegistry();

        TaskQueueExecutor taskExecutor = createTaskQueueExecutor(registry);
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(1, "test"));
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(2, "test"));

        CountDownLatch handler1slot1 = new CountDownLatch(1);
        CountDownLatch handler2slot1 = new CountDownLatch(1);
        taskHandler.latches.put(1, handler1slot1);
        taskHandler2.latches.put(1, handler2slot1);

        taskExecutor.init();
        shutdown.add(taskExecutor::shutdown);

        await5s().until(() -> taskHandler.isRunning.getOrDefault(1, false));
        // Very long task queue
        Assertions.assertThat(taskHandler.isRunning.getOrDefault(2, false)).isFalse();

        // Here comes other task
        taskQueueRegistrator.registerTask(new TestTaskHandler2.Task2(1, "I'm a task2"));
        await5s().until(() -> taskHandler2.isRunning.getOrDefault(1, false));
        taskQueueRegistrator.registerTask(new TestTaskHandler2.Task2(2, "I'm a task2-1"));

        handler1slot1.countDown();
        handler2slot1.countDown();

        // Everything runs fine then
        await5s().until(() -> Objects.equals(false, taskHandler2.isRunning.get(1)));
        await5s().until(() -> Objects.equals(false, taskHandler2.isRunning.get(2)));
        await5s().until(() -> Objects.equals(false, taskHandler.isRunning.get(1)));
        await5s().until(() -> Objects.equals(false, taskHandler.isRunning.get(2)));
    }

    @Test
    public void testThreadNameInHandler() {
        // create thread name holder
        AtomicReference<String> threadReference = new AtomicReference<>();

        class ThreadNameHandler implements TaskQueueHandler<UnknownTask> {
            @Nullable
            @Override
            public Object handle(UnknownTask task, TaskRecord taskRecord) {
                threadReference.set(Thread.currentThread().getName());
                return null;
            }
        }

        // create registry and executor
        TaskQueueHandlerRegistry registry = TaskQueueHandlerRegistry.newBuilder()
                .addGroup(1, new ThreadNameHandler())
                .buildRegistry();

        TaskQueueExecutor taskExecutor = createTaskQueueExecutor(registry);
        taskQueueRegistrator.registerTask(new UnknownTask(1));

        // run executor
        taskExecutor.init();
        shutdown.add(taskExecutor::shutdown);

        // wait until task is processed
        await5s().until(() -> !Objects.equals(null, threadReference.get()));

        Assertions.assertThat(threadReference)
                .hasValue("TaskQueueExecutor-0-ThreadNameHandler");
    }

    @Test
    public void testCalculateStatistic() {
        taskQueueExecutor.init();

        assertThat(taskHandler.results).isEmpty();
        // in case of usage - it won't see task from not yet committed transaction
        CountDownLatch latch = new CountDownLatch(1);
        taskHandler.latches.put(SLOT1, latch);
        transactionTemplate.execute(status ->
                taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test")));
        await5s().until(
                () -> {
                    return taskQueueRepository.calculateStatistic().get(0).getExecutionLagByNonFailedTask() > 2;
                }
        );
        List<TaskQueueStatistic> statistics = taskQueueRepository.calculateStatistic();
        latch.countDown();
        TaskQueueStatistic taskQueueStatistic = statistics.get(0);
        Assertions.assertThat(taskQueueStatistic.getTaskType()).isEqualTo("TestTaskHandlerBase.Task");
        Assertions.assertThat(taskQueueStatistic.getTaskWithoutAttemptsInQueueCount()).isGreaterThan(0);
        Assertions.assertThat(taskQueueStatistic.getExecutionLagByNonFailedTask()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void testCalculateStatisticWithAttemptedTask() {
        taskHandler.runsToFail.put(SLOT1, 100);
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 101));
        taskQueueExecutor.init(); // run threads

        AtomicReference<List<TaskQueueStatistic>> ref = new AtomicReference<>();
        await5s().until(() -> {
            List<TaskQueueStatistic> statistics = taskQueueRepository.calculateStatistic();
            ref.set(statistics);
            return statistics.get(0).getExecutionLagByAllTask() > 1;
        });
        List<TaskQueueStatistic> statistics = ref.get();
        TaskQueueStatistic taskQueueStatistic = statistics.get(0);
        Assertions.assertThat(taskQueueStatistic.getTaskWithAttemptsInQueueCount()).isGreaterThanOrEqualTo(1);
        Assertions.assertThat(taskQueueStatistic.getExecutionLagByAllTask()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void testCalculateStatisticForCompletedTask() {
        List<TaskQueueStatistic> statistics = taskQueueRepository.calculateStatistic();
        Assertions.assertThat(statistics).isEmpty();

        taskHandler.runsToFail.put(SLOT1, 0);
        taskQueueRegistrator.registerTask(new TestTaskHandler.Task(SLOT1, "test", 2));
        taskQueueExecutor.init(); // run threads

        await5s().until(() -> {
            TaskRecord task = taskQueueRepository.findAll().get(0);
            return task.getTaskState() == TaskRecord.TaskState.DONE;
        });
        //for completed task => not data
        Assertions.assertThat(taskQueueRepository.calculateStatistic()).isEmpty();
    }

    private ConditionFactory await5s() {
        return await().atMost(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    private static final class UnknownTask implements TaskQueueTask {
        private int stub;

        UnknownTask() {
        }

        UnknownTask(int stub) {
            this.stub = stub;
        }

        public int getStub() {
            return stub;
        }

        public void setStub(int stub) {
            this.stub = stub;
        }
    }
}
