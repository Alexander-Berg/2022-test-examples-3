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
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeLastMileToPickupUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

public class ChangeLastMileToPickupUpdatingRequestStatusTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private ChangeLastMileToPickupUpdatingRequestProcessor processor;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Ничего не происходит пока заявка в статусе PROCESSING")
    @DatabaseSetup("/controller/order/change_last_mile_to_pickup/update_request_status/before/processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/before/processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void nothingHappensWhileRequestIsProcessing() {
        processor.processPayload(PAYLOAD);
    }

    /**
     * Имеем заказ с сегментами FF-SC-MOVEMENT(PREPARING)-PICKUP(PREPARING)-COURIER.
     * При успешной заявке сохраняем/обновляем необходимые поля заказа, обновляем статусы активности сегментов.
     * В diff в событии только обновления полей заказа, без обновления статусов активности сегментов.
     * В snapshot в событии актуальный теперь путевой лист FF-SC-MOVEMENT-PICKUP.
     */
    @Test
    @DisplayName("Обновление заказа при всех посегментных заявках в статусе SUCCESS")
    @DatabaseSetup("/controller/order/change_last_mile_to_pickup/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void orderUpdateUponRequestSuccess() {
        processor.processPayload(PAYLOAD);
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/change_last_mile_to_pickup/update_request_status/after/diff.json",
            JSONCompareMode.LENIENT
        );
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            1L,
            "controller/order/change_last_mile_to_pickup/update_request_status/after/snapshot.json",
            "created",
            "updated",
            "changeOrderRequests[0].created",
            "changeOrderRequests[0].updated"
        );
    }

    /**
     * Имеем заказ с сегментами FF-SC-MOVEMENT(PREPARING)-PICKUP(PREPARING)-COURIER и заявку на обновление КИЗов на
     * подготавливаемых сегментах.
     * При успешной заявке заявки на обновление КИЗов возобновляются.
     */
    @Test
    @DisplayName("Возобновление обновления КИЗов при всех посегментных заявках в статусе SUCCESS")
    @DatabaseSetup("/controller/order/change_last_mile_to_pickup/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/before/update_items_instances.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/after/"
            + "success_with_update_items_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void continueUpdatingOrderItems() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Обновление заказа без кода получателя на PICKUP сегменте при Redelivery в Постомат")
    @DatabaseSetup("/controller/order/change_last_mile_to_pickup/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = {
            "/controller/order/change_last_mile_to_pickup/update_request_status/before/success.xml",
            "/controller/order/change_last_mile_to_pickup/update_request_status/before/market_locker.xml"

        },
        type = DatabaseOperation.UPDATE
    )
    void orderUpdateWithoutVerificationCode() {
        processor.processPayload(PAYLOAD);
        Order updatedOrder = orderService.getOrderOrThrow(1L);
        softly.assertThat(updatedOrder.getRecipientVerificationCode()).isNull();
    }

    @Test
    @DisplayName("Заявка переходит в статус FAIL при падении заявки на сегменте")
    @DatabaseSetup("/controller/order/change_last_mile_to_pickup/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/before/fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/change_last_mile_to_pickup/update_request_status/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestFailed() {
        processor.processPayload(PAYLOAD);
    }
}
