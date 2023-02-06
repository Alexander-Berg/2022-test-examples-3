package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.lom.client.async.LomDeliveryServiceConsumerClient;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Обработка ошибок при изменении даты доставки")
class GetOrdersDeliveryDateErrorIntegrationLomTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 2L;
    private static final Long PARENT_TASK_ID = 1L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private LomDeliveryServiceConsumerClient lomClient;

    @Autowired
    private GetOrdersDeliveryDateErrorExecutor getOrdersDeliveryDateErrorExecutor;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient);
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {0}")
    @MethodSource
    @DisplayName("Не изменилась дата доставки, вызов клиента лома")
    void tasksWithError(
        String displayName,
        String taskMessageFile,
        boolean isTechError,
        String expectedMessage
    ) {
        ClientTask task = getTask(taskMessageFile);
        ClientTask parentTask = getParentTask();

        when(repository.findTask(eq(TASK_ID))).thenReturn(task);
        when(repository.findTask(eq(PARENT_TASK_ID))).thenReturn(parentTask);

        getOrdersDeliveryDateErrorExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(lomClient).setGetOrdersDeliveryDateError(
            eq(123L),
            anyList(),
            eq(145L),
            eq(isTechError),
            eq(expectedMessage)
        );
    }

    @Nonnull
    private static Stream<Arguments> tasksWithError() {
        return Stream.of(
            Arguments.of(
                "Техническая ошибка",
                "fixtures/executors/get_order_delivery_date/get_order_delivery_date_tech_error.json",
                true,
                "Error getting delivery date"
            ),
            Arguments.of(
                "Не техническая ошибка",
                "fixtures/executors/get_order_delivery_date/get_order_delivery_date_non_tech_error.json",
                false,
                "Не найдено событие"
            )
        );
    }

    @Nonnull
    private ClientTask getTask(String filename) {
        ClientTask task = new ClientTask();
        task.setId(TASK_ID);
        task.setParentId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_ORDERS_DELIVERY_DATE_ERROR);
        task.setMessage(getFileContent(filename));
        task.setConsumer(TaskResultConsumer.LOM);
        task.setProcessId("123");
        return task;
    }

    @Nonnull
    private ClientTask getParentTask() {
        ClientTask task = new ClientTask();
        task.setId(PARENT_TASK_ID);
        task.setRootId(PARENT_TASK_ID);
        task.setStatus(TaskStatus.ERROR);
        task.setFlow(RequestFlow.DS_GET_ORDERS_DELIVERY_DATE);
        task.setMessage(getFileContent("fixtures/executors/get_order/get_orders_delivery_date_task_message.json"));
        task.setConsumer(TaskResultConsumer.LOM);
        return task;
    }
}

