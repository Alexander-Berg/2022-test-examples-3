package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderRequestProcessingService;
import ru.yandex.market.logistics.lom.utils.WaybillUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

class UpdateOrderItemsProcessorDropshipTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderRequestPayload(1L, "1", 1L);

    @Autowired
    private ChangeOrderRequestProcessingService processor;

    @Test
    @DisplayName(
        "Успешное создание заявок на обновление товаров на одном сегменте на основе " +
            "ORDER_CHANGED_BY_PARTNER заявки"
    )
    @DatabaseSetup("/controller/order/update_order_items/dropship/before/single_ds_segment.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_items/dropship/after/single_segment_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void oneDeliverySegmentProcessingOrderChangedByPartner() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
            PayloadFactory.createChangeOrderSegmentRequestPayload(1L, "1", 1L)
        );
    }

    @Test
    @DisplayName("Успешное создание заявок на обновление товаров на двух сегмента")
    @DatabaseSetup("/controller/order/update_order_items/dropship/before/multiple_ds_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_items/dropship/after/2_3_segments_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleDeliverySegmentsProcessingOrderChangedByPartner() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
            PayloadFactory.createChangeOrderSegmentRequestPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
            PayloadFactory.createChangeOrderSegmentRequestPayload(2L, "2", 2L)
        );
    }

    @Test
    @DisplayName("Заявки создаются для СД и СЦ сегментов")
    @DatabaseSetup("/controller/order/update_order_items/dropship/before/ds_ff_sc_segments.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_order_items/dropship/after/2_3_segments_requests_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateItemsRequestCreatedForDeliveryAndSortingCenterSegments() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
            PayloadFactory.createChangeOrderSegmentRequestPayload(1L, "1", 1L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
            PayloadFactory.createChangeOrderSegmentRequestPayload(2L, "2", 2L)
        );
        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_ORDER_ITEMS,
            PayloadFactory.createChangeOrderSegmentRequestPayload(3L, "3", 3L)
        );
    }

    @Test
    @DisplayName("Запрос уже обработан")
    @DatabaseSetup("/controller/order/update_order_items/dropship/before/single_ds_segment.xml")
    @DatabaseSetup(
        value = "/controller/order/update_order_items/dropship/before/request_is_already_processing.xml",
        type = DatabaseOperation.UPDATE
    )
    void requestIsAlreadyProcessing() {
        processor.processPayload(PAYLOAD);

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заказ в неподходящем статусе")
    @DatabaseSetup("/controller/order/update_order_items/dropship/before/single_ds_segment.xml")
    @DatabaseSetup(
        value = "/controller/order/update_order_items/dropship/before/order_in_wrong_status.xml",
        type = DatabaseOperation.UPDATE
    )
    void orderInWrongStatus() {
        ProcessingResult processingResult = processor.processPayload(PAYLOAD);
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        softly.assertThat(processingResult.getComment())
            .isEqualTo("Try to change order 1 with status VALIDATION_ERROR");
    }

    @Test
    @DisplayName("У заказа нет FF - сегмента")
    @DatabaseSetup("/controller/order/update_order_items/dropship/before/no_ff_segment.xml")
    void orderWithoutFFSegment() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(WaybillUtils.SegmentNotFoundException.class)
            .hasMessage("Can't find fulfillment segment for order with id = 1");
    }
}
