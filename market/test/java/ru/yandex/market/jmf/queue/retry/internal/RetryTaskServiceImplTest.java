package ru.yandex.market.jmf.queue.retry.internal;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.queue.retry.CreateTaskRequestBuilder;
import ru.yandex.market.jmf.queue.retry.CyclicalRetryTaskHandler;
import ru.yandex.market.jmf.queue.retry.RetryServiceTestConfiguration;
import ru.yandex.market.jmf.queue.retry.RetryTaskConfiguration;
import ru.yandex.market.jmf.queue.retry.RetryTaskPriority;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RetryServiceTestConfiguration.class)
@TestPropertySource("classpath:/ru/yandex/market/jmf/queue/retry/test.properties")
@Transactional(propagation = Propagation.NEVER)
public class RetryTaskServiceImplTest {

    @Inject
    RetryTaskServiceImpl service;
    @Inject
    RetryTaskProcessor processor;
    @Inject
    RetryTaskDao dao;
    @Inject
    RetryTaskTestUtils utils;
    @Inject
    RetryTaskTestHandler handler;
    @Inject
    CyclicalRetryTaskHandler cyclicalHandler;
    @Inject
    ObjectSerializeService serializeService;
    @Inject
    TxService txService;
    @Inject
    FastRetryTasksQueue queue;

    @Test
    public void checkFailureExceeded() {
        // настройка системы
        handler.setHandler((c) -> false);

        createTask(1);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        assertTaskIsFailed();
    }

    @Test
    public void checkFailure2Exceeded() {
        // настройка системы
        handler.setHandler((c) -> false);

        createTask(2);

        // вызов системы
        processor.processPendingTasksWithReset(queue);
        sleep(1500); // ждем 2 сек. чтобы у задачи наступило время следующей попытки ее выполнения
        handler.reset();
        handler.setHandler((c) -> false);
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        assertTaskIsFailed();
    }

    @Test
    public void checkExceptionExceeded() {
        // настройка системы
        handler.setHandler((c) -> {
            throw new RuntimeException();
        });

        createTask(1);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        assertTaskIsFailed();
    }


    @Test
    public void checkFailure() {
        // настройка системы
        handler.setHandler((c) -> false);

        createTask(2);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        Assertions.assertFalse(handler.isSuccessful());
        Assertions.assertFalse(handler.isFailure());

        RetryTask task = utils.getSingleTask();
        Assertions.assertEquals(1, task.getAttemptNumber(), "Сделали одну попытку");
    }

    @Test
    public void checkSerialization() {
        // настройка системы
        TestContext context = createTask(1);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        Assertions.assertNotNull(handler.getTask());
        Assertions.assertEquals(context.getValue(), handler.getTask().getValue());
    }

    @Test
    public void checkSuccessful() {
        // настройка системы
        createTask(1);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        assertTaskIsSuccessfullyCompleted();
    }

    /**
     * Проверяем, что если нет задач для выпонение, то сервис не ломается
     */
    @Test
    public void doNothing() {
        txService.runInNewTx(() -> utils.createTask(1000));
        Assertions.assertFalse(processor.processPendingTasksWithReset(queue));
    }

    @Test
    public void doNothingIfNoBeanDefinitionFound() {
        // настройка системы
        RetryTaskConfiguration configuration = new RetryTaskConfiguration();
        configuration.setHandler("UnknownHandler");
        configuration.setDelay(1000);

        RetryTask task = new RetryTask();
        task.setConfiguration(serializeService.serialize(configuration));
        task.setContext(RetryTaskTestUtils.randomBytea());
        task.setAttemptNumber(0);
        task.setNextAttemptTime(Now.offsetDateTime().minusSeconds(1));
        task.setPriority(RetryTaskPriority.NORMAL);

        add(task);

        sleep(100L);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        task = utils.getSingleTask();
        Assertions.assertEquals(0, task.getAttemptNumber());
    }

    @Test
    public void restoreRequestIdByDefault() {
        String initialRequestId = "123";
        setRequestId(initialRequestId);
        handler.setHandler(ignored -> {
            Assertions.assertEquals(initialRequestId, getRequestId());
            return true;
        });
        createTask(1);

        processor.processPendingTasksWithReset(queue);

        assertTaskIsSuccessfullyCompleted();
    }

    @Test
    public void restoreRequestIdWithSubRequestNumberByDefault() {
        String initialRequestId = "123";
        setRequestId(initialRequestId);
        RequestContextHolder.getContext().generateNextSubRequestId();
        handler.setHandler(ignored -> {
            Assertions.assertEquals(initialRequestId + "/1", getRequestId());
            return true;
        });
        createTask(1);

        processor.processPendingTasksWithReset(queue);

        assertTaskIsSuccessfullyCompleted();
    }

