package ru.yandex.market.crm.tasks.primitives;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.domain.TaskInstanceInfo;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.primitives.ParallelTaskData.SubTask;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceFactory;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author apershukov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClusterTasksServiceTestConfig.class)
public class ParallelTaskTest {

    private static class SubTaskData1 {

        @JsonProperty("foo")
        private String foo;

        SubTaskData1(String foo) {
            this.foo = foo;
        }

        public SubTaskData1() {
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    private static class SubTaskData2 {

        @JsonProperty("bar")
        private String bar;

        SubTaskData2(String bar) {
            this.bar = bar;
        }

        public SubTaskData2() {
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }
    }

    private static class SubTask1 implements Task<Void, SubTaskData1> {

        @Nonnull
        @Override
        public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) throws Exception {
            return ExecutionResult.completed();
        }

        @Override
        public String getId() {
            return SubTask1.class.getSimpleName();
        }
    }

    private static class SubTask2 implements Task<Void, SubTaskData2> {

        @Nonnull
        @Override
        public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) throws Exception {
            return ExecutionResult.completed();
        }

        @Override
        public String getId() {
            return SubTask2.class.getSimpleName();
        }
    }

    private static class TaskWrapper extends DefaultSequenceTask<Void, Void, SequenceTaskData> {

        TaskWrapper(JsonDeserializer jsonDeserializer,
                    JsonSerializer jsonSerializer,
                    Task<? super Void, ?>... steps) {
            super(
                    jsonDeserializer,
                    jsonSerializer,
                    (v, d) -> null,
                    steps
            );
        }

        @Override
        public String getId() {
            return TASK_ID;
        }
    }

    private static class DefaultTask extends ParallelTask<Void> {

        private final Task<? super Void, ?>[] steps;

        DefaultTask(JsonSerializer jsonSerializer,
                    JsonDeserializer jsonDeserializer,
                    Task<? super Void, ?>... steps) {
            super(jsonSerializer, jsonDeserializer);
            this.steps = steps;
            for (Task<? super Void, ?> step : steps) {
                register(step);
            }
        }

        @Override
        protected void scheduleTasks(Void context) {
            for (Task<? super Void, ?> step : steps) {
                schedule(step, null);
            }
        }
    }

    private static final String TASK_ID = "task_id";

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private ClusterTasksServiceFactory serviceFactory;

    @Inject
    private ClusterTasksDAO clusterTasksDAO;

    @Inject
    private DbTestTool dbTestTool;

    private ClusterTasksService service;

    @AfterEach
    public void tearDown() throws Exception {
        dbTestTool.runScript("/sql/clearDatabase.sql");
        if (service != null) {
            service.stop();
        }
    }

