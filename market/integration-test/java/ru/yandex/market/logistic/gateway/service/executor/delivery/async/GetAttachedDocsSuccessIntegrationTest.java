package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.ShipmentType;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.MdbDeliveryConsumerClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;

public class GetAttachedDocsSuccessIntegrationTest extends AbstractIntegrationTest {
    private final static long TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private MdbDeliveryConsumerClient consumerClient;

    @Autowired
    private GetAttachedDocsSuccessExecutor getAttachedDocsSuccessExecutor;

    @Test
    public void executeSuccess() {
        ClientTask task = createClientTask(
            TASK_ID,
            RequestFlow.DS_GET_ATTACHED_DOCS_SUCCESS,
            "fixtures/executors/get_attached_docs/get_attached_docs_success.json"
        );
        task.setConsumer(TaskResultConsumer.MDB);
        when(repository.findTask(TASK_ID)).thenReturn(task);
        doNothing().when(consumerClient).setGetAttachedDocsSuccess(any(), any(), any(), anyLong(), any(), anyString());

        getAttachedDocsSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        verify(consumerClient, times(1)).setGetAttachedDocsSuccess(
            eq(ResourceId.builder().setYandexId("101").build()),
            eq(ShipmentType.ACCEPTANCE),
            eq(new DateTime("2018-08-22T00:00:00+03:00")),
            eq(48L),
            eq("http://example.com/ololo.pdf"),
            isNull()
        );
    }
}
