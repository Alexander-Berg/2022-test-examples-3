package ru.yandex.market.logistics.lom.jobs.executor;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.Mockito.mock;

@DisplayName("Тесты джобы создания возвратных реестров")
class CreateReturnRegistryExecutorTest extends AbstractContextualTest {

    @Autowired
    private CreateReturnRegistryExecutor createReturnRegistryExecutor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Test
    @DisplayName("Создание таски для заказа без возвратного реестра")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/createReturnRegister/after/new_return_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceTaskForOrderWithoutReturnRegistry() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
            PayloadFactory.createReturnRegistryIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Не создаём таску для заказа с успешно созданным возвратным реестром")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/return_registry_in_success_status.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_has_return_register.xml",
        type = DatabaseOperation.REFRESH
    )
    void produceNoTaskForOrderWithSuccessReturnRegistry() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Не создаём таску для заказа, у которого в истории нет возвратного статуса")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/no_return_status.xml")
    void produceNoTaskForOrderWhichHasNoReturnStatus() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Создание таски для заказа с возвратного реестра в ошибочном статусе")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/return_registry_in_error_status.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_has_return_register.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/createReturnRegister/after/new_return_registry_id_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceTaskForOrderWithErrorReturnRegistry() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
            PayloadFactory.createReturnRegistryIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Создаём единственную задачу для заказов с одинаковыми senderId и returnSortingCenterId")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_2_in_return_status.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/jobs/executor/createReturnRegister/after/new_return_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceSingleTaskForOrdersWithEqualSenderIdsAndReturnSortingCenterIds() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
            PayloadFactory.createReturnRegistryIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @JpaQueriesCount(42)
    @DisplayName("Создаём разные задачи для заказов с разными senderId")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_2_in_return_status.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/update_sender_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/createReturnRegister/after/new_return_registries_for_different_senders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceDifferentTasksForOrdersWithDifferentSenderIds() {
        createReturnRegistryExecutor.doJob(jobContext);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
            PayloadFactory.createReturnRegistryIdPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
            PayloadFactory.createReturnRegistryIdPayload(2L, "2", 2L)
        );
    }

    @Test
    @DisplayName(
        "Создаём таску для заказа, который не в возвратном статусе, " +
            "но у которого в истории сегментов есть возвратный статус"
    )
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/no_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/waybill_segment_status_history_with_return_status.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/jobs/executor/createReturnRegister/after/new_return_registry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void produceSingleTaskForOrderInNotReturnStatusButHasReturnStatusInHistory() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_RETURN_REGISTRY,
            PayloadFactory.createReturnRegistryIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Не создаём таску для заказа, у которого возвратный склад не в вайт-листе")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_return_sorting_center_not_in_whitelist.xml",
        type = DatabaseOperation.REFRESH
    )
    void produceNoTaskForOrderWithReturnSortingCenterBlackListed() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Не создаём таску для заказа без возвратного сегмента любого типа")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_without_return_segment.xml",
        type = DatabaseOperation.DELETE
    )
    void produceNoTaskForOrderWithoutReturnSegment() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Не создаём таску для заказа без возвратного СЦ-сегмента")
    @DatabaseSetup("/jobs/executor/createReturnRegister/before/order_in_return_status.xml")
    @DatabaseSetup(
        value = "/jobs/executor/createReturnRegister/before/order_without_return_sc_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    void produceNoTaskForOrderWithoutReturnScSegment() {
        createReturnRegistryExecutor.doJob(jobContext);
        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
