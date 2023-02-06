package ru.yandex.market.crm.tasks.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
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
import ru.yandex.market.crm.tasks.domain.TaskIncident;
import ru.yandex.market.crm.tasks.domain.TaskInstanceInfo;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceFactory;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.misc.thread.ThreadUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author apershukov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClusterTasksServiceTestConfig.class)
public class ClusterTasksServiceTest {

    private static class TaskData {

        @JsonProperty("value")
        private String field;

        TaskData(String field) {
            this.field = field;
        }

        @SuppressWarnings("unused")
        public TaskData() {
        }

        String getField() {
            return field;
        }

        void setField(String field) {
            this.field = field;
        }
    }

    private static class DefaultTask<T> implements Task<Void, T> {

        @Nonnull
        @Override
        public ExecutionResult run(Void v, T status, Control<T> control) throws Exception {
            return ExecutionResult.completed();
        }

        @Override
        public String getId() {
            return TASK_ID;
        }
    }

    private static final String TASK_ID = "TASK_ID";
    private static final Task<Void, Void> DEFAULT_TASK = new DefaultTask<>() {};

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    private ClusterTasksDAO tasksDAO;

    @Inject
    private TaskIncidentsDAO taskIncidentsDAO;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private ClusterTasksServiceFactory serviceFactory;

    private ClusterTasksService service;

    @AfterEach
    public void tearDown() throws Exception {
        dbTestTool.runScript("/sql/clearDatabase.sql");
        if (service != null) {
            service.stop();
        }
    }