    @Test
    public void thereIsNoInitialRequestId_waitNewRequestIdInsideTheHandlerCall() {
        Assertions.assertNull(getRequestId());

        handler.setHandler(ignored -> {
            Assertions.assertNotNull(getRequestId());
            return true;
        });
        createTask(1);

        processor.processPendingTasksWithReset(queue);

        assertTaskIsSuccessfullyCompleted();
    }

    @Test
    public void disableRestoringRequestId_waitEmptyRequestIdInsideTheHandlerCall() {
        String initialRequestId = "123";
        setRequestId(initialRequestId);

        handler.setHandler(ignored -> {
            String handlerRequestId = getRequestId();
            Assertions.assertNotNull(handlerRequestId);
            Assertions.assertNotEquals(initialRequestId, handlerRequestId);
            return true;
        });
        createTaskWithoutRestoringRequestId(1);

        processor.processPendingTasksWithReset(queue);

        assertTaskIsSuccessfullyCompleted();
    }

    @Test
    public void batchCreate() {
        RetryTaskConfiguration configuration = getTestRetryTaskConfiguration(1);

        List<TestContext> runTasks = new ArrayList<>();
        handler.setHandler(context -> {
            runTasks.add(context);
            return true;
        });

        TestContext context1 = getTestContext();
        TestContext context2 = getTestContext();
        txService.runInNewTx(() -> {
            service.addTaskBatch(configuration, "1", context1);
            service.addTaskBatch(configuration, "2", context2);
        });

        sleep(100L);

        queue.reset();
        processor.processPendingTasksWithReset(queue);

        Assertions.assertTrue(handler.isSuccessful());
        Assertions.assertEquals(2, runTasks.size(), "Обе операции должны выполниться");
    }

    @Test
    public void batchCreateSameTask() {
        RetryTaskConfiguration configuration = getTestRetryTaskConfiguration(1);

        List<TestContext> runTasks = new ArrayList<>();
        handler.setHandler(context -> {
            runTasks.add(context);
            return true;
        });

        TestContext context1 = getTestContext();
        TestContext context2 = getTestContext();
        txService.runInNewTx(() -> {
            service.addTaskBatch(configuration, "1", context1);
            service.addTaskBatch(configuration, "1", context2);
        });

        sleep(100L);

        processor.processPendingTasksWithReset(queue);

        Assertions.assertTrue(handler.isSuccessful());
        Assertions.assertEquals(1, runTasks.size());
        Assertions.assertEquals(
                context2.getValue(), runTasks.get(0).getValue(), "Должна остаться только одна операция");
    }

    @Test
    public void batchDeleteThenCreateTask() {
        RetryTaskConfiguration configuration = getTestRetryTaskConfiguration(1);

        List<TestContext> runTasks = new ArrayList<>();
        handler.setHandler(context -> {
            runTasks.add(context);
            return true;
        });

        TestContext context1 = getTestContext();
        TestContext context2 = getTestContext();
        txService.runInNewTx(() -> {
            service.addTaskBatch(configuration, "1", context1);
            service.deleteTaskBatch("1");
            service.addTaskBatch(configuration, "1", context2);
        });

        sleep(100L);

        processor.processPendingTasksWithReset(queue);

        Assertions.assertTrue(handler.isSuccessful());
        // Должна остаться только последняя операция
        Assertions.assertEquals(1, runTasks.size());
        Assertions.assertEquals(context2.getValue(), runTasks.get(0).getValue());
    }

    @Test
    public void batchCreateThenDeleteTask() {
        RetryTaskConfiguration configuration = getTestRetryTaskConfiguration(1);

        List<TestContext> runTasks = new ArrayList<>();
        handler.setHandler(context -> {
            runTasks.add(context);
            return true;
        });

        TestContext context1 = getTestContext();
        TestContext context2 = getTestContext();
        txService.runInNewTx(() -> {
            service.addTaskBatch(configuration, "1", context1);
        });
        txService.runInNewTx(() -> {
            service.addTaskBatch(configuration, "1", context2);
            service.deleteTaskBatch("1");
        });

        sleep(100L);

        processor.processPendingTasksWithReset(queue);

        // Т.к. последняя операция delete, то ничего не должно попасть в очередь на выполнение
        Assertions.assertTrue(runTasks.isEmpty());
    }

    private void add(RetryTask task) {
        txService.runInNewTx(() -> dao.add(task));
    }

