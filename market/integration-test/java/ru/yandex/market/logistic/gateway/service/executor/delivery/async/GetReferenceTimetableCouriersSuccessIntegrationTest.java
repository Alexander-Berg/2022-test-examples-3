package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location.LocationBuilder;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimetableCourier.TimetableCourierBuilder;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.management.client.async.LmsLgwCallbackClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetReferenceTimetableCouriersSuccessIntegrationTest extends AbstractIntegrationTest {
    private static final Long TASK_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LmsLgwCallbackClient lmsLgwCallbackClient;

    @Autowired
    private GetReferenceTimetableCouriersSuccessExecutor getReferenceTimetableCouriersSuccessExecutor;

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = getTask();

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);

        getReferenceTimetableCouriersSuccessExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lmsLgwCallbackClient).getReferenceTimetableCouriersSuccess(
            eq(145L),
            eq(
                List.of(
                    new TimetableCourierBuilder(
                        new LocationBuilder("Россия", "Новосибирск", "Новосибирская область").build(),
                        List.of(new WorkTime(1, List.of(new TimeInterval("14:30/15:30"))))
                    )
                        .setHolidays(List.of(new DateTime("2020-10-12T00:00:00+03:00")))
                        .build()
                )
            )
        );
    }

    private ClientTask getTask() {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setRootId(TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_REFERENCE_TIMETABLE_COURIERS);
        task.setMessage(getFileContent("fixtures/executors/get_reference_timetable_couriers/correct_message_response.json"));
        task.setConsumer(TaskResultConsumer.LMS);
        return task;
    }
}
