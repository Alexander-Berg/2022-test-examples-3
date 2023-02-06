package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.io.IOException;

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

public class GetAttachedDocsSuccessIntegrationTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 100L;
    private static final long PARTNER_ID = 48L;
    private static final String URL = "http://example.com/test.pdf";
    private static final String SHIPMENT_ID = "testYandexId";

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private LomFulfillmentConsumerClient lomClient;

    @Autowired
    private GetAttachedDocsSuccessExecutor getAttachedDocsSuccessExecutor;

    @Before
    public void setUp() {
        doNothing().when(lomClient).setGetAttachedDocsSuccess(anyString(), anyLong(), anyString(), any());
    }

    @Test
    public void executeSuccessfully() throws Exception {
        ClientTask task = getClientTask("fixtures/executors/fulfillment_get_attached_docs/get_attached_docs_success.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        TaskMessage message = getAttachedDocsSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lomClient).setGetAttachedDocsSuccess(SHIPMENT_ID, PARTNER_ID, URL, null);

        softAssert.assertThat(message.getMessageBody()).isNull();
    }

    private ClientTask getClientTask(String filename) throws IOException {
        return ClientTaskFactory.createClientTask(
            TASK_ID,
            RequestFlow.FF_GET_ATTACHED_DOCS_SUCCESS,
            filename
        ).setConsumer(TaskResultConsumer.LOM);
    }
}
