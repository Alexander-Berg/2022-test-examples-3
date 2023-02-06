package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentReturnArrivedService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

@DatabaseSetup("/service/process_segment_return_arrived/before/setup.xml")
class ProcessSegmentReturnArrivedServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessSegmentReturnArrivedService processSegmentReturnArrivedService;

    @Test
    @DisplayName("Процессинг возвратного статуса от ФФ")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFF() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(1, 1, "1", 1));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDS() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(2, 3, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(3L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД без заявки на отмену заказа")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/new_cancellation_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithoutCancellationOrderRequest() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(3, 4, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД без заявки на отмену доставленного заказа (50)")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithoutCancellationOrderRequestDelivered50() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(4, 5, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД без заявки на отмену доставленного заказа (49)")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithoutCancellationOrderRequestDelivered49() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(5, 6, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД после истечения срока хранения в Лавку")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/expired_lavka.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateExpiredLavkaOrder() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(8, 11, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД после истечения срока хранения в ПВЗ")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/expired_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateExpiredPickupOrder() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(6, 7, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от СД после истечения срока хранения в ПВЗ раньше времени")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/wrong_expired_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateExpiredPickupOrderWrong() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(7, 9, "2", 2));
    }

    @Test
    @DisplayName("Процессинг возвратного статуса от ДС после чекпоинтов 45 и 43")
    @ExpectedDatabase(
        value = "/service/process_segment_return_arrived/after/after43.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPickupExpiredAfter45and43checkpoints() {
        processSegmentReturnArrivedService.processPayload(createWaybillSegmentPayload(9, 13, "1", 1));
    }

}
