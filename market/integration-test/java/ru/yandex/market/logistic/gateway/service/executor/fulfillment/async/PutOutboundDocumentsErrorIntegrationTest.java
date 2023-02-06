package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.TMFulfillmentConsumerClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PutOutboundDocumentsErrorIntegrationTest extends AbstractIntegrationTest {
    private static final long TASK_ID = 100L;
    private static final long PARENT_TASK_ID = 99L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private TMFulfillmentConsumerClient tmClient;

    @Autowired
    private PutOutboundDocumentsErrorExecutor putOutboundDocumentsErrorExecutor;

    @Test
    public void executeError() {
        ClientTask task =
            getClientTask("fixtures/executors/put_outbound_documents/put_outbound_documents_task_response_error.json");

        ClientTask parentTask =
            getParentClientTask("fixtures/executors/put_outbound_documents/put_outbound_documents_task_response.json");

        when(repository.findTask(TASK_ID)).thenReturn(task);
        when(repository.findTask(PARENT_TASK_ID)).thenReturn(parentTask);

        TaskMessage message = putOutboundDocumentsErrorExecutor.execute(
            new ExecutorTaskWrapper(TASK_ID, 0)
        );

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmClient).setPutOutboundDocumentsError(
            eq("123"),
            Mockito.isNull(),
            eq(145L),
            eq(TASK_ID),
            eq(TEST_PROCESS_ID_STRING),
            eq("Error")
        );
    }

    private ClientTask getClientTask(String filename) {
        return new ClientTask()
            .setId(TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setParentId(PARENT_TASK_ID)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(RequestFlow.FF_PUT_OUTBOUND_DOCUMENTS_ERROR)
            .setProcessId(TEST_PROCESS_ID_STRING)
            .setConsumer(TaskResultConsumer.TM)
            .setMessage(getFileContent(filename));
    }

    private ClientTask getParentClientTask(String filename) {
        return new ClientTask()
            .setId(PARENT_TASK_ID)
            .setRootId(PARENT_TASK_ID)
            .setStatus(TaskStatus.ERROR)
            .setFlow(RequestFlow.FF_PUT_OUTBOUND_DOCUMENTS)
            .setMessage(getFileContent(filename));
    }
}
