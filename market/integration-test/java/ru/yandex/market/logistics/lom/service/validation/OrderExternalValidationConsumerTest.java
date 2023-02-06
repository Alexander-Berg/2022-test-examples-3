package ru.yandex.market.logistics.lom.service.validation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.OrderExternalValidationConsumer;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@DisplayName("Тест на потребителя задачи VALIDATE_ORDER_EXTERNAL")
public class OrderExternalValidationConsumerTest extends AbstractContextualTest {
    private static final OrderIdPayload PAYLOAD = PayloadFactory.createOrderIdPayload(ORDER_ID, "1001");
    @Autowired
    private MarketIdService marketIdService;
    @Autowired
    private OrderExternalValidationConsumer consumer;

    @Test
    @DisplayName("Сервис MarketId недоступен, результат задачи FAIL")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    void marketIdServiceUnavailableTaskMustBeRetried() {
        mockMarketIdService();
        Task<OrderIdPayload> task = TaskFactory.createTask(QueueType.VALIDATE_ORDER_EXTERNAL, PAYLOAD, 1);
        TaskExecutionResult executionResult = consumer.execute(task);
        softly.assertThat(executionResult.getActionType()).isEqualTo(TaskExecutionResult.Type.FAIL);
    }

    @Test
    @DisplayName(
        "Сервис MarketId не доступен 3 раза. " +
            "Результат задачи FINISH. " +
            "Задача доступна для ручного перевыставления в админке."
    )
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    @DatabaseSetup("/service/externalvalidation/before/market_id_service_unavailable.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/market_id_service_unavailable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void marketIdServiceUnavailableMaxAttemptsNumberTimesTaskMustBeRetried() {
        mockMarketIdService();
        Task<OrderIdPayload> task = TaskFactory.createTask(QueueType.VALIDATE_ORDER_EXTERNAL, PAYLOAD);
        TaskExecutionResult executionResult = consumer.execute(task);
        softly.assertThat(executionResult.getActionType()).isEqualTo(TaskExecutionResult.Type.FINISH);
    }

    private void mockMarketIdService() {
        doThrow(new RuntimeException("MarketId service unavailable, task should be retried"))
            .when(marketIdService).findAccountById(anyLong());
    }
}
