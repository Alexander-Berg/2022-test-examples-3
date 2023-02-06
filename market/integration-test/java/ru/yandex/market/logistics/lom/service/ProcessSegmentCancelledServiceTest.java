package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.OrderCancellationProperties;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessSegmentCancelledService;

import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createSegmentCancellationRequestIdPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;

@DatabaseSetup("/service/process_segment_cancelled/before/setup.xml")
class ProcessSegmentCancelledServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessSegmentCancelledService processSegmentCancelledService;
    @Autowired
    private OrderCancellationProperties orderCancellationProperties;

    @BeforeEach
    void setup() {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(false);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Процессинг статуса отмены от ФФ")
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFF(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(1, 1, "1", 1));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(1L, "1", 1L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Процессинг статуса отмены от СД (отмена по 410)")
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDSWithFastCancellation(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(2, 2, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Процессинг статуса отмены от СД (отмена по 410), после 50чп нельзя отменять")
    @DatabaseSetup(value = "/service/process_segment_cancelled/before/50cp.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/without_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notCancelAfter50Checkpoint(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(2, 2, "2", 2));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Процессинг статуса отмены от СД (отмена по 410), после 49чп нельзя отменять")
    @DatabaseSetup(value = "/service/process_segment_cancelled/before/49cp.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/without_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notCancelAfter49Checkpoint() {
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(2, 2, "2", 2));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Процессинг статуса отмены от СД (отмена по 410), после 49чп можно отменять")
    @DatabaseSetup(value = "/service/process_segment_cancelled/before/49cp.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelAfter49Checkpoint() {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(true);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(2, 2, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(2L, "1", 1L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Процессинг статуса отмены от СД с заявкой на отмену заказа в статусе FAIL")
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/cancellation_order_request_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithCancellationOrderRequestFailed(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(3, 4, "2", 2));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_CANCELLATION_REQUEST,
            createSegmentCancellationRequestIdPayload(4L, "1", 1L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Процессинг статуса отмены от СД без заявки на отмену заказа (отмена по 410)")
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/new_cancellation_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderWithoutCancellationOrderRequestWithFastCancellation(
        boolean cancellationAvailableAfter49Checkpoint
    ) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(4, 5, "2", 2));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Отмена заказов с тегом ON_DEMAND по 410")
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/new_cancellation_on_demand_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void onDemandOrdersFastCancellation(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(5, 7, "2", 2));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Отмена заказов с тегом CALL_COURIER по 410")
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/new_cancellation_express_order_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void expressOrdersFastCancellation(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(6, 9, "2", 2));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(booleans = {true, false})
    @DisplayName("Отмена заказа от ФФ, новая заявка")
    @DatabaseSetup(
        value = "/service/process_segment_cancelled/before/cancelled_status_history.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/process_segment_cancelled/after/ff_new_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFFNewRequest(boolean cancellationAvailableAfter49Checkpoint) {
        orderCancellationProperties.setCancellationAvailableAfter49Checkpoint(cancellationAvailableAfter49Checkpoint);
        processSegmentCancelledService.processPayload(createWaybillSegmentPayload(6, 8, "1", 1));
    }
}
