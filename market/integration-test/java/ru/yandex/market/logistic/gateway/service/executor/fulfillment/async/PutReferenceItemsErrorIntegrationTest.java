package ru.yandex.market.logistic.gateway.service.executor.fulfillment.async;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.PutReferenceItemsErrorExecutor.DEFAULT_ERROR_MESSAGE;

public class PutReferenceItemsErrorIntegrationTest extends AbstractIntegrationTest {

    private final static long PUT_REFERENCE_ITEMS_ERROR_TASK_ID = 70L;

    private final static long PUT_REFERENCE_ITEMS_ERROR_PARENT_TASK_ID = 60L;

    @MockBean
    private ClientTaskRepository repository;

    @SpyBean
    private IrisFulfillmentConsumerClient irisFulfillmentConsumerClient;

    @Autowired
    private PutReferenceItemsErrorExecutor putReferenceItemsErrorExecutor;

    @Before
    public void init() {
        doNothing().when(irisFulfillmentConsumerClient).setPutReferenceItemsResult(any(), any());
    }

    @Test
    public void executeError() {
        ClientTask task = getPutReferenceItemsErrorTask();
        ClientTask parentTask =
            getPutReferenceItemsErrorParentTask("fixtures/executors/put_reference_items/put_reference_items_task_message.json");

        when(repository.findTask(eq(PUT_REFERENCE_ITEMS_ERROR_TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PUT_REFERENCE_ITEMS_ERROR_PARENT_TASK_ID))).thenReturn(parentTask);

        TaskMessage message = putReferenceItemsErrorExecutor
            .execute(new ExecutorTaskWrapper(PUT_REFERENCE_ITEMS_ERROR_TASK_ID, 0));
        softAssert.assertThat(message.getMessageBody()).isNull();

        verify(irisFulfillmentConsumerClient).setPutReferenceItemsResult(emptyList(), getErrorItems());
    }

    private ClientTask getPutReferenceItemsErrorTask() {
        ClientTask task = new ClientTask();
        task.setId(PUT_REFERENCE_ITEMS_ERROR_TASK_ID);
        task.setRootId(PUT_REFERENCE_ITEMS_ERROR_PARENT_TASK_ID);
        task.setParentId(PUT_REFERENCE_ITEMS_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.FF_PUT_REFERENCE_ITEMS_ERROR);
        task.setConsumer(TaskResultConsumer.IRIS);
        return task;
    }

    private ClientTask getPutReferenceItemsErrorParentTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(PUT_REFERENCE_ITEMS_ERROR_PARENT_TASK_ID);
        task.setRootId(PUT_REFERENCE_ITEMS_ERROR_PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.FF_PUT_REFERENCE_ITEMS);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.IRIS);
        return task;
    }

    private List<ErrorItem> getErrorItems() {
        UnitId firstUnitId = new UnitId("159346127", 465852L, "000139.би");
        UnitId secondUnitId = new UnitId("100561421890", 549309L, "4607086562215");
        ErrorPair errorPair = new ErrorPair(ErrorCode.UNKNOWN_ERROR, DEFAULT_ERROR_MESSAGE, null, null);
        ErrorItem firstErrorItem = new ErrorItem(firstUnitId, errorPair);
        ErrorItem secondErrorItem = new ErrorItem(secondUnitId, errorPair);
        return asList(firstErrorItem, secondErrorItem);
    }
}