    /**
     * Выполняются все задачи, выполнение которых было запланировано в переопределенном методе scheduleTasks().
     *
     * При этом:
     * 1. Каждая подзадача оперирует своей собственной информацией, формат которой индивидуален для каждой подзадачи
     * 2. После выполнения всех запланированных подзадач происходит завершение родительской задачи
     *    (с вызовом соответствующего метода)
     */
    @Test
    void testExecuteAllTasks() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                control.saveData(new SubTaskData1("data1"));
                return ExecutionResult.completed();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                control.saveData(new SubTaskData2("data2"));
                return ExecutionResult.completed();
            }
        };

        ParallelTask<Void> parallelTask = defaultTask(subTask1, subTask2);

        CountDownLatch onSuccessLatch = new CountDownLatch(1);

        TaskWrapper wrapper = new TaskWrapper(jsonDeserializer, jsonSerializer, parallelTask) {

            @Override
            public void onSuccess(Void context, SequenceTaskData data, Control<SequenceTaskData> control) {
                onSuccessLatch.countDown();
            }
        };

        service = serviceFactory.create(
                Collections.singleton(wrapper)
        );
        service.start();

        long taskId = service.submitTask(TASK_ID, new SequenceTaskData());

        assertTrue(
                runLatch1.await(10, TimeUnit.SECONDS),
                "First task has not been invoked"
        );

        assertTrue(
                runLatch2.await(10, TimeUnit.SECONDS),
                "Second task has not been invoked"
        );

        assertTrue(
                onSuccessLatch.await(10, TimeUnit.SECONDS),
                "Global onSuccess() method has not been called"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.COMPLETED, info.getStatus());

        ParallelTaskData stepData = getStepData(parallelTask, info);

        SubTaskData1 subTask1Data = getData(stepData, subTask1.getId(), SubTaskData1.class);
        assertNotNull(subTask1Data);
        assertEquals("data1", subTask1Data.getFoo());

        SubTaskData2 subTask2Data = getData(stepData, subTask2.getId(), SubTaskData2.class);
        assertNotNull(subTask2Data);
        assertEquals("data2", subTask2Data.getBar());

        ExecutionResult result1 = getExecutionResult(stepData, subTask1.getId());
        assertNotNull(result1);
        assertTrue(result1.isCompleted());

        ExecutionResult result2 = getExecutionResult(stepData, subTask2.getId());
        assertNotNull(result2);
        assertTrue(result2.isCompleted());
    }

    /**
     * В обработчик подзадачи в десериализованном виде передается информация,
     * связанная с подзадачей
     */
    @Test
    void testDeserializedDataIsPassedToSubStepRunMethod() throws Exception {
        BlockingQueue<SubTaskData1> queue = new ArrayBlockingQueue<>(1);

        SubTask1 subTask = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                queue.add(data);
                return ExecutionResult.completed();
            }
        };

        DefaultTask task = defaultTask(subTask);

        SubTask subTaskData = new SubTask(
                subTask.getId(),
                toJsonNode(new SubTaskData1("value"))
        );

        saveTaskData(task, subTaskData);

        startService(task);

        SubTaskData1 passedData = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(passedData);
        assertEquals("value", passedData.getFoo());
    }

    /**
     * Если одна из подзадач после вычисления перешла в ожидание, то родительская таска
     * так же переходит в статус WAITING
     */
    @Test
    void testIfOneOfTasksReturnsWaitResultAllTaskSwitchesToWaiting() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                return ExecutionResult.completed();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                return ExecutionResult.yield();
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        startService(task);
        long taskId = service.submitTask(TASK_ID, new SequenceTaskData());

        assertTrue(
                runLatch1.await(10, TimeUnit.SECONDS),
                "First task has not been invoked"
        );

        assertTrue(
                runLatch2.await(10, TimeUnit.SECONDS),
                "Second task has not been invoked"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.WAITING, info.getStatus());
        assertNull(info.getNextRunTime());
    }

    /**
     * Если несколько подзадач перешли в статус WAITING родительская задача так же
     * переходит в WAITING с запланированным временем выполнения равным минимальному запланированному
     * времени ожидающей подзадачи
     */
    @Test
    void testWhenSeveralSubtaskGoneToWaitingTaskWaitsMinimalPeriod() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                return ExecutionResult.repeatIn(Duration.ofMinutes(1));
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                return ExecutionResult.repeatIn(Duration.ofMinutes(30));
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        startService(task);
        long taskId = service.submitTask(TASK_ID, new SequenceTaskData());

        assertTrue(
                runLatch1.await(10, TimeUnit.SECONDS),
                "First task has not been invoked"
        );

        assertTrue(
                runLatch2.await(10, TimeUnit.SECONDS),
                "Second task has not been invoked"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.WAITING, info.getStatus());

        ParallelTaskData stepTaskData = getStepData(task, info);
        ExecutionResult firstResult = getExecutionResult(stepTaskData, subTask1.getId());

        assertEquals(TaskStatus.WAITING, firstResult.getNextStatus());

        LocalDateTime substepNextRunTime = firstResult.getNextRunTime().orElse(null);
        assertNotNull(substepNextRunTime);

        assertEquals(substepNextRunTime.withNano(0), info.getNextRunTime().withNano(0));
    }

    /**
     * Подзадачи, которые уже были выполнены не запускаются повторно
     */
    @Test
    void testDoNotRunCompletedTask() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                return ExecutionResult.completed();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                return ExecutionResult.completed();
            }
        };


        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                ExecutionResult.completed()
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                ExecutionResult.yield()
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        startService(task);

        assertFalse(
                runLatch1.await(2, TimeUnit.SECONDS),
                "Completed subtask has been executed again"
        );

        assertTrue(
                runLatch2.await(2, TimeUnit.SECONDS),
                "Waited subtask has not been executed"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        ParallelTaskData data = getStepData(task, info);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.COMPLETING, result1.getNextStatus());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.COMPLETING, result2.getNextStatus());
    }

    /**
     * Выполняются только те подзадачи время выполнения которых подошло
     */
    @Test
    void testRunOnlyEligibleTasks() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                return ExecutionResult.completed();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                return ExecutionResult.completed();
            }
        };


        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.WAITING,
                        LocalDateTime.now().minusMinutes(1),
                        null
                )
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.WAITING,
                        LocalDateTime.now().plusHours(1),
                        null
                )
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        startService(task);

        assertTrue(
                runLatch1.await(2, TimeUnit.SECONDS),
                "Eligible subtask has not been executed"
        );

        assertFalse(
                runLatch2.await(2, TimeUnit.SECONDS),
                "Not eligible subtask has been executed"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        ParallelTaskData data = getStepData(task, info);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.COMPLETING, result1.getNextStatus());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.WAITING, result2.getNextStatus());
    }

    /**
     * При остановке таски выполняется метод onCancel() всех её подзадач.
     *
     * При этом метод вызывается только для тех поздадач остановка которых еще не была обработана
     * (статус не CANCELLED). После обработки остановки задачи она переводится в статус CANCELLED
     */
    @Test
    void testExecuteCancelMethodOfSubTasksOnCancel() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        CountDownLatch onCancelLatch1 = new CountDownLatch(1);
        CountDownLatch onCancelLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                return ExecutionResult.completed();
            }

            @Override
            public void onCancel(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onCancelLatch1.countDown();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                return ExecutionResult.completed();
            }

            @Override
            public void onCancel(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onCancelLatch2.countDown();
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.WAITING,
                        LocalDateTime.now().minusMinutes(1),
                        null
                )
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.CANCELLED,
                        null,
                        null
                )
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        createService(task);

        service.cancelTask(taskId);

        service.start();

        assertFalse(
                runLatch1.await(2, TimeUnit.SECONDS),
                "Cancelled subtask has been executed"
        );

        assertFalse(
                runLatch2.await(2, TimeUnit.SECONDS),
                "Cancelled subtask has been executed"
        );

        assertTrue(
                onCancelLatch1.await(2, TimeUnit.SECONDS),
                "onCancel() method of first task has not been executed"
        );

        assertFalse(
                onCancelLatch2.await(2, TimeUnit.SECONDS),
                "onCancel() method of second task has been executed"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        ParallelTaskData data = getStepData(task, info);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.CANCELLED, result1.getNextStatus());
        assertFalse(result1.getNextRunTime().isPresent());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.CANCELLED, result2.getNextStatus());
        assertFalse(result2.getNextRunTime().isPresent());
    }

    /**
     * При отмене таски для успешно завершенных тасков метод onCancel() не вызывается
     */
    @Test
    void testDoNotRunOnCancelForCompletedSubtasks() throws Exception {
        CountDownLatch onCancelLatch1 = new CountDownLatch(1);
        CountDownLatch onCancelLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Override
            public void onCancel(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onCancelLatch1.countDown();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Override
            public void onCancel(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onCancelLatch2.countDown();
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.WAITING,
                        LocalDateTime.now().minusMinutes(1),
                        null
                )
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                ExecutionResult.completed()
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        createService(task);

        service.cancelTask(taskId);

        service.start();

        assertTrue(
                onCancelLatch1.await(2, TimeUnit.SECONDS),
                "onCancel() method of first task has not been executed"
        );

        assertFalse(
                onCancelLatch2.await(2, TimeUnit.SECONDS),
                "onCancel() method of completed task has been executed"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        ParallelTaskData data = getStepData(task, info);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.CANCELLED, result1.getNextStatus());
        assertFalse(result1.getNextRunTime().isPresent());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.COMPLETING, result2.getNextStatus());
        assertFalse(result2.getNextRunTime().isPresent());
    }

    /**
     * В случае если одна из подзадач завершается ошибкой таска переходит в состояние WAITING.
     * Это нужно для того чтобы при следующем выполнении таски незавершенные подзадачи были
     * отменены и уже после этого таска пофейлилась.
     *
     * @see testIfAtLeastOneFailedTaskAppearedOtherTasksMustBeCancelled
     */
    @Test
    void testIfOneOfSubtasksThrewAnExceptionTaskMovesToWaitingState() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(3);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) throws Exception {
                barrier.await(5, TimeUnit.SECONDS);
                return ExecutionResult.completed();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) throws Exception {
                barrier.await(5, TimeUnit.SECONDS);
                if (context == null) {
                    throw new RuntimeException("Subtask failed");
                }
                return ExecutionResult.completed();
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        startService(task);
        long taskId = service.submitTask(TASK_ID, new SequenceTaskData());

        barrier.await(5, TimeUnit.SECONDS);

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.WAITING, info.getStatus());
        assertNull(info.getNextRunTime());

        ParallelTaskData data = getStepData(task, info);

        ExecutionResult subResult1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.COMPLETING, subResult1.getNextStatus());

        ExecutionResult subResult2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.FAILING, subResult2.getNextStatus());
        assertThat(subResult2.getNextStatusMessage(), containsString("Subtask failed"));
    }

    /**
     * В случае если в результате отмены таски прерывается поток её выполнения
     * вместе с ним прерываются потоки выполнения её подзадач
     */
    @Test
    void testPropagateInterruptionOnSubtaskThreads() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch exceptionLatch = new CountDownLatch(1);

        SubTask1 subTask = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) throws Exception {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    Thread.sleep(25_000);
                } catch (InterruptedException e) {
                    exceptionLatch.countDown();
                    throw e;
                }
                return ExecutionResult.completed();
            }
        };

        DefaultTask task = defaultTask(subTask);

        startService(task);
        long taskId = service.submitTask(TASK_ID, new SequenceTaskData());

        barrier.await(5, TimeUnit.SECONDS);

        service.cancelTask(taskId);

        assertTrue(
                exceptionLatch.await(2, TimeUnit.SECONDS),
                "Subtask thread has not been interrupted"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.CANCELLING, info.getStatus());
    }

    /**
     * В случае если выполнение хотя бы одной из подзадач завершилось ошибкой
     * незавершенные подзадачи отменяются а сама таска фейлится
     *
     * @see testIfOneOfSubtasksThrewAnExceptionTaskMovesToWaitingState()
     */
    @Test
    void testIfAtLeastOneFailedTaskAppearedOtherTasksMustBeCancelled() throws Exception {
        CountDownLatch runLatch1 = new CountDownLatch(1);
        CountDownLatch runLatch2 = new CountDownLatch(1);

        CountDownLatch onCancelLatch1 = new CountDownLatch(1);
        CountDownLatch onCancelLatch2 = new CountDownLatch(1);

        CountDownLatch onFailLatch1 = new CountDownLatch(1);
        CountDownLatch onFailLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                runLatch1.countDown();
                return ExecutionResult.completed();
            }

            @Override
            public void onCancel(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onCancelLatch1.countDown();
            }

            @Override
            public void onFail(String message, Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onFailLatch1.countDown();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                runLatch2.countDown();
                return ExecutionResult.completed();
            }

            @Override
            public void onCancel(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onCancelLatch2.countDown();
            }

            @Override
            public void onFail(String message, Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onFailLatch2.countDown();
            }
        };


        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.FAILING,
                        null,
                        null
                )
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                ExecutionResult.yield()
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        startService(task);

        assertTrue(
                onCancelLatch2.await(2, TimeUnit.SECONDS),
                "Not completed task must be cancelled"
        );

        assertFalse(
                runLatch1.await(2, TimeUnit.SECONDS),
                "Failed task must not run again"
        );

        assertFalse(
                runLatch2.await(2, TimeUnit.SECONDS),
                "Not completed task must not run"
        );

        assertFalse(
                onCancelLatch1.await(2, TimeUnit.SECONDS),
                "Failed task must not be cancelled"
        );

        assertTrue(
                onFailLatch1.await(2, TimeUnit.SECONDS),
                "onFail() method of failed task has not been called"
        );

        assertFalse(
                onFailLatch2.await(5, TimeUnit.SECONDS),
                "onFail() method of succeed task has been called"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILED, info.getStatus());

        ParallelTaskData data = getStepData(task, info);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.FAILED, result1.getNextStatus());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.CANCELLED, result2.getNextStatus());
    }

    /**
     * В случае если таска приостановлена в процессе выполнения двух её подзадач то:
     *
     * 1. потоки выполнения этих подзадач прерываются
     * 2. для обоих подзадач вызывается метод обработки приостановки onPause()
     * 3. после того как приостановка обработана обе подзадачи переводятся в статус PAUSED
     */
    @Test
    void testCancellTaskWithTwoParallelSubtasks() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(3);
        CountDownLatch onPauseLatch1 = new CountDownLatch(1);
        CountDownLatch onPauseLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData1 data, Control<SubTaskData1> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                new CountDownLatch(1).await();
                return ExecutionResult.completed();
            }

            @Override
            public void onPause(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onPauseLatch1.countDown();
            }
        };

        SubTask2 subTask2 = new SubTask2() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, SubTaskData2 data, Control<SubTaskData2> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                new CountDownLatch(1).await();
                return ExecutionResult.completed();
            }

            @Override
            public void onPause(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onPauseLatch2.countDown();
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                null
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                null
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        createService(task);

        service.start();

        barrier.await(10, TimeUnit.SECONDS);

        service.pauseTask(taskId);

        assertTrue(onPauseLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(onPauseLatch2.await(10, TimeUnit.SECONDS));

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        ParallelTaskData data = getStepData(task, info);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.PAUSED, result1.getNextStatus());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.PAUSED, result2.getNextStatus());
    }

    /**
     * При возобновлении таски не должен вызываться метод onRecover() у успешно завершившихся подтасок
     */
    @Test
    void testDoNotRunOnRecoverForCompletedSubtasks() throws Exception {
        CountDownLatch onFailLatch1 = new CountDownLatch(1);
        CountDownLatch onFailLatch2 = new CountDownLatch(1);

        CountDownLatch onRecoverLatch1 = new CountDownLatch(1);
        CountDownLatch onRecoverLatch2 = new CountDownLatch(1);

        SubTask1 subTask1 = new SubTask1() {
            @Override
            public void onRecover(Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onRecoverLatch1.countDown();
            }

            @Override
            public void onFail(String message, Void context, SubTaskData1 data, Control<SubTaskData1> control) {
                onFailLatch1.countDown();
            }
        };

        SubTask2 subTask2 = new SubTask2() {
            @Override
            public void onRecover(Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onRecoverLatch2.countDown();
            }

            @Override
            public void onFail(String message, Void context, SubTaskData2 data, Control<SubTaskData2> control) {
                onFailLatch2.countDown();
            }
        };

        DefaultTask task = defaultTask(subTask1, subTask2);

        SubTask subTask1Data = new SubTask(
                subTask1.getId(),
                null,
                new ExecutionResult(
                        TaskStatus.FAILING,
                        null,
                        null
                )
        );

        SubTask subTask2Data = new SubTask(
                subTask2.getId(),
                null,
                ExecutionResult.completed()
        );

        long taskId = saveTaskData(task, subTask1Data, subTask2Data);

        startService(task);

        assertTrue(
                onFailLatch1.await(2, TimeUnit.SECONDS),
                "onFail() method of failed task has not been called"
        );

        assertFalse(
                onFailLatch2.await(5, TimeUnit.SECONDS),
                "onFail() method of succeed task has been called"
        );

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILED, info.getStatus());

        service.recoverTask(taskId);

        assertTrue(
                onRecoverLatch1.await(2, TimeUnit.SECONDS),
                "onRecover() method of failed task has not been called"
        );

        assertFalse(
                onRecoverLatch2.await(5, TimeUnit.SECONDS),
                "onRecover() method of succeed task has been called"
        );

        service.stopGracefully();

        TaskInstanceInfo newInfo = clusterTasksDAO.getTask(taskId);
        assertNotNull(newInfo);
        assertEquals(TaskStatus.COMPLETED, newInfo.getStatus());

        ParallelTaskData data = getStepData(task, newInfo);

        ExecutionResult result1 = getExecutionResult(data, subTask1.getId());
        assertEquals(TaskStatus.COMPLETING, result1.getNextStatus());

        ExecutionResult result2 = getExecutionResult(data, subTask2.getId());
        assertEquals(TaskStatus.COMPLETING, result2.getNextStatus());
    }

    private long saveTaskData(DefaultTask task, SubTask... subTasks) {
        ParallelTaskData parallelTaskData = new ParallelTaskData();
        parallelTaskData.setSubTasks(Arrays.asList(subTasks));

        SequenceTaskData data = new SequenceTaskData();
        data.setSteps(
                Collections.singletonMap(task.getId(), toJsonNode(parallelTaskData))
        );

        TaskInstanceInfo info = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setStatus(TaskStatus.WAITING)
                .setData(jsonSerializer.writeObjectAsString(data));

        return clusterTasksDAO.insertTask(info).getId();
    }

    private <T> T getData(ParallelTaskData data, String subTaskId, Class<T> clazz) {
        List<SubTask> subTasks = data.getSubTasks();
        assertNotNull(subTasks);

        return data.getSubTasks().stream()
                .filter(x -> subTaskId.equals(x.getId()))
                .findFirst()
                .map(SubTask::getData)
                .map(x -> jsonDeserializer.readObject(clazz, x.toString()))
                .orElse(null);
    }

    private ExecutionResult getExecutionResult(ParallelTaskData data, String subTaskId) {
        List<SubTask> subTasks = data.getSubTasks();
        assertNotNull(subTasks);

        return data.getSubTasks().stream()
                .filter(x -> subTaskId.equals(x.getId()))
                .findFirst()
                .map(SubTask::getExecutionResult)
                .orElse(null);
    }

    private void createService(ParallelTask<Void> task) {
        service = serviceFactory.create(
                Collections.singleton(new TaskWrapper(jsonDeserializer, jsonSerializer, task))
        );
    }

    private void startService(ParallelTask<Void> task) {
        createService(task);
        service.start();
    }

    private DefaultTask defaultTask(Task<? super Void, ?>... steps) {
        return new DefaultTask(jsonSerializer, jsonDeserializer, steps);
    }

    private JsonNode toJsonNode(Object value) {
        String json = jsonSerializer.writeObjectAsString(value);
        return jsonDeserializer.readObject(JsonNode.class, json);
    }

    private ParallelTaskData getStepData(ParallelTask<Void> parallelTask, TaskInstanceInfo info) {
        String rawData = info.getData();
        assertNotNull(rawData);

        SequenceTaskData taskData = jsonDeserializer.readObject(SequenceTaskData.class, rawData);
        return jsonDeserializer.readObject(
                ParallelTaskData.class,
                taskData.getSteps().get(parallelTask.getId()).toString()
        );
    }
}
