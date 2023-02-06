package ru.yandex.market.logistic.gateway.service.executor;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.request.RequestState;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ExecutorException;
import ru.yandex.market.logistic.gateway.exceptions.NotRetryableException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CancelOrderExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderDeliveryDateExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateOrderItemsExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.UpdateRecipientExecutor;
import ru.yandex.market.logistic.gateway.service.flow.FlowService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link ExecutorService}.
 */
public class ExecutorServiceTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 1L;
    private static final Long PARENT_TASK_ID = 456L;
    private static final String REQUEST_ID = "123";
    private static final RequestFlow REQUEST_FLOW = RequestFlow.DS_CANCEL_ORDER;
    private static final String PROCESS_ID = "processIdABC123";

    private static final ExecutorTaskWrapper EXECUTOR_TASK_WRAPPER =
        new ExecutorTaskWrapper(TASK_ID, System.currentTimeMillis());
    private static final ExecutorTaskWrapper EXECUTOR_PARENT_TASK_WRAPPER =
        new ExecutorTaskWrapper(PARENT_TASK_ID, System.currentTimeMillis());

    private static final TaskMessage TASK_MESSAGE = new TaskMessage("{}");
    private static final String MESSAGE_ID = "123456";

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private ClientTaskRepository clientTaskRepository;

    @Autowired
    private CreateOrderExecutor createOrderExecutor;

    @MockBean
    private CancelOrderExecutor cancelOrderExecutor;

    @MockBean
    private UpdateRecipientExecutor updateRecipientExecutor;

    @MockBean
    private UpdateOrderDeliveryDateExecutor updateDeliveryDateExecutor;

    @MockBean
    private UpdateOrderItemsExecutor updateOrderItemsExecutor;

    @SpyBean
    private FlowService flowService;

    @Test
    @DatabaseSetup(
        value = "/repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/expected/client_task_after_not_retryable_exception.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void exceptionTest() {
        exceptionTest(true, new NotRetryableException("Error with request"));
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_after_no_notify.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void exceptionNoNotifyTest() {
        exceptionTest(false, new NotRetryableException("Error with request"));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/expected/cancel_order_task_after_request_state.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipRequestStateCancelOrderNotification() {
        exceptionTest(true, new RequestStateErrorException("Error with request", RequestState.failure(List.of(
            new ErrorPair(ErrorCode.BAD_REQUEST, "")
        ))));
    }

    private void exceptionTest(boolean shouldNotify, Exception error) {
        clientTaskRepository.save(buildClientTask(REQUEST_FLOW));

        when(cancelOrderExecutor.execute(EXECUTOR_TASK_WRAPPER)).thenThrow(error);
        when(cancelOrderExecutor.shouldNotifyOnError(error)).thenReturn(true);
        when(flowService.shouldNotify(REQUEST_FLOW)).thenReturn(shouldNotify);
        executorService.processExecutor(REQUEST_FLOW, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/mail_after_4000_code_exception.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void exceptionTestUpdateRecipientExecutorNotify() {
        ClientTask clientTask = buildClientTask(RequestFlow.DS_UPDATE_RECIPIENT);

        clientTask.setMessage(getFileContent("fixtures/executors/update_order/update_recipient_task_message_lom.json"));
        clientTaskRepository.save(clientTask);
        RequestState requestState = new RequestState();
        requestState.setErrorCodes(Collections.singletonList(new ErrorPair(ErrorCode.ENTITY_NOT_FOUND, "error")));

        when(updateRecipientExecutor.execute(EXECUTOR_TASK_WRAPPER))
            .thenThrow(new RequestStateErrorException("error", requestState));
        when(updateRecipientExecutor.shouldNotifyOnError(any())).thenCallRealMethod();
        when(flowService.shouldNotify(RequestFlow.DS_UPDATE_RECIPIENT)).thenReturn(true);
        executorService.processExecutor(RequestFlow.DS_UPDATE_RECIPIENT, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void exceptionTestUpdateRecipientExecutorNoNotify() {
        ClientTask clientTask = buildClientTask(RequestFlow.DS_UPDATE_RECIPIENT);

        clientTask.setMessage(getFileContent("fixtures/executors/update_order/update_recipient_task_message_lom.json"));
        clientTaskRepository.save(clientTask);
        RequestState requestState = new RequestState();
        requestState
            .setErrorCodes(Collections.singletonList(new ErrorPair(ErrorCode.OPERATION_CANNOT_BE_PROCESSED, "error")));

        when(updateRecipientExecutor.execute(EXECUTOR_TASK_WRAPPER))
            .thenThrow(new RequestStateErrorException("error", requestState));
        when(updateRecipientExecutor.shouldNotifyOnError(any())).thenCallRealMethod();
        executorService.processExecutor(RequestFlow.DS_UPDATE_RECIPIENT, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_after_request_state_exception.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void requestStateExceptionTest() {
        clientTaskRepository.save(buildClientTask(REQUEST_FLOW));

        String message = "Error with request";
        RequestState requestState = new RequestState();
        requestState.setIsError(true);
        requestState.setErrorCodes(Collections.singletonList(new ErrorPair(ErrorCode.BAD_REQUEST, message)));
        RequestStateErrorException requestStateErrorException = new RequestStateErrorException(message, requestState);

        when(cancelOrderExecutor.execute(EXECUTOR_TASK_WRAPPER)).thenThrow(requestStateErrorException);
        executorService.processExecutor(REQUEST_FLOW, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_after_retry_exception.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void retryExceptionTest() {
        ClientTask entity = buildClientTask(RequestFlow.DS_UPDATE_ORDER_DELIVERY_DATE);
        entity.setCountRetry(10);
        clientTaskRepository.save(entity);

        String message = "Error with request";
        RequestState requestState = new RequestState();
        requestState.setIsError(true);
        RequestStateErrorException requestStateErrorException = new RequestStateErrorException(message, requestState);

        when(updateDeliveryDateExecutor.execute(EXECUTOR_TASK_WRAPPER)).thenThrow(requestStateErrorException);
        when(updateDeliveryDateExecutor.shouldNotifyOnError(any())).thenCallRealMethod();
        executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_DELIVERY_DATE, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    @Test(expected = ExecutorException.class)
    @DatabaseSetup(value = "classpath:repository/state/client_task_with_unreadable_message.xml",
        connection = "dbUnitDatabaseConnection")
    public void testExecutionWithUnreadableTaskFailed() {
        createOrderExecutor.execute(EXECUTOR_TASK_WRAPPER);
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/client_task_cancel_order_new.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_cancel_order_success.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testExecutionWithCreatingSuccessFlowTask() {
        when(cancelOrderExecutor.execute(eq(EXECUTOR_PARENT_TASK_WRAPPER))).thenReturn(TASK_MESSAGE);

        executorService.processExecutor(RequestFlow.DS_CANCEL_ORDER, EXECUTOR_PARENT_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService).processFlow(
            eq(RequestFlow.DS_CANCEL_ORDER_SUCCESS),
            isNull(),
            eq(TASK_MESSAGE),
            eq(PARENT_TASK_ID),
            isNull()
        );
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/client_task_cancel_order_new.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_cancel_order_error.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testExecutionWithCreatingErrorFlowTask() {
        when(cancelOrderExecutor.execute(eq(EXECUTOR_PARENT_TASK_WRAPPER))).thenThrow(NotRetryableException.class);

        executorService.processExecutor(RequestFlow.DS_CANCEL_ORDER, EXECUTOR_PARENT_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService).processFlow(
            eq(RequestFlow.DS_CANCEL_ORDER_ERROR),
            isNull(),
            eq(new TaskMessage(
                "{\"exceptionClass\":\"ru.yandex.market.logistic.gateway.exceptions.NotRetryableException\"}"
            )),
            eq(PARENT_TASK_ID),
            isNull()
        );
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/client_task_cancel_order_new_with_process_id.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_cancel_order_success_with_process_id.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testExecutionWithCreatingSuccessFlowTaskWithProcessId() {
        when(cancelOrderExecutor.execute(eq(EXECUTOR_PARENT_TASK_WRAPPER))).thenReturn(TASK_MESSAGE);

        executorService.processExecutor(RequestFlow.DS_CANCEL_ORDER, EXECUTOR_PARENT_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService).processFlow(
            eq(RequestFlow.DS_CANCEL_ORDER_SUCCESS),
            isNull(),
            eq(TASK_MESSAGE),
            eq(PARENT_TASK_ID),
            eq(PROCESS_ID)
        );
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/client_task_cancel_order_new_with_process_id.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_cancel_order_error_with_process_id.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testExecutionWithCreatingErrorFlowTaskWithProcessId() {
        when(cancelOrderExecutor.execute(eq(EXECUTOR_PARENT_TASK_WRAPPER))).thenThrow(NotRetryableException.class);

        executorService.processExecutor(RequestFlow.DS_CANCEL_ORDER, EXECUTOR_PARENT_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService).processFlow(
            eq(RequestFlow.DS_CANCEL_ORDER_ERROR),
            isNull(),
            eq(new TaskMessage(
                "{\"exceptionClass\":\"ru.yandex.market.logistic.gateway.exceptions.NotRetryableException\"}"
            )),
            eq(PARENT_TASK_ID),
            eq(PROCESS_ID)
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_task_update_order_items.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/expected/client_task_update_order_items_success.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testExecutionUpdateOrderItemsSuccessFlowTask() {
        when(updateOrderItemsExecutor.execute(eq(EXECUTOR_PARENT_TASK_WRAPPER))).thenReturn(TASK_MESSAGE);

        executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_ITEMS, EXECUTOR_PARENT_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService).processFlow(
            eq(RequestFlow.DS_UPDATE_ORDER_ITEMS_SUCCESS),
            isNull(),
            eq(TASK_MESSAGE),
            eq(PARENT_TASK_ID),
            isNull()
        );
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void exceptionTestUpdateOrderItemsExecutorNoNotify() {
        ClientTask clientTask = buildClientTask(RequestFlow.DS_UPDATE_ORDER_ITEMS);

        clientTask.setMessage(getFileContent("fixtures/executors/update_order/update_order_items_task_message.json"));
        clientTaskRepository.save(clientTask);
        RequestState requestState = new RequestState();
        requestState
            .setErrorCodes(Collections.singletonList(new ErrorPair(ErrorCode.OPERATION_CANNOT_BE_PROCESSED, "error")));

        when(updateRecipientExecutor.execute(EXECUTOR_TASK_WRAPPER))
            .thenThrow(new RequestStateErrorException("error", requestState));
        when(updateRecipientExecutor.shouldNotifyOnError(any())).thenCallRealMethod();
        executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_ITEMS, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/state/empty.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testTaskNotTerminalStatusOnException() {
        ClientTask clientTask = buildClientTask(RequestFlow.DS_UPDATE_ORDER_ITEMS);
        clientTaskRepository.save(clientTask);

        doThrow(new RuntimeException()).when(flowService).processFlow(any(), any(), any(), any(), any());
        when(updateOrderItemsExecutor.execute(EXECUTOR_TASK_WRAPPER)).thenReturn(new TaskMessage(""));

        assertThrows(RuntimeException.class, () ->
            executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_ITEMS, EXECUTOR_TASK_WRAPPER, MESSAGE_ID));

        assertEquals(clientTaskRepository.findTask(TASK_ID).getStatus(), TaskStatus.IN_PROGRESS);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_task_with_child.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/state/client_task_with_child.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testTaskWithChildWasNotProcessed() {
        executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_ITEMS, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService, never()).updateTaskStatus(anyLong(), any(), any());
        verify(updateOrderItemsExecutor, never()).execute(any());
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_task_ready.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/state/client_task_ready.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testTaskWithReadyStatusWasNotProcessed() {
        executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_ITEMS, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);

        verify(flowService, never()).updateTaskStatus(anyLong(), any(), any());
        verify(updateOrderItemsExecutor, never()).execute(any());
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/client_task_error.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "/repository/expected/client_task_error_with_child.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testTaskWithErrorStatusWillCreateErrorTask() {
        executorService.processExecutor(RequestFlow.DS_UPDATE_ORDER_ITEMS, EXECUTOR_TASK_WRAPPER, MESSAGE_ID);
    }

    private ClientTask buildClientTask(RequestFlow requestFlow) {
        ClientTask clientTask = new ClientTask();
        clientTask.setId(TASK_ID);
        clientTask.setRootId(TASK_ID);
        clientTask.setRequestId(REQUEST_ID);
        clientTask.setFlow(requestFlow);
        clientTask.setStatus(TaskStatus.NEW);
        clientTask.setMessage("{}");
        return clientTask;
    }
}
