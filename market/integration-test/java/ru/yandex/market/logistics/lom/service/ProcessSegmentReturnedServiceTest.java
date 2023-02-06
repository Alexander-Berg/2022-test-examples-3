package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentReturnedService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

@DatabaseSetup("/service/process_segment_returned/before/setup.xml")
class ProcessSegmentReturnedServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessSegmentReturnedService processSegmentReturnService;

    @Test
    @DisplayName("Процессинг возвратного статуса от ФФ")
    @ExpectedDatabase(
        value = "/service/process_segment_returned/after/ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFF() {
        processSegmentReturnService.processPayload(createWaybillSegmentPayload(1, 1, "1", 1));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД")
    @ExpectedDatabase(
        value = "/service/process_segment_returned/after/ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDS() {
        processSegmentReturnService.processPayload(createWaybillSegmentPayload(2, 2, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от возвратного склада")
    @ExpectedDatabase(
        value = "/service/process_segment_returned/after/return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderReturn() {
        processSegmentReturnService.processPayload(createWaybillSegmentPayload(2, 3, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД без заявки на отмену заказа")
    @ExpectedDatabase(
        value = "/service/process_segment_returned/after/new_cancellation_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithoutCancellationOrderRequest() {
        processSegmentReturnService.processPayload(createWaybillSegmentPayload(3, 4, "2", 2));
    }
}
