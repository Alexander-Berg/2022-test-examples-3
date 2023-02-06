package ru.yandex.market.logistic.gateway.service.executor;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.request.RequestState;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.exceptions.NotRetryableException;
import ru.yandex.market.logistic.gateway.exceptions.PartnerMethodNotFoundException;
import ru.yandex.market.logistic.gateway.exceptions.ResponseFormatException;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.sync.CreateOrderRequestExecutor;
import ru.yandex.market.logistic.gateway.service.flow.FlowService;
import ru.yandex.market.logistic.gateway.utils.CommonDtoFactory;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcessExecutorServiceTest extends AbstractIntegrationTest {

    @MockBean
    private FlowService flowService;

    @MockBean
    private ClientTaskRepository clientTaskRepository;

    @MockBean
    private CreateOrderRequestExecutor createOrderRequestExecutor;

    @SpyBean
    private CreateOrderExecutor createOrderExecutor;

    @Autowired
    private ProcessExecutorService processExecutorService;

    @SpyBean(name = "readWriteTransactionTemplate")
    private TransactionTemplate readWriteTransactionTemplate;

    @SpyBean(name = "readOnlyTransactionTemplate")
    private TransactionTemplate readOnlyTransactionTemplate;

    @Test
    public void testProcessExecutorWillRetryAfterRuntimeException() {
        executeProcessWithRetryCheck(new RuntimeException());
    }

    @Test
    public void testProcessExecutorWillRetryAfterResonseFormatException() {
        executeProcessWithRetryCheck(new ResponseFormatException("Incorrect response", new Partner(1L)));
    }

    @Test
    public void testProcessExecutorWillRetryAfterServiceInteractionResponseFormatException() {
        executeProcessWithRetryCheck(new ServiceInteractionResponseFormatException("Incorrect response"));
    }

    @Test
    public void testProcessExecutorWillRetryAfterRequestStateErrorException() {
        executeProcessWithRetryCheck(new RequestStateErrorException("Incorrect response"));
    }

    @Test
    public void testProcessExecutorWillRetryAfterServiceUnavailableErrorCode() {
        RequestState requestState = getRequestState(ErrorCode.SERVICE_UNAVAILABLE);
        executeProcessWithRetryCheck(new RequestStateErrorException("Incorrect response", requestState));
    }

    @Test
    public void testProcessExecutorWillRetryAfterUnknownErrorCode() {
        RequestState requestState = getRequestState(ErrorCode.UNKNOWN_ERROR);
        executeProcessWithRetryCheck(new RequestStateErrorException("Incorrect response", requestState));
    }

    @Test
    public void testProcessExecutorWillRetryAfterUnknownError() {
        RequestState requestState = new RequestState().setErrorCodes(Collections.emptyList());
        executeProcessWithRetryCheck(new RequestStateErrorException("Incorrect response", requestState));
    }

    @Test(expected =  RequestStateErrorException.class)
    public void testProcessExecutorRequestStateErrorExceptionWillThrow() {
        RequestState requestState = getRequestState(ErrorCode.FAILED_TO_PARSE_XML);
        doThrow(new RequestStateErrorException("Incorrect response", requestState))
            .when(createOrderExecutor).execute(any(ExecutorTaskWrapper.class));

        executeProcess();
    }

    @Test(expected = NotRetryableException.class)
    public void testProcessExecutorNotRetryableExceptionWillThrow() {
        doThrow(new NotRetryableException(""))
            .when(createOrderExecutor).execute(any(ExecutorTaskWrapper.class));

        executeProcess();
    }

    @Test(expected = PartnerMethodNotFoundException.class)
    public void testProcessExecutorWillNotRetryAfterPartnerMethodNotFound() {
        doThrow(new PartnerMethodNotFoundException(""))
            .when(createOrderExecutor).execute(any(ExecutorTaskWrapper.class));

        executeProcess();
    }

    @Test
    public void testSplitTransaction() throws JsonProcessingException {
        int taskId = 1;
        ClientTask clientTask = ClientTaskFactory.createClientTask(taskId, RequestFlow.DS_CREATE_ORDER);
        String message = jsonMapper.writeValueAsString(new CreateOrderRequest(DeliveryDtoFactory.createOrder(),
            CommonDtoFactory.createPartner(), null));
        clientTask.setMessage(message);

        when(clientTaskRepository.findTask(taskId)).thenReturn(clientTask);
        CreateOrderResponse response = new CreateOrderResponse(ResourceId.builder().setPartnerId("123").build(), null);
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return response;
        }).when(createOrderRequestExecutor).tryExecute(any(), anySet());
        processExecutorService.processExecutor(createOrderExecutor, new ExecutorTaskWrapper(taskId, 0), m -> {});
        verify(readOnlyTransactionTemplate).execute(any());
        verify(readWriteTransactionTemplate).execute(any());
    }

    private void executeProcess() {
        processExecutorService.processExecutor(createOrderExecutor, new ExecutorTaskWrapper(), taskMessage -> {});
    }

    private void executeProcessWithRetryCheck(Throwable throwable) {
        doThrow(throwable).when(createOrderExecutor).execute(any(ExecutorTaskWrapper.class));

        executeProcess();

        verify(flowService).retryFlow(anyLong(), eq(throwable));
    }

    private RequestState getRequestState(ErrorCode code) {
        return new RequestState()
            .setErrorCodes(Collections.singletonList(
                new ErrorPair(code, "exception message"))
            );
    }
}
