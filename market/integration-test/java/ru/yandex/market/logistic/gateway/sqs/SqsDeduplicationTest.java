package ru.yandex.market.logistic.gateway.sqs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.UnexpectedRollbackException;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.ClientTaskWrapper;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.TaskNotFoundException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.repository.SqsMessageRepository;
import ru.yandex.market.logistic.gateway.service.client.ClientTaskConsumer;
import ru.yandex.market.logistic.gateway.service.client.ClientTaskProducer;
import ru.yandex.market.logistic.gateway.service.executor.ExecutorService;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CancelOrderExecutor;
import ru.yandex.market.logistic.gateway.service.flow.FlowService;
import ru.yandex.market.logistic.gateway.service.flow.TaskProcessService;
import ru.yandex.market.logistic.gateway.service.util.probe.SqsQueueActionProcessingTimeProbe;
import ru.yandex.market.logistic.gateway.service.util.probe.SqsQueueActionProcessingTimeProbeAspect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqsDeduplicationTest extends AbstractIntegrationTest {

    private static final RequestFlow FLOW = RequestFlow.DS_CREATE_INTAKE;
    private static final String MESSAGE = "message";
    private static final String MESSAGE_ID = "100";

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private ClientTaskConsumer clientTaskConsumer;

    @Autowired
    private ClientTaskRepository taskRepository;

    @Autowired
    private SqsMessageRepository sqsMessageRepository;

    @SpyBean
    private SqsQueueActionProcessingTimeProbeAspect aspect;

    @SpyBean
    private FlowService flowService;

    @SpyBean
    private TaskProcessService taskProcessService;

    @MockBean
    private ClientTaskProducer clientTaskProducer;

    @MockBean
    private CancelOrderExecutor cancelOrderExecutor;

    @Test
    @DisplayName("Rollback откатывает запись в sqs_message")
    public void rollbackSaveToSqsMessageWorksCorrect() throws Throwable {
        ClientTaskWrapper clientTaskWrapper = getClientTaskWrapper(FLOW, MESSAGE);
        TaskMessage expectedTaskMessage = new TaskMessage(MESSAGE);

        Exception expectedException = new RuntimeException("GET OVER HERE");

        doThrow(expectedException).when(flowService).startFlow(eq(FLOW), isNull(), eq(expectedTaskMessage), isNull());

        softAssert.assertThatThrownBy(() -> clientTaskConsumer.consumeClientTask(clientTaskWrapper, MESSAGE_ID))
            .isEqualTo(expectedException);

        softAssert.assertThat(sqsMessageRepository.findById(MESSAGE_ID)).isEmpty();
        softAssert.assertThatThrownBy(() -> taskRepository.findTask(1L))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessage("Task with id: 1 not found");

        TaskMessage anotherMessage = new TaskMessage("another");
        clientTaskConsumer.consumeClientTask(getClientTaskWrapper(FLOW, "another"), MESSAGE_ID);

        verify(aspect, times(2))
            .executeWithLogging(any(ProceedingJoinPoint.class), any(SqsQueueActionProcessingTimeProbe.class));
        verify(flowService).startFlow(eq(FLOW), isNull(), eq(anotherMessage), isNull());
        ClientTask savedTask = taskRepository.findTask(1L);
        softAssert.assertThat(savedTask.getConsumer()).isEqualTo(null);
    }

    @Test
    @DisplayName("Один из двух консьюмеров с одинаковыми messageId откатывает свою транзакцию при параллельной работе.")
    public void raceConditionResultsToRollbackOneOfConsumerTransactions() throws Throwable {
        ClientTaskWrapper clientTaskWrapper = getClientTaskWrapper(FLOW, MESSAGE);
        TaskMessage expectedTaskMessage = new TaskMessage(MESSAGE);

        ExceptionHolder exceptionHolder = new ExceptionHolder();
        runTwoRunnablesParallel(
            () -> clientTaskConsumer.consumeClientTask(clientTaskWrapper, MESSAGE_ID),
            () -> clientTaskConsumer.consumeClientTask(clientTaskWrapper, MESSAGE_ID),
            exceptionHolder
        );

        verify(aspect, times(2))
            .executeWithLogging(any(ProceedingJoinPoint.class), any(SqsQueueActionProcessingTimeProbe.class));
        verify(flowService, times(2))
            .startFlow(eq(FLOW), isNull(), eq(expectedTaskMessage), isNull());

        ClientTask savedTask = taskRepository.findTask(1L);
        softAssert.assertThat(savedTask.getConsumer()).isEqualTo(null);
        softAssert.assertThat(exceptionHolder.getException()).isInstanceOf(UnexpectedRollbackException.class);
        softAssert.assertThat(exceptionHolder.getCount()).isEqualTo(1);
    }

    private static final Long PARENT_TASK_ID = 456L;
    private static final ExecutorTaskWrapper EXECUTOR_PARENT_TASK_WRAPPER =
        new ExecutorTaskWrapper(PARENT_TASK_ID, System.currentTimeMillis());

    private static final TaskMessage TASK_MESSAGE = new TaskMessage("{}");

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_tasks_cancel_order_new.xml",
        connection = "dbUnitDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/repository/expected/client_tasks_cancel_order_one_is_successful.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Экзекьютор обрабатывает только одну таску с одинаковым messageId")
    public void testOnlyOneTaskWillProcessedWithEqualMessageId() {
        Instant fixedInstant = LocalDateTime.of(2021, 3, 23, 12, 0, 0)
            .toInstant(ZoneOffset.ofHours(3));

        ExecutorTaskWrapper firstTaskWrapper = new ExecutorTaskWrapper(456L, fixedInstant.toEpochMilli());
        ExecutorTaskWrapper secondTaskWrapper = new ExecutorTaskWrapper(457L, fixedInstant.toEpochMilli());

        TaskMessage expectedTaskMessage = new TaskMessage(MESSAGE);

        when(cancelOrderExecutor.execute(eq(firstTaskWrapper))).thenReturn(expectedTaskMessage);
        when(cancelOrderExecutor.execute(eq(secondTaskWrapper))).thenReturn(expectedTaskMessage);

        executorService.processExecutor(
            RequestFlow.DS_CANCEL_ORDER,
            firstTaskWrapper,
            MESSAGE_ID
        );

        executorService.processExecutor(
            RequestFlow.DS_CANCEL_ORDER,
            secondTaskWrapper,
            MESSAGE_ID
        );

        verify(flowService, times(2)).updateTaskStatus(
            anyLong(),
            eq(TaskStatus.IN_PROGRESS),
            eq("Started to process task")
        );

        verify(flowService).updateTaskStatus(
            eq(456L),
            eq(TaskStatus.READY),
            eq("Task processing finished")
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_tasks_cancel_order_new.xml",
        connection = "dbUnitDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/repository/expected/client_tasks_cancel_order_one_is_successful_another_is_error.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("messageId не сохраняется, если была ошибка во время процессинга.")
    public void testMessageIdDidntSaveIfProcessingWasInterrupted() {
        Instant fixedInstant = LocalDateTime.of(2021, 3, 23, 12, 0, 0)
            .toInstant(ZoneOffset.ofHours(3));

        ExecutorTaskWrapper firstTaskWrapper = new ExecutorTaskWrapper(456L, fixedInstant.toEpochMilli());
        ExecutorTaskWrapper secondTaskWrapper = new ExecutorTaskWrapper(457L, fixedInstant.toEpochMilli());

        TaskMessage expectedTaskMessage = new TaskMessage(MESSAGE);

        when(cancelOrderExecutor.execute(eq(firstTaskWrapper))).thenReturn(expectedTaskMessage);
        when(cancelOrderExecutor.execute(eq(secondTaskWrapper))).thenReturn(expectedTaskMessage);

        Exception exception = new RuntimeException("GET OVER HERE");

        doThrow(exception).when(taskProcessService).updateTaskStatus(
            eq(457L),
            eq(TaskStatus.IN_PROGRESS),
            eq("Started to process task")
        );

        executorService.processExecutor(
            RequestFlow.DS_CANCEL_ORDER,
            secondTaskWrapper,
            MESSAGE_ID
        );

        softAssert.assertThat(sqsMessageRepository.findById(MESSAGE_ID)).isEmpty();

        executorService.processExecutor(
            RequestFlow.DS_CANCEL_ORDER,
            firstTaskWrapper,
            MESSAGE_ID
        );

        verify(flowService).updateTaskStatus(
            eq(456L),
            eq(TaskStatus.READY),
            eq("Task processing finished")
        );

        final List<ClientTask> all = taskRepository.findAll();
        System.out.println();
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_tasks_cancel_order_new.xml",
        connection = "dbUnitDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/repository/expected/client_tasks_cancel_order_one_is_successful_another_is_error.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("messageId не сохраняется, если была ошибка во время процессинга после транзакции.")
    public void testMessageIdDidntSaveIfProcessingWasInterrupted2() {
        Instant fixedInstant = LocalDateTime.of(2021, 3, 23, 12, 0, 0)
            .toInstant(ZoneOffset.ofHours(3));

        ExecutorTaskWrapper firstTaskWrapper = new ExecutorTaskWrapper(456L, fixedInstant.toEpochMilli());
        ExecutorTaskWrapper secondTaskWrapper = new ExecutorTaskWrapper(457L, fixedInstant.toEpochMilli());

        TaskMessage expectedTaskMessage = new TaskMessage(MESSAGE);

        when(cancelOrderExecutor.execute(eq(firstTaskWrapper))).thenReturn(expectedTaskMessage);
        when(cancelOrderExecutor.execute(eq(secondTaskWrapper))).thenReturn(expectedTaskMessage);

        Exception exception = new RuntimeException("GET OVER HERE");

        doThrow(exception).when(taskProcessService).updateTaskStatus(
            eq(457L),
            eq(TaskStatus.IN_PROGRESS),
            eq("Started to process task")
        );

        executorService.processExecutor(
            RequestFlow.DS_CANCEL_ORDER,
            secondTaskWrapper,
            MESSAGE_ID
        );

        softAssert.assertThat(sqsMessageRepository.findById(MESSAGE_ID)).isEmpty();

        executorService.processExecutor(
            RequestFlow.DS_CANCEL_ORDER,
            firstTaskWrapper,
            MESSAGE_ID
        );

        verify(flowService).updateTaskStatus(
            eq(456L),
            eq(TaskStatus.READY),
            eq("Task processing finished")
        );

        final List<ClientTask> all = taskRepository.findAll();
        System.out.println();
    }

    @Nonnull
    private ClientTaskWrapper getClientTaskWrapper(RequestFlow flow, String message) {
        return new ClientTaskWrapper(100L, flow, message, null, null, null);
    }

    private void runTwoRunnablesParallel(
        Runnable firstRunnable,
        Runnable secondRunnable,
        ExceptionHolder exceptionHolder
    ) throws Throwable {
        CountDownLatch beforeExecution = new CountDownLatch(2);
        CountDownLatch afterExecution = new CountDownLatch(2);

        startRunnableInsideAThread(firstRunnable, exceptionHolder, beforeExecution, afterExecution);
        startRunnableInsideAThread(secondRunnable, exceptionHolder, beforeExecution, afterExecution);

        afterExecution.await();
    }

    private void startRunnableInsideAThread(
        Runnable runnable,
        ExceptionHolder exceptionHolder,
        CountDownLatch before,
        CountDownLatch after
    ) {
        new Thread(() -> {
            before.countDown();
            try {
                before.await();
                runnable.run();
            } catch (Exception e) {
                exceptionHolder.setException(e);
            }
            after.countDown();
        }).start();
    }

    @Getter
    private class ExceptionHolder {
        Exception exception;
        int count = 0;

        void setException(Exception exception) {
            this.exception = exception;
            count++;
        }
    }
}
