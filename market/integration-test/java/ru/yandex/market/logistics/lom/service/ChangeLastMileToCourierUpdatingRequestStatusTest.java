package ru.yandex.market.logistics.lom.service;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileToCourierUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Тесты обработки изменения статуса заявки на изменение последней мили на курьерскую")
public class ChangeLastMileToCourierUpdatingRequestStatusTest extends AbstractContextualTest {

    private static final ChangeOrderSegmentRequestPayload COURIER_PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);
    private static final ChangeOrderSegmentRequestPayload PICKUP_PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(2L, "1", 1L);

    @Autowired
    private ChangeLastMileToCourierUpdatingRequestProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Ничего не происходит пока заявка в статусе PROCESSING")
    @DatabaseSetup("/controller/order/change_last_mile_to_courier/update_request_status/before/processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void nothingHappensWhileRequestIsProcessing() {
        processor.processPayload(COURIER_PAYLOAD);
    }

    /**
     * Имеем заказ с сегментами FF-SC-MOVEMENT-PICKUP-COURIER(PREPARING).
     * При успешной заявке на сегменте COURIER сохраняем/обновляем необходимые поля заказа,
     * обновляем статусы активности сегментов.
     * Продолжаем обработку заявки на сегменте PICKUP для отмены заказа в ПВЗ.
     * В diff в событии только обновления полей заказа, без обновления статусов активности сегментов.
     * В snapshot в событии актуальный теперь путевой лист FF-SC-COURIER.
     */
    @Test
    @DisplayName("Обновление заказа при успешном выполнении заявки на сегменте COURIER")
    @DatabaseSetup("/controller/order/change_last_mile_to_courier/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderUpdateUponSuccessfulUpdateInTpl() {
        processor.processPayload(COURIER_PAYLOAD);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_LAST_MILE_TO_COURIER_REQUEST_STATUS_UPDATE,
            PayloadFactory.createChangeOrderSegmentRequestPayload(2, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE,
            PayloadFactory.createChangeOrderSegmentRequestPayload(2, "2", 2)
        );
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/change_last_mile_to_courier/update_request_status/after/diff.json",
            JSONCompareMode.LENIENT
        );
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/order/change_last_mile_to_courier/update_request_status/after/snapshot.json",
            "created",
            "updated",
            "changeOrderRequests[0].created",
            "changeOrderRequests[0].updated"
        );
    }

    /**
     * Имеем заказ с сегментами FF-SC-MOVEMENT-PICKUP-COURIER(PREPARING).
     * При успешной заявке на сегменте COURIER заявки на обновление КИЗов возобновляются.
     */
    @Test
    @DisplayName("Возобновление обновления КИЗов при успешном выполнении заявки на сегменте COURIER")
    @DatabaseSetup("/controller/order/change_last_mile_to_courier/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/update_items_instances.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/after/"
            + "success_with_update_items_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void continueUpdatingOrderItems() {
        processor.processPayload(COURIER_PAYLOAD);
    }

    @Test
    @DisplayName("Заявка переходит в статус REQUIRED_SEGMENT_FAIL при падении заявки на сегменте COURIER")
    @DatabaseSetup("/controller/order/change_last_mile_to_courier/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestFailed() {
        processor.processPayload(COURIER_PAYLOAD);
    }

    @Test
    @DisplayName("Падение заявки на сегменте PICKUP не влияет на итоговый успешный статус заявки")
    @DatabaseSetup("/controller/order/change_last_mile_to_courier/update_request_status/after/success.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/cancel_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/after/cancel_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestDoesNotFailUponCancellationFailure() {
        processor.processPayload(PICKUP_PAYLOAD);
    }

    @Test
    @DisplayName("Успешное выполнение заявки на сегменте PICKUP переводит статус заявки в SUCCESS")
    @DatabaseSetup("/controller/order/change_last_mile_to_courier/update_request_status/after/success.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/before/cancel_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_courier/update_request_status/after/cancel_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestSuccessAfterSuccessfullCancel() {
        processor.processPayload(PICKUP_PAYLOAD);
    }
}
