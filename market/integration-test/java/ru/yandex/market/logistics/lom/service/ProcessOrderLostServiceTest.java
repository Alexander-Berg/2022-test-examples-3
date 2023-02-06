package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderLostService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

@DatabaseSetup("/service/process_order_lost/before/setup.xml")
class ProcessOrderLostServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessOrderLostService processOrderLostService;

    @Test
    @DisplayName("Процессинг статуса потери товара от СД")
    @ExpectedDatabase(
        value = "/service/process_order_lost/after/ds_lost.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDS() {
        processOrderLostService.processPayload(createWaybillSegmentPayload(1, 1, "1", 1));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг статуса потери товара от СД без заявки на отмену заказа")
    @ExpectedDatabase(
        value = "/service/process_order_lost/after/new_cancellation_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithoutCancellationOrderRequest() {
        processOrderLostService.processPayload(createWaybillSegmentPayload(2, 2, "1", 1));
    }
}
