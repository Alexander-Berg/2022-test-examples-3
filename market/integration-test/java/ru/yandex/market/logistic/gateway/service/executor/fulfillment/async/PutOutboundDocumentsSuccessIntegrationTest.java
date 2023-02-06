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

public class PutOutboundDocumentsSuccessIntegrationTest extends AbstractIntegrationTest {
    private static final long TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private TMFulfillmentConsumerClient tmClient;

    @Autowired
    private PutOutboundDocumentsSuccessExecutor putOutboundDocumentsSuccessExecutor;

    @Test
    public void executeSuccess() {
        ClientTask task =
            getClientTask("fixtures/executors/put_outbound_documents/put_outbound_documents_task_response.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        TaskMessage message = putOutboundDocumentsSuccessExecutor.execute(
            new ExecutorTaskWrapper(TASK_ID, 0)
        );

        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(tmClient).setPutOutboundDocumentsSuccess(
            eq("123"),
            Mockito.isNull(),
            eq(145L),
            eq(TEST_PROCESS_ID_STRING)
        );
    }

    private ClientTask getClientTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_OUTBOUND_DOCUMENTS_SUCCESS);
        task.setMessage(getFileContent(filename));
        task.setProcessId(TEST_PROCESS_ID_STRING);
        task.setConsumer(TaskResultConsumer.TM);
        return task;
    }
}
