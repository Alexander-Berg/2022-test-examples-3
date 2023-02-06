package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ErrorCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ErrorItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ErrorPair;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.consumer.IrisFulfillmentConsumerClient;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class PutReferenceItemsSuccessIntegrationTest extends AbstractIntegrationTest {

    private final static long TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private IrisFulfillmentConsumerClient irisFulfillmentConsumerClient;

    @Autowired
    private PutReferenceItemsSuccessExecutor putReferenceItemsSuccessExecutor;

    @Before
    public void init() {
        doNothing().when(irisFulfillmentConsumerClient).setPutReferenceItemsResult(any(), any());
    }

    @Test
    public void executeSuccess() {
        ClientTask task =
            getPutReferenceItemsTask("fixtures/executors/put_reference_items/put_reference_items_task_response.json");
        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        TaskMessage message = putReferenceItemsSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        Mockito.verify(irisFulfillmentConsumerClient).setPutReferenceItemsResult(getCreatedItems(), getErrorItems());
    }

    private ClientTask getPutReferenceItemsTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_UPDATE_ORDER);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.IRIS);
        return task;
    }

    private List<UnitId> getCreatedItems() {
        UnitId unitId = new UnitId("159346127", 465852L, "000139.би");
        return singletonList(unitId);
    }

    private List<ErrorItem> getErrorItems() {
        UnitId unitId = new UnitId("100561421890", 549309L, "4607086562215");
        ErrorPair errorPair = new ErrorPair(ErrorCode.UNKNOWN_ERROR, "Не прошел валидацию", null, emptyList());
        ErrorItem errorItem = new ErrorItem(unitId, errorPair);
        return singletonList(errorItem);
    }
}