    /**
     * Новая таска добавляется в статусе WAITING с пустым host_key
     */
    @Test
    void testScheduleNewTask() {
        prepareService(DEFAULT_TASK);

        long id = service.submitTask(TASK_ID, new TaskData("bar"));

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(id);
        assertNotNull(taskInstanceInfo);
        assertEquals(TASK_ID, taskInstanceInfo.getTaskId());
        assertEquals(TaskStatus.WAITING, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
        assertNotNull(taskInstanceInfo.getCreationTime());

        String rawData = taskInstanceInfo.getData();
        assertNotNull(rawData);

        TaskData data = jsonDeserializer.readObject(TaskData.class, rawData);
        assertEquals("bar", data.getField());
    }

    /**
     * При немедленном вызове таски сразу выполнятся её метод run а затем onSuccess
     */
    @Test
    void testRunTaskImmediately() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(2);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                queue.add(1);
                return ExecutionResult.completed();
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                queue.add(2);
            }
        };

        startService(task);

        long taskId = service.submitTask(TASK_ID, null);

        Integer item1 = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(item1, "Task has not been executed");
        assertEquals(1, (int) item1, "Wrong order of task methods execution");

        Integer item2 = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(item2, "Task onSuccess handler has not been executed");
        assertEquals(2, (int) item2, "Wrong order of task methods execution");

        waitForStatus(taskId, TaskStatus.COMPLETED);
    }

    /**
     * В процессе выполнения:
     * 1. Статус таски выставляется как RUNNING
     * 2. Заполняется ключ инстанса
     */
    @Test
    void testSetRunningStatusWhileRunning() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Task<Void, Void> task = new Task<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) throws Exception {
                latch1.countDown();
                latch2.await(5, TimeUnit.MINUTES);
                return ExecutionResult.completed();
            }

            @Override
            public String getId() {
                return TASK_ID;
            }
        };

        startService(task);

        long id = service.submitTask(TASK_ID, null);

        latch1.await(5, TimeUnit.MINUTES);

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(id);
        assertNotNull(taskInstanceInfo, "No info for running task");
        assertEquals(TaskStatus.RUNNING, taskInstanceInfo.getStatus());
        assertNotNull(taskInstanceInfo.getNodeKey());

        latch2.countDown();
    }

    /**
     * На выполнение берутся только таски в статусе WAITING
     */
    @Test
    void testTakeForExecutionOnlyWaitingTasks() {
        insertTaskInfo(TaskStatus.RUNNING);
        long taskId = insertTaskInfo(TaskStatus.WAITING);
        insertTaskInfo(TaskStatus.PAUSED);

        startService();

        waitForStatus(taskId, TaskStatus.COMPLETED);

        Set<TaskStatus> statuses = tasksDAO.getTasks(0, Integer.MAX_VALUE).stream()
                .map(TaskInstanceInfo::getStatus)
                .collect(Collectors.toSet());

        assertEquals(ImmutableSet.of(TaskStatus.RUNNING, TaskStatus.PAUSED, TaskStatus.COMPLETED), statuses);
    }

    /**
     * Если с таской связана какая-то информация, она в десериализованном виде в
     * качестве аргумента передается в метод run
     */
    @Test
    void testTaskStatusIsPassedToRunMethod() throws InterruptedException {
        insertTaskInfo(TaskStatus.WAITING, new TaskData("foo"), null);

        BlockingQueue<TaskData> queue = new ArrayBlockingQueue<>(1);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, TaskData status, Control<TaskData> control) {
                queue.add(status);
                return ExecutionResult.completed();
            }
        };

        startService(task);

        TaskData data = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(data, "No data");
        assertEquals("foo", data.getField());
    }

    /**
     * Если с таской связана какая-то информация, она в десериализованном виде в
     * качестве аргумента передается в метод onSuccess
     */
    @Test
    void testTaskStatusIsPassedToOnSuccessMethod() throws InterruptedException {
        insertTaskInfo(TaskStatus.WAITING, new TaskData("foo"), null);

        BlockingQueue<TaskData> queue = new ArrayBlockingQueue<>(1);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, TaskData status, Control<TaskData> control) {
                return ExecutionResult.completed();
            }

            @Override
            public void onSuccess(Void context, TaskData status, Control<TaskData> control) {
                queue.add(status);
            }
        };

        startService(task);

        TaskData data = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(data, "No data");
        assertEquals("foo", data.getField());
    }

    /**
     * Если обработчик таски в качестве результата вернул переход в ожидание:
     * 1. Таска переходит в статус WAITING
     * 2. У нее заполняется поле next_run_time
     * 3. Метод onSuccess() обработчика не вызывается
     */
    @Test
    void testTransferTaskToWaitingState() throws InterruptedException {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean onSucceesCalled = new AtomicBoolean(false);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                latch.countDown();
                return ExecutionResult.repeatIn(Duration.ofSeconds(30));
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                onSucceesCalled.set(true);
            }
        };

        startService(task);

        latch.await(5, TimeUnit.SECONDS);
        service.stopGracefully();

        TaskInstanceInfo info = tasksDAO.getTask(taskId);

        assertNotNull(info);
        assertEquals(TaskStatus.WAITING, info.getStatus());
        assertNull(info.getNodeKey());
        assertNotNull(info.getNextRunTime());

        assertFalse(onSucceesCalled.get(), "onSuccess method has been called");
    }

    /**
     * Ожидающая таска, время выполнения которой еще не пришло, не выполняется
     */
    @Test
    void testTaskNotEligibleForExecutionIsNotExecuted() throws Exception {
        insertWaiting(LocalDateTime.now().plusDays(1));

        CountDownLatch latch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                latch.countDown();
                return ExecutionResult.completed();
            }
        };

        startService(task);

        boolean hasBeenCalled = latch.await(2, TimeUnit.SECONDS);
        assertFalse(hasBeenCalled, "Not eligible task has been executed");
    }

    /**
     * Ожидающая таска, время выполнения которой пришло, выполняется
     */
    @Test
    void testTaskEligibleForExecutionIsExecuted() throws Exception {
        insertWaiting(LocalDateTime.now().minusMinutes(5));

        CountDownLatch latch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                latch.countDown();
                return ExecutionResult.completed();
            }
        };

        startService(task);

        boolean hasBeenCalled = latch.await(2, TimeUnit.SECONDS);
        assertTrue(hasBeenCalled, "Eligible task has not been executed");
    }

    /**
     * При отмене ожидающей таски, вызывается её метод onCancel(). При этом:
     * 1. Пока обработка отмены полностью не завершится таска находится в состоянии CANCELLING
     * 2. При выполнении onCancel() заполняется instance key таски
     * 3. В метод onCancel() в качестве аргумента передается десериализованная информация, связанная с таской
     * 4. После завершения обработки отмены таска переходит в состояние CANCELLED.
     *    Её instance key при этом сбрасывается.
     */
    @Test
    void testOnCancelWaitingTask() throws InterruptedException {
        long taskId = insertTaskInfo(
                TaskStatus.WAITING,
                new TaskData("baz"),
                LocalDateTime.now().plusMinutes(30)
        );

        BlockingQueue<TaskData> queue = new ArrayBlockingQueue<>(1);

        CountDownLatch latch = new CountDownLatch(1);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Override
            public void onCancel(Void context, TaskData status, Control<TaskData> control) throws Exception {
                queue.add(status);
                latch.await(5, TimeUnit.SECONDS);
            }
        };

        startService(task);

        service.cancelTask(taskId);

        TaskData data = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(
                data,
                "Ether onCancel has not been executed or data is null"
        );
        assertEquals("baz", data.getField());

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.CANCELLING, info.getStatus());
        assertNotNull(info.getNodeKey(), "Instance key is not specified after acquiring");

        latch.countDown();

        service.stopGracefully();

        info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.CANCELLED, info.getStatus());
        assertNull(info.getNodeKey(), "Instance key is specified for cancelled task");
    }

    /**
     * В случае если у таски в статусе CANCELLING заполнено свойство node key
     * метод onCancel() таски не выполняется, т. к. считается что отмена уже обрабатывается
     * на другом инстансе
     */
    @Test
    void testOnCancelIsNotExecutedForCancelledTasksWithInstanceKey() throws Exception {
        insertTaskInfo(
                new TaskInstanceInfo()
                        .setTaskId(TASK_ID)
                        .setStatus(TaskStatus.CANCELLING)
                        .setNodeKey("host.yandex.net")
        );

        CountDownLatch latch = new CountDownLatch(1);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Override
            public void onCancel(Void context, TaskData status, Control<TaskData> control) {
                latch.countDown();
            }
        };

        startService(task);

        boolean hasBeenCalled = latch.await(5, TimeUnit.SECONDS);
        assertFalse(hasBeenCalled);
    }

    /**
     * В случае если отменяется уже выполняемая таска:
     * 1. Поток, в котором она выполняется, прерывается
     * 2. На том же инстансе после прерывания потока происходит выполнение метода onCancel()
     * 3. После выполнения onCancel() таска переводится в статус CANCELLED
     */
    @Test
    void testIfCancelIsInvokedOnRunningTaskItsThreadIsInterrupted() throws InterruptedException {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch onSuccessLatch = new CountDownLatch(1);

        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) throws Exception {
                latch1.countDown();
                try {
                    latch2.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    queue.add("interrupted");
                    throw e;
                }
                return ExecutionResult.completed();
            }

            @Override
            public void onCancel(Void context, Void status, Control<Void> control) {
                queue.add("cancelled");
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                onSuccessLatch.countDown();
            }
        };

        startService(task);

        latch1.await(10, TimeUnit.SECONDS);

        service.cancelTask(taskId);

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.CANCELLING, taskInstanceInfo.getStatus());
        assertNotNull(taskInstanceInfo.getNodeKey());

        assertEquals(
                "interrupted",
                queue.poll(10, TimeUnit.SECONDS),
                "Interrupted exception has not been thrown"
        );

        assertEquals(
                "cancelled",
                queue.poll(10, TimeUnit.SECONDS),
                "onCancel() method has not been invoked"
        );

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been invoked"
        );

        service.stopGracefully();

        taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.CANCELLED, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
    }

    /**
     * В случае если при отмене таски метод run() успел доработать до конца
     * таска все равно переводится в статус CANCELLED. При этом метод onSuccess()
     * не выполняется и выполняется метод onCancel()
     */
    @Test
    void testIfRunMethodIgnoredThreadInterruptionTaskIsCancelledAnyway() throws InterruptedException {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch onCancelLatch = new CountDownLatch(1);
        CountDownLatch onSuccessLatch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                latch1.countDown();
                try {
                    latch2.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
                return ExecutionResult.completed();
            }

            @Override
            public void onCancel(Void context, Void status, Control<Void> control) {
                onCancelLatch.countDown();
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                onSuccessLatch.countDown();
            }
        };

        startService(task);

        latch1.await(10, TimeUnit.SECONDS);

        service.cancelTask(taskId);

        assertTrue(
                onCancelLatch.await(10, TimeUnit.SECONDS),
                "onCancel() method has not been invoked"
        );

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been invoked"
        );

        service.stopGracefully();

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.CANCELLED, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
    }

    /**
     * При остановке сервиса потоки в которых выполняются таски прерываются.
     * Таска переводится в статус WAITING с пустым node key.
     * При этом методы onSuccess() и onCancel() не вызываются
     */
    @Test
    void testInterruptRunningTasksOnShutdown() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch interruptedThrownLatch = new CountDownLatch(1);
        CountDownLatch onSuccessLatch = new CountDownLatch(1);
        CountDownLatch onCancelLatch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    interruptedThrownLatch.countDown();
                    throw e;
                }

                return ExecutionResult.completed();
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                onSuccessLatch.countDown();
            }

            @Override
            public void onCancel(Void context, Void status, Control<Void> control) {
                onCancelLatch.countDown();
            }
        };

        startService(task);

        barrier.await(10, TimeUnit.SECONDS);

        service.stop();

        assertTrue(
                interruptedThrownLatch.await(5, TimeUnit.SECONDS),
                "Interrupt exception has not been thrown"
        );

        assertFalse(
                onCancelLatch.await(2, TimeUnit.SECONDS),
                "onCancel() method has been invoked"
        );

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been invoked"
        );

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.WAITING, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
    }

    /**
     * В случае если выполняющаяся таска проигнорировала остановку своего потока,
     * её методы onSuccess() и onCancel() не выполняются. Таска переводится в тот статус
     * который вернул обработчик (в данном случае COMPLETING).
     */
    @Test
    void testTaskIgnoringInterruptionOnShutdown() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch onSuccessLatch = new CountDownLatch(1);
        CountDownLatch onCancelLatch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException ignored) {}
                return ExecutionResult.completed();
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                onSuccessLatch.countDown();
            }

            @Override
            public void onCancel(Void context, Void status, Control<Void> control) {
                onCancelLatch.countDown();
            }
        };

        startService(task);

        barrier.await(10, TimeUnit.SECONDS);

        assertFalse(
                onCancelLatch.await(2, TimeUnit.SECONDS),
                "onCancel() method has been invoked"
        );

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been invoked"
        );

        service.stopGracefully();

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.COMPLETING, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
    }

    /**
     * В случае если отмена выполняемой таски произошла в процессе остановки сервиса
     * и обработчик проигнорировал остановку потока метод onCancel() не выполняется
     * и таска остается в статусе CANCELLING
     */
    @Test
    void testIfTaskWasCancelledDuringShutdownItStaysInCancellingState() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch onSuccessLatch = new CountDownLatch(1);
        CountDownLatch onCancelLatch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ignored) {}
                return ExecutionResult.completed();
            }

            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                onSuccessLatch.countDown();
            }

            @Override
            public void onCancel(Void context, Void status, Control<Void> control) {
                onCancelLatch.countDown();
            }
        };

        startService(task);

        barrier.await(10, TimeUnit.SECONDS);

        service.cancelTask(taskId);

        service.stop();

        assertFalse(
                onCancelLatch.await(2, TimeUnit.SECONDS),
                "onCancel() method has been invoked"
        );

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been invoked"
        );

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.CANCELLING, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
    }

    /**
     * В случае если выполнение таски завершается с ошибкой, вызывается метод onFail() в который в
     * качестве аргумента передается десериализованная информация, связанная с таской
     */
    @Test
    void testHandleErrorOnRun() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING, new TaskData("foo"), null);

        CountDownLatch onSuccessLatch = new CountDownLatch(1);
        CountDownLatch onFailLatch = new CountDownLatch(1);
        BlockingQueue<TaskData> dataQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(1);

        Task<Void, TaskData> task = new DefaultTask<>() {
            @Nonnull
            @Override
            public ExecutionResult run(Void context, TaskData status, Control<TaskData> control) {
                throw new RuntimeException("Task execution failed");
            }

            @Override
            public void onSuccess(Void context, TaskData status, Control<TaskData> control) {
                onSuccessLatch.countDown();
            }

            @Override
            public void onFail(String message, Void context, TaskData status, Control<TaskData> control) throws Exception {
                dataQueue.add(status);
                messageQueue.add(message);
                onFailLatch.await(10, TimeUnit.SECONDS);
            }
        };

        startService(task);

        TaskData data = dataQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(
                data,
                "Either data was not passed in onFail() method or it was not invoked"
        );

        assertEquals("foo", data.getField());

        String message = messageQueue.poll(10, TimeUnit.SECONDS);
        assertThat(message, containsString("Task execution failed"));

        TaskInstanceInfo taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.FAILING, taskInstanceInfo.getStatus());
        assertNotNull(taskInstanceInfo.getNodeKey());

        onFailLatch.countDown();

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been invoked"
        );

        service.stopGracefully();

        taskInstanceInfo = tasksDAO.getTask(taskId);
        assertNotNull(taskInstanceInfo);
        assertEquals(TaskStatus.FAILED, taskInstanceInfo.getStatus());
        assertNull(taskInstanceInfo.getNodeKey());
        assertThat(taskInstanceInfo.getStatusMessage(), containsString("Task execution failed"));

        List<TaskIncident> incidents = taskIncidentsDAO.getAllIncidents();
        assertEquals(1, incidents.size());

        TaskIncident incident = incidents.get(0);
        assertEquals(task.getId(), incident.getTaskId());
        assertEquals(taskId, incident.getTaskInstanceId());
        assertThat(incident.getMessage(), containsString("Task execution failed"));
        assertNotNull(incident.getTime());
    }

    /**
     * Когда таска возвращает результат, означающий что она завершилась с ошибкой,
     * происходит регистрация инцидента.
     */
    @Test
    void testWhenTaskReturnsFailingResultIncidentIsRegistered() {
        long taskId = insertWaiting(null);

        Task<Void, TaskData> task = new DefaultTask<>() {
            @Nonnull
            @Override
            public ExecutionResult run(Void v, TaskData status, Control<TaskData> control) {
                return ExecutionResult.failed("Error message");
            }
        };

        startService(task);

        waitForStatus(taskId, TaskStatus.FAILED);

        List<TaskIncident> incidents = taskIncidentsDAO.getAllIncidents();
        assertThat(incidents, hasSize(1));

        TaskIncident incident = incidents.get(0);
        assertEquals(task.getId(), incident.getTaskId());
        assertEquals(taskId, incident.getTaskInstanceId());
        assertEquals("Error message", incident.getMessage());
        assertNotNull(incident.getTime());
    }

    /**
     * В случае если обработка отмены таски завершается с ошибкой вызывается метод onFail()
     */
    @Test
    void testHandleErrorOnCancel() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.CANCELLING);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {
            @Override
            public void onCancel(Void context, Void status, Control<Void> control) {
                throw new RuntimeException("onCancel() invocation failed");
            }

            @Override
            public void onFail(String message, Void context, Void status, Control<Void> control) throws Exception {
                latch1.countDown();
                latch2.await(10, TimeUnit.SECONDS);
            }
        };

        startService(task);

        assertTrue(
                latch1.await(10, TimeUnit.SECONDS),
                "onFail() has not been called"
        );

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILING, info.getStatus());
        assertNotNull(info.getNodeKey());

        latch2.countDown();

        service.stopGracefully();

        info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILED, info.getStatus());
        assertNull(info.getNodeKey());
        assertThat(info.getStatusMessage(), containsString("onCancel() invocation failed"));
    }

    /**
     * В случае если обработка успешного завершения таски завершается с ошибкой вызывается метод onFail()
     */
    @Test
    void testHandleErrorOnSuccess() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.COMPLETING);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {
            @Override
            public void onSuccess(Void context, Void status, Control<Void> control) {
                if (status == null) {
                    throw new RuntimeException("onSuccess() invocation failed");
                }
            }

            @Override
            public void onFail(String message, Void context, Void status, Control<Void> control) throws Exception {
                latch1.countDown();
                latch2.await(10, TimeUnit.SECONDS);
            }
        };

        startService(task);

        assertTrue(
                latch1.await(10, TimeUnit.SECONDS),
                "onFail() has not been called"
        );

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILING, info.getStatus());
        assertNotNull(info.getNodeKey());

        latch2.countDown();

        service.stopGracefully();

        info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILED, info.getStatus());
        assertNull(info.getNodeKey());
        assertThat(info.getStatusMessage(), containsString("onSuccess() invocation failed"));
    }

    /**
     * В случае если выполняющаяся таска ставится на паузу поток в котором она выполнялась прерывается
     * и выполняется метод onPause() в который в качестве аргумента передается десериализованная информация,
     * связанная с задачей
     */
    @Test
    void testInterruptRunningTaskOnPause() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING, new TaskData("value"), null);

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch exceptionLatch = new CountDownLatch(1);
        CountDownLatch onPauseLatch = new CountDownLatch(1);

        BlockingQueue<TaskData> queue = new ArrayBlockingQueue<>(1);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, TaskData status, Control<TaskData> control) throws Exception {
                latch.countDown();
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    exceptionLatch.countDown();
                    throw e;
                }
                return ExecutionResult.completed();
            }

            @Override
            public void onPause(Void context, TaskData status, Control<TaskData> control) throws Exception {
                queue.add(status);
                onPauseLatch.await(10, TimeUnit.SECONDS);
            }
        };

        startService(task);

        latch.await(10, TimeUnit.SECONDS);

        service.pauseTask(taskId);

        assertTrue(
                exceptionLatch.await(10, TimeUnit.SECONDS),
                "Task thread has not been interrupted"
        );

        TaskData data = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(
                data,
                "Either data is empty or onPause() method has not been called"
        );
        assertEquals("value", data.getField());

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.PAUSING, info.getStatus());
        assertNotNull(info.getNodeKey());

        onPauseLatch.countDown();

        service.stopGracefully();

        info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.PAUSED, info.getStatus());
        assertNull(info.getNodeKey());
    }

    /**
     * Если приостановленная выполняющаяся таска проигнорировала остановку своего потока
     * она в любом случае будет приостановлена. При этом будет вызван метод onPause()
     * и не будет вызван метод onSuccess()
     */
    @Test
    void testIfTaskIgnoredInterruptionOnPauseHandlerIsCalledAnyway() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING);

        CountDownLatch runLatch = new CountDownLatch(1);
        CountDownLatch onPauseLatch = new CountDownLatch(1);
        CountDownLatch onSuccessLatch = new CountDownLatch(1);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, TaskData status, Control<TaskData> control) {
                runLatch.countDown();
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ignored) {}
                return ExecutionResult.completed();
            }

            @Override
            public void onSuccess(Void context, TaskData status, Control<TaskData> control) {
                onSuccessLatch.countDown();
            }

            @Override
            public void onPause(Void context, TaskData status, Control<TaskData> control) {
                onPauseLatch.countDown();
            }
        };

        startService(task);

        runLatch.await(10, TimeUnit.SECONDS);

        service.pauseTask(taskId);

        assertTrue(
                onPauseLatch.await(10, TimeUnit.SECONDS),
                "onPause() method has not been called"
        );

        assertFalse(
                onSuccessLatch.await(2, TimeUnit.SECONDS),
                "onSuccess() method has been called"
        );

        service.stopGracefully();

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.PAUSED, info.getStatus());
        assertNull(info.getNodeKey());
    }

    /**
     * Информация, связанная с таской, может быть изменена в процессе выполнения с помощью
     * специального объекта, передаваемого в обработчик в качестве аргумента
     */
    @Test
    void testChangeTaskDataWithControl() throws Exception {
        long taskId = insertTaskInfo(TaskStatus.WAITING, new TaskData("foo"), null);

        CyclicBarrier barrier = new CyclicBarrier(2);

        Task<Void, TaskData> task = new DefaultTask<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, TaskData status, Control<TaskData> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                status.setField("bar");
                control.saveData(status);
                return ExecutionResult.completed();
            }
        };

        startService(task);

        barrier.await(10, TimeUnit.SECONDS);

        service.stopGracefully();

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info.getData());

        TaskData data = jsonDeserializer.readObject(TaskData.class, info.getData());
        assertEquals("bar", data.getField());
    }

    @Test
    void testFailingTaskShouldBeFailed() {
        Task<Void, Void> task = new DefaultTask<>() {
            @Override
            public void onFail(String message, Void context, Void data, Control<Void> control) {
                throw new RuntimeException("Ooops!");
            }
        };

        long taskId = insertTaskInfo(TaskStatus.FAILING);
        startService(task);

        waitForStatus(taskId, TaskStatus.FAILED);
    }

    /**
     * Время на выполнение распределяется равномерно между тасками
     */
    @Test
    void testFairExecutionTimeDistribution() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Task<Void, Void> task1 = new Task<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                latch1.countDown();
                return ExecutionResult.yield();
            }

            @Override
            public String getId() {
                return "task_1";
            }
        };

        Task<Void, Void> task2 = new Task<>() {

            @Nonnull
            @Override
            public ExecutionResult run(Void context, Void status, Control<Void> control) {
                latch2.countDown();
                return ExecutionResult.yield();
            }

            @Override
            public String getId() {
                return "task_2";
            }
        };

        insertTaskInfo(
                new TaskInstanceInfo()
                        .setTaskId(task1.getId())
                        .setStatus(TaskStatus.WAITING)
        );

        insertTaskInfo(
                new TaskInstanceInfo()
                        .setTaskId(task2.getId())
                        .setStatus(TaskStatus.WAITING)
        );

        service = serviceFactory.create(
                Arrays.asList(task1, task2)
        );
        service.start();

        assertTrue(
                latch1.await(5, TimeUnit.SECONDS),
                "Task 1 has never been called"
        );

        assertTrue(
                latch2.await(5, TimeUnit.SECONDS),
                "Task 2 has never been called"
        );
    }

    /**
     * В случае если в процессе выполнения метода таски onFail() сервис был остановлен
     * таска остается в статусе FAILING и свойство node key таски очищается
     * (без этого она не будет взята на повторную обработку и застрянет в текущем статусе)
     */
    @Test
    void testIfServiceStopsDuringFailureHandlingTaskIsReleased() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch = new CountDownLatch(1);

        Task<Void, Void> task = new DefaultTask<>() {

            @Override
            public void onFail(String message, Void context, Void data, Control<Void> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
                latch.await();
            }
        };

        long taskId = insertTaskInfo(TaskStatus.FAILING);

        startService(task);

        barrier.await(10, TimeUnit.SECONDS);

        service.stop();

        TaskInstanceInfo info = tasksDAO.getTask(taskId);
        assertNotNull(info);
        assertEquals(TaskStatus.FAILING, info.getStatus());
        assertNull(info.getNodeKey());
    }

    /**
     * Если продолжить приостановленную таску у неё будет вызван метод onResume()
     * после чего таска продолжит выполнение и будет завершена естественным путем
     */
    @Test
    void testCallOnResumeForResumedTask() throws Exception {
        var barrier = new CyclicBarrier(2);
        var latch = new CountDownLatch(1);

        var task = new DefaultTask<Void>() {

            @Override
            public void onResume(Void context, Void data, Control<Void> control) throws Exception {
                barrier.await();
            }

            @Override
            public void onSuccess(Void context, Void data, Control<Void> control) {
                latch.countDown();
            }
        };

        long taskId = insertTaskInfo(TaskStatus.PAUSED);

        startService(task);

        service.resumeTask(taskId);

        barrier.await(1, TimeUnit.MINUTES);
        assertTrue(latch.await(1, TimeUnit.MINUTES));

        service.stopGracefully();

        var instance = tasksDAO.getTask(taskId);
        assertNotNull(instance);
        assertEquals(TaskStatus.COMPLETED, instance.getStatus());
    }

    private void startService() {
        startService(DEFAULT_TASK);
    }

    private void startService(Task<Void, ?> task) {
        prepareService(task);
        service.start();
    }

    private void prepareService(Task<Void, ?> task) {
        service = serviceFactory.create(
                Collections.singleton(task)
        );
    }

    private void waitForStatus(long taskId, TaskStatus targetStatus) {
        long deadline = System.currentTimeMillis() + 5_000;

        TaskStatus currentStatus = null;

        while (System.currentTimeMillis() < deadline) {
            var task = tasksDAO.getTask(taskId);
            assertNotNull(task);
            currentStatus = task.getStatus();
            if (currentStatus == targetStatus) {
                return;
            }

            ThreadUtils.sleep(500);
        }

        fail("Task is still in " + currentStatus + " status");
    }

    private long insertTaskInfo(TaskStatus status, Object data, LocalDateTime nextRunTime) {
        TaskInstanceInfo taskInstanceInfo = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setNextRunTime(nextRunTime)
                .setStatus(status)
                .setData(jsonSerializer.writeObjectAsString(data));

        return insertTaskInfo(taskInstanceInfo);
    }

    private long insertWaiting(LocalDateTime nextRunTime) {
        return insertTaskInfo(TaskStatus.WAITING, null, nextRunTime);
    }

    private long insertTaskInfo(TaskInstanceInfo taskInstanceInfo) {
        return tasksDAO.insertTask(taskInstanceInfo).getId();
    }

    private long insertTaskInfo(TaskStatus status) {
        return insertTaskInfo(status, null, null);
    }
}
