package ru.yandex.market.logistic.gateway.service.flow;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.ExecutorBusinessException;
import ru.yandex.market.logistic.gateway.exceptions.NotRetryableException;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.utils.CommonDtoFactory;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;
import ru.yandex.market.logistic.gateway.utils.FulfillmentDtoFactory;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ExecutorFlowTest extends RequestFlowTest {

    private static final String EXCEPTION_MESSAGE = "failed with test exception";

    protected static final String REQUEST_ID = "requestId/1/2/3";
    private static final String SUB_REQUEST_ID = REQUEST_ID + "/1";

    /**
     * Успешный сценарий DS.createOrder.
     *
     * @throws GatewayApiException
     * @throws ExecutorBusinessException
     */
    @Test
    public void testCreateOrderSuccessSended() throws Exception {
        sendCreateOrder();

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
        verify(createOrderSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
    }

    /**
     * Сценарий с перевыставлением таски (retry по RuntimeException) и успешным повторным выполнением.
     * (для RuntimeException)
     *
     * @throws GatewayApiException
     * @throws ExecutorBusinessException
     * @throws ExecutionException
     * @throws IOException
     */
    @Test
    public void testCreateOrderSuccessAfterRetryRuntimeException() throws Exception {
        when(createOrderExecutor.execute(any()))
            .thenThrow(new RuntimeException("test exception"))
            .thenReturn(createOrderTaskMessage);

        sendCreateOrder();

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS + SQS_RETRY_TIMEOUT_MILLIS).times(2))
            .execute(any());
        verify(createOrderSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS + SQS_RETRY_TIMEOUT_MILLIS))
            .execute(any());
    }

    /**
     * Сценарий с неразрешимой ошибкой во flow.
     *
     * @throws GatewayApiException
     * @throws ExecutorBusinessException
     */
    @Test
    public void testCreateOrderFailed() throws Exception {

        when(createOrderExecutor.execute(any()))
            .thenThrow(new NotRetryableException(EXCEPTION_MESSAGE));

        sendCreateOrder();

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
        verify(createOrderErrorExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
        verify(createOrderSuccessExecutor, never()).execute(any());
    }

    /**
     * Сценарий с ошибкой при сохранении client_task.
     */
    @Test
    public void testCreateOrderSuccessAfterStartFlowException() throws Exception {

        doThrow(new RuntimeException("test exception"))
            .doCallRealMethod()
            .when(flowService).startFlow(any(), any(), any(), any());

        sendCreateOrder();

        verify(flowService, timeout(SQS_OPERATION_TIMEOUT_MILLIS + SQS_RETRY_TIMEOUT_MILLIS).times(2))
            .startFlow(any(), any(), any(), any());
        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS + SQS_RETRY_TIMEOUT_MILLIS))
            .execute(any());
        verify(createOrderSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS + SQS_RETRY_TIMEOUT_MILLIS))
            .execute(any());
    }

    /**
     * Сценарий недоступности базы при первой попытке вычитать и обработать задачу из клиентской очереди.
     * После этого таск возвращается в очередь, перечитывается и обрабатывается нормально.
     *
     * @throws GatewayApiException
     * @throws ExecutorBusinessException
     */
    @Test
    public void testCreateOrderPgaasFailedWithRetry() throws Exception {
        doThrow(new RuntimeException("test exception"))
            .doCallRealMethod().when(flowService).
            startFlow(
                eq(RequestFlow.DS_CREATE_ORDER),
                any(TaskResultConsumer.class),
                any(TaskMessage.class),
                any(String.class)
            );

        sendCreateOrder();

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
        verify(createOrderSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
    }

    @Test
    public void testCreateOrderSuccessSendedWillGenerateRequestIds() throws Exception {
        RequestContextHolder.createContext(Optional.of(REQUEST_ID));
        ArgumentCaptor<ExecutorTaskWrapper> executorTaskCaptor = ArgumentCaptor.forClass(ExecutorTaskWrapper.class);

        sendCreateInbound();
        verify(createInboundExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS))
            .execute(executorTaskCaptor.capture());
        verify(createInboundSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS))
            .execute(executorTaskCaptor.capture());

        List<ExecutorTaskWrapper> capturedExecutorTasks = executorTaskCaptor.getAllValues();

        assertions.assertThat(capturedExecutorTasks)
            .as("Asserting that capturedExecutorTasks list has size 2")
            .hasSize(2);
        assertions.assertThat(capturedExecutorTasks.get(0).getRequestId())
            .as("Asserting the first task requestId")
            .isEqualTo(SUB_REQUEST_ID);
        assertions.assertThat(capturedExecutorTasks.get(1).getRequestId())
            .as("Asserting the second task requestId")
            .isEqualTo(SUB_REQUEST_ID);

        assertions.assertThat(clientTaskRepository.findTask(capturedExecutorTasks.get(0).getId()).getLastSubReqNumber())
            .as("Asserting the first task last subReqNumber is not null")
            .isNotNull();
        assertions.assertThat(clientTaskRepository.findTask(capturedExecutorTasks.get(1).getId()).getLastSubReqNumber())
            .as("Asserting the second task last subReqNumber is not null")
            .isNotNull();
    }

    /**
     * Проверяет, что при отсутствии requestId в клиенте, он генерируется.
     *
     * @throws Exception
     */
    @Test
    public void testCreateOrderSuccessSendedWithoutRequestIdWillGenerateRequestId() throws Exception {
        RequestContextHolder.clearContext();
        sendCreateOrder();

        ArgumentCaptor<ExecutorTaskWrapper> executorTaskCaptor = ArgumentCaptor.forClass(ExecutorTaskWrapper.class);

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS))
            .execute(executorTaskCaptor.capture());

        List<ExecutorTaskWrapper> capturedExecutorTasks = executorTaskCaptor.getAllValues();

        assertions.assertThat(capturedExecutorTasks.get(0).getRequestId())
            .as("Asserting the first task requestId")
            .isNotNull();

        assertions.assertThat(clientTaskRepository.findTask(capturedExecutorTasks.get(0).getId()).getLastSubReqNumber())
            .as("Asserting the first task last subReqNumber is not null")
            .isNotNull();
    }

    @Test
    public void testManyConcurrencyExecutedTasks() throws Exception {
        int concurrency = 5;
        for (int i = 0; i < concurrency; i++) {
            sendCreateOrder();
        }

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS).times(concurrency)).execute(any());
        verify(createOrderSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS).times(concurrency)).execute(any());
    }

    @Test
    public void oneStuckTaskWillNotCompletelyBlockQueue() throws Exception {
        CountDownLatch countDownLatchBlock = new CountDownLatch(1);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(createOrderExecutor.execute(any()))
            .thenAnswer(invocation -> {
                countDownLatchBlock.await();
                Thread.sleep(1000);
                return createOrderTaskMessage;
            })
            .thenAnswer(invocation -> createOrderTaskMessage);

        when(createOrderSuccessExecutor.execute(any())).thenAnswer(invocation -> {
            countDownLatchBlock.countDown();
            return emptyTaskMessage;
        }).thenAnswer(invocation -> {
            countDownLatch.countDown();
            return emptyTaskMessage;
        });

        sendCreateOrder();
        sendCreateOrder();

        countDownLatchBlock.await(SQS_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        verify(createOrderExecutor, times(2)).execute(any());
        verify(createOrderSuccessExecutor, times(1)).execute(any());

        countDownLatch.await(SQS_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        verify(createOrderSuccessExecutor, times(2)).execute(any());
    }

    @Test
    public void oneStuckQueueWillNotCompletelyBlockAnotherQueue() throws Exception {
        CountDownLatch countDownLatchBlock = new CountDownLatch(1);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(createOrderExecutor.execute(any())).thenAnswer(invocation -> {
            countDownLatchBlock.await();
            Thread.sleep(1000);
            return createOrderTaskMessage;
        });

        when(createOrderSuccessExecutor.execute(any()))
            .thenAnswer(invocation -> emptyTaskMessage)
            .thenAnswer(invocation -> emptyTaskMessage)
            .thenAnswer(invocation -> {
                countDownLatch.countDown();
                return emptyTaskMessage;
            });

        when(createInboundSuccessExecutor.execute(any()))
            .thenAnswer(invocation -> {
                countDownLatchBlock.countDown();
                return emptyTaskMessage;
            });

        sendCreateOrder();
        sendCreateOrder();
        sendCreateOrder();
        sendCreateInbound();

        countDownLatchBlock.await(SQS_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        verify(createOrderExecutor, times(2)).execute(any());
        verify(createOrderSuccessExecutor, never()).execute(any());
        verify(createInboundExecutor, times(1)).execute(any());
        verify(createInboundSuccessExecutor, times(1)).execute(any());

        countDownLatch.await(SQS_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        verify(createOrderExecutor, times(3)).execute(any());
        verify(createOrderSuccessExecutor, times(3)).execute(any());
    }

    @Test
    public void doubleReceivedMessageWillNotExecutedTwice() throws Throwable {
        int defVisibilityTimeout = jmsAmazonSQSCustomClient.getVisibilityTimeout();
        jmsAmazonSQSCustomClient.setVisibilityTimeout(1);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mockito.doCallRealMethod()
            .doCallRealMethod()
            .doAnswer(invocation -> {
                countDownLatch.countDown();
                return invocation.callRealMethod();
            })
            .doCallRealMethod()
            .when(sqsDeduplicationAspect).checkDuplicationAndProceed(any(), any());

        when(createOrderExecutor.execute(any())).thenAnswer(invocation -> {
            countDownLatch.await();
            return createOrderTaskMessage;
        });

        sendCreateOrder();

        verify(createOrderExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
        verify(createOrderSuccessExecutor, timeout(SQS_OPERATION_TIMEOUT_MILLIS)).execute(any());
        verify(sqsDeduplicationAspect, timeout(SQS_OPERATION_TIMEOUT_MILLIS)
            .atLeast(4)).checkDuplicationAndProceed(any(), any());
        jmsAmazonSQSCustomClient.setVisibilityTimeout(defVisibilityTimeout);
    }

    @Test
    public void taskWillReprocessedOnError() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(createOrderExecutor.execute(any())).thenReturn(createOrderTaskMessage);
        when(createOrderSuccessExecutor.execute(any())).thenAnswer(invocation -> {
            countDownLatch.countDown();
            return new TaskMessage();
        });

        doCallRealMethod()
            .doThrow(new RuntimeException("test exception"))
            .doThrow(new RuntimeException("test exception"))
            .doCallRealMethod()
            .when(flowService).processFlow(any(), any(), any(), any(), any());

        sendCreateOrder();
        countDownLatch.await(SQS_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        verify(createOrderExecutor, times(2)).execute(any());
        verify(createOrderSuccessExecutor, times(1)).execute(any());
    }

    private void sendCreateOrder() throws GatewayApiException {
        Order order = DeliveryDtoFactory.createOrder(UUID.randomUUID().toString());
        Partner partner = CommonDtoFactory.createPartner();
        getDeliveryClient().createOrder(order, partner);
    }

    private void sendCreateInbound() throws GatewayApiException {
        Inbound inbound = FulfillmentDtoFactory.createInbound(UUID.randomUUID().toString());
        Partner partner = CommonDtoFactory.createPartner();
        getFulfillmentClient().createInbound(inbound, partner);
    }
}