    @BeforeEach
    public void setUp() {
        txService.runInNewTx(() -> dao.deleteTasks());
        handler.reset();
        clearRequestId();

        reset(cyclicalHandler);
        when(cyclicalHandler.serialize(any())).thenReturn(Randoms.bytea());
        when(cyclicalHandler.deserialize(any())).thenReturn(getTestContext());
    }

    @Test
    public void rescheduleCyclicalTask() {
        // настройка системы
        OffsetDateTime nextExecutionTime = Now.offsetDateTime().withNano(0).plusMinutes(1);
        setUpCyclicalHandler(nextExecutionTime, true);
        createCyclicalTask(1);

        // вызов системы
        processor.processPendingTasksWithReset(queue);

        // проверка утверждений
        RetryTask task = utils.getSingleTask();
        Assertions.assertEquals(0, task.getAttemptNumber());
        Assertions.assertTrue(
                nextExecutionTime.isEqual(task.getNextAttemptTime()), String.format("expected: %s, actual: %s",
                        nextExecutionTime, task.getNextAttemptTime()));
    }

    @Test
    public void deleteCyclicalTaskOnFailed() {
        OffsetDateTime nextExecutionTime = Now.offsetDateTime().plusMinutes(1);
        setUpCyclicalHandler(nextExecutionTime, false);
        createCyclicalTask(2);

        processor.processPendingTasksWithReset(queue);
        RetryTask task = utils.getSingleTask();
        Assertions.assertEquals(1, task.getAttemptNumber());

        sleep(1500); // ждем чтобы у задачи наступило время следующей попытки ее выполнения
        processor.processPendingTasksWithReset(queue);
        Assertions.assertEquals(0, dao.getTasks().size());
    }

    @Test
    public void handleFailureOnNextExecutionTimeErrorForCyclicalTask() {
        // настройка системы
        when(cyclicalHandler.nextExecutionTime(any())).thenThrow(new RuntimeException());
        when(cyclicalHandler.invoke(any())).thenReturn(true);

        createCyclicalTask(1);
        processor.processPendingTasksWithReset(queue);

        verify(cyclicalHandler, times(1)).handleFailure(any(), any());

        Assertions.assertEquals(0, dao.getTasks().size());
    }

    private void setUpCyclicalHandler(OffsetDateTime nextExecutionTime, boolean result) {
        when(cyclicalHandler.nextExecutionTime(any())).thenReturn(nextExecutionTime);
        when(cyclicalHandler.invoke(any())).thenReturn(result);
    }

    private void clearRequestId() {
        RequestContextHolder.clearContext();
    }

    private String getRequestId() {
        return RequestContextHolder.getContext().getRequestId();
    }

    private void setRequestId(String requestId) {
        RequestContextHolder.createContext(Optional.of(requestId));
    }

    private TestContext createTask(int number) {
        return createTask(number, RetryTaskTestHandler.BEAN_NAME);
    }

    private TestContext createCyclicalTask(int number) {
        return createTask(number, RetryServiceTestConfiguration.CYCLICAL_HANDLER_BEAN_NAME);
    }

    private TestContext createTask(int number, String handler) {
        RetryTaskConfiguration configuration = getTestRetryTaskConfiguration(number, handler);
        TestContext context = getTestContext();

        service.addTask(configuration, context);

        sleep(100L);

        return context;
    }

    private TestContext createTaskWithoutRestoringRequestId(int number) {
        RetryTaskConfiguration configuration = getTestRetryTaskConfiguration(number);
        TestContext context = getTestContext();

        service.addTask(CreateTaskRequestBuilder.valueOf(configuration, context).setPreserveCallContext(false).build());

        sleep(100L);

        return context;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private TestContext getTestContext() {
        TestContext context = new TestContext();
        context.setValue(UUID.randomUUID().toString());
        return context;
    }

    private RetryTaskConfiguration getTestRetryTaskConfiguration(int maxAttemptNumber) {
        return getTestRetryTaskConfiguration(maxAttemptNumber, RetryTaskTestHandler.BEAN_NAME);
    }

    private RetryTaskConfiguration getTestRetryTaskConfiguration(int maxAttemptNumber,
                                                                 String handler) {
        RetryTaskConfiguration configuration = new RetryTaskConfiguration();
        configuration.setHandler(handler);
        configuration.setDelay(1);
        configuration.setInitialDelay(0);
        configuration.setMaxAttemptCount(maxAttemptNumber);
        return configuration;
    }

    private void assertTaskIsSuccessfullyCompleted() {
        Assertions.assertTrue(handler.isSuccessful());
        Assertions.assertFalse(handler.isFailure());
    }

    private void assertTaskIsFailed() {
        Assertions.assertFalse(handler.isSuccessful());
        Assertions.assertTrue(handler.isFailure());
    }

}
