package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.LomFulfillmentConsumerClient;
import ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetAttachedDocsErrorIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;
    private static final long PARENT_TASK_ID = 99L;
    private static final long PARTNER_ID = 48L;
    private static final String SHIPMENT_ID = "testYandexId";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private LomFulfillmentConsumerClient lomClient;

    @Autowired
    private GetAttachedDocsErrorExecutor getAttachedDocsErrorExecutor;

    @Before
    public void setUp() {
        doNothing().when(lomClient).setGetAttachedDocsError(anyString(), anyLong(), any(), any());
    }

    @Test
    public void getAttachedDocs() {
        ClientTask parentTask = ClientTaskFactory.createClientTask(
            PARENT_TASK_ID,
            RequestFlow.FF_GET_ATTACHED_DOCS,
            "fixtures/executors/fulfillment_get_attached_docs/get_attached_docs_error.json"
        );

        ClientTask task = ClientTaskFactory.createClientTask(
            TASK_ID,
            PARENT_TASK_ID,
            RequestFlow.FF_GET_ATTACHED_DOCS_SUCCESS
        )
            .setConsumer(TaskResultConsumer.LOM);

        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        TaskMessage message = getAttachedDocsErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lomClient).setGetAttachedDocsError(SHIPMENT_ID, PARTNER_ID, null, null);

        softAssert.assertThat(message.getMessageBody()).isNull();
    }
}
