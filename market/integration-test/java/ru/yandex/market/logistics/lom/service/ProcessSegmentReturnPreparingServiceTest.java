package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentReturnPreparingService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

@DatabaseSetup("/service/process_segment_return_preparing/before/setup.xml")
class ProcessSegmentReturnPreparingServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessSegmentReturnPreparingService processSegmentReturnPreparingService;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
    }

    @AfterEach
    void cleanup() {
        featureProperties.setCancellationWithLrmAllEnabled(false);
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД: курьерка")
    @ExpectedDatabase(
        value = "/service/process_segment_return_preparing/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderCourier() {
        processSegmentReturnPreparingService.processPayload(createWaybillSegmentPayload(1, 2, "2", 2));

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.UPDATE_ORDER_CANCELLATION_REQUEST);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.SEND_CANCELLATION_TO_LRM,
            PayloadFactory.createOrderIdPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД: курьерка. Есть активный запрос на отмену")
    @DatabaseSetup(
        value = "/service/process_segment_return_preparing/before/cancellation_order_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/process_segment_return_preparing/after/cancellation_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderCourierOrderHasActiveCancellationRequest() {
        processSegmentReturnPreparingService.processPayload(createWaybillSegmentPayload(1, 2, "2", 2));

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.UPDATE_ORDER_CANCELLATION_REQUEST);
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД: ПВЗ")
    @ExpectedDatabase(
        value = "/service/process_segment_return_preparing/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderPickup() {
        processSegmentReturnPreparingService.processPayload(createWaybillSegmentPayload(1, 21, "2", 2));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД: неподходящий тип")
    @ExpectedDatabase(
        value = "/service/process_segment_return_preparing/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDSWrongType() {
        processSegmentReturnPreparingService.processPayload(createWaybillSegmentPayload(2, 4, "2", 2));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД: DAAS")
    @ExpectedDatabase(
        value = "/service/process_segment_return_preparing/after/daas.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDaas() {
        processSegmentReturnPreparingService.processPayload(createWaybillSegmentPayload(3, 5, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(3L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
    }
}
