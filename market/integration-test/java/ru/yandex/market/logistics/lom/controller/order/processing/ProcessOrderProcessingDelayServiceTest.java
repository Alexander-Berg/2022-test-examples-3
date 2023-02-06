package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ProcessOrderProcessingDelayPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderProcessingDelayService;
import ru.yandex.market.logistics.lom.utils.WaybillUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;

import static ru.yandex.market.logistics.lom.jobs.model.QueueType.EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED;

@DisplayName("Тесты на обработку факта исключения заказа из отгрузки")
@DatabaseSetup("/controller/order/proccessprocessingdelay/before/prepare.xml")
class ProcessOrderProcessingDelayServiceTest extends AbstractContextualTest {
    private static final ProcessOrderProcessingDelayPayload PAYLOAD =
        PayloadFactory.processOrderProcessingDelayPayload(
            1,
            1,
            2,
            "1",
            1
        );
    @Autowired
    protected OrderCombinedRouteHistoryYdbConverter routeHistoryConverter;

    @Autowired
    private ProcessOrderProcessingDelayService processor;

    @Test
    @DisplayName("Успешная обработка")
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/recalculation_change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processingSuccess() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE,
            PayloadFactory.createChangeOrderRequestPayload(1L, "1", 1)
        );
    }

    @Test
    @DisplayName("Не создаем заявку на изменение даты отгрузки, если такая уже есть")
    @DatabaseSetup("/controller/order/proccessprocessingdelay/before/existing_change_order_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/recalculation_change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotCreateChangeOrderRequestIfSuchAlreadyExists() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertNoQueueTasksCreated();

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "payload=Change request was already created before\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "entity_types=order,lom_order\t" +
                    "entity_values=order:1001,lom_order:1"
            ));
    }

    @Test
    @DisplayName("Успешная обработка, у заказа есть тег DELAYED_RDD_NOTIFICATION")
    @DatabaseSetup(
        value = "/controller/order/proccessprocessingdelay/before/add_order_tag.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/cor_created_without_user_notification.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processingSuccessOrderHasTag() {
        processor.processPayload(PAYLOAD);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_RECALCULATE_ORDER_DELIVERY_DATE,
            PayloadFactory.createChangeOrderRequestPayload(1L, "1", 1)
        );
    }

    @Test
    @DisplayName("Заказ доставлен")
    @DatabaseSetup(
        value = "/controller/order/proccessprocessingdelay/before/order_delivered.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/order_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderDelivered() {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Order 1 is not processing");

        queueTaskChecker.assertQueueTaskCreated(
            EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED,
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(1L, 1L, 2L, 1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Заказ в процессе отмены")
    @DatabaseSetup("/controller/order/proccessprocessingdelay/before/order_is_cancelling.xml")
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/order_is_cancelling.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderIsCancelling() {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Order 1 is already in process of cancellation");

        queueTaskChecker.assertQueueTaskCreated(
            EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED,
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(1L, 1L, 2L, 1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Заказ не найден")
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/recalculation_change_request_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noOrder() {
        softly.assertThatThrownBy(
            () -> processor.processPayload(PayloadFactory.processOrderProcessingDelayPayload(2, 1, 2, "1", 1))
        )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [2]");
    }

    @Test
    @DisplayName("У заказа нет указанного сегмента")
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/recalculation_change_request_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noSpecifiedSegment() {
        softly.assertThatThrownBy(() ->
            processor.processPayload(PayloadFactory.processOrderProcessingDelayPayload(1L, 2L, 2L, "1", 1))
        )
            .isInstanceOf(WaybillUtils.SegmentNotFoundException.class)
            .hasMessage("Waybill segment with id = 2 must exist. order: 1");
    }

    @Test
    @DisplayName("У DROPSHIP сегмента нет shipmentDateTime")
    @DatabaseSetup(
        value = "/controller/order/proccessprocessingdelay/before/no_shipment_date_time.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/recalculation_change_request_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noShipmentDateTime() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Order 1 has no processing deadline");
    }

    @Test
    @DisplayName("Обработка финальной ошибки - перевод заявки в FAIL")
    @ExpectedDatabase(
        value = "/controller/order/proccessprocessingdelay/after/recalculation_change_request_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processFinalFailure() {
        processor.processFinalFailure(PAYLOAD, new IllegalStateException("Order 1 has no processing deadline"));
        queueTaskChecker.assertQueueTaskCreated(
            EXPORT_ORDER_FROM_SHIPMENT_EXCLUSION_FINISHED,
            PayloadFactory.exportOrderFromShipmentExclusionFinishedPayload(1L, 1L, 2L, 1L, "1", 1L)
        );
    }
}
