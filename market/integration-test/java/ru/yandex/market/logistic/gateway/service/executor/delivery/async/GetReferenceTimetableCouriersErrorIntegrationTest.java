package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.management.client.async.LmsLgwCallbackClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetReferenceTimetableCouriersErrorIntegrationTest extends AbstractIntegrationTest {
    private static final Long TASK_ID = 1L;
    private static final Long PARENT_TASK_ID = 2L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LmsLgwCallbackClient lmsLgwCallbackClient;

    @Autowired
    private GetReferenceTimetableCouriersErrorExecutor getReferenceTimetableCouriersErrorExecutor;

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getTask();
        ClientTask parentTask = getParentTask();

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);

        getReferenceTimetableCouriersErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lmsLgwCallbackClient).getReferenceTimetableCouriersError(eq(145L), eq("Не найдено событие"), isNull());
    }

    private ClientTask getTask() {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_REFERENCE_TIMETABLE_COURIERS);
        task.setMessage(getFileContent("fixtures/executors/get_reference_timetable_couriers/error_task_message.json"));
        task.setConsumer(TaskResultConsumer.LMS);
        task.setParentId(PARENT_TASK_ID);
        return task;
    }

    private ClientTask getParentTask() {
        ClientTask task = new ClientTask();
        task.setId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.DS_GET_REFERENCE_TIMETABLE_COURIERS);
        task.setMessage(getFileContent("fixtures/executors/get_reference_timetable_couriers/correct_message.json"));
        task.setConsumer(TaskResultConsumer.LMS);
        return task;
    }
}
