package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.AbstractUpdateMissingItemsRequestProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil.assertOrderDiff;
import static ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil.assertOrderHistoryEventCount;

abstract class AbstractUpdateMissingItemsRequestTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2020-05-02T22:00:00Z");

    static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    @DisplayName(
        "Создание ORDER_CHANGED_BY_PARTNER ChangeRequest " +
            "при успешной обработке ITEM_NOT_FOUND Change Request"
    )
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/updaterequeststatus/before/all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/item_not_found_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/controller/order/updateitems/before/item_not_found_payload.xml")
    @ExpectedDatabase(
        value =
            "/controller/order/updateitems/updaterequeststatus/after/all_success_order_changed_by_partner_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void allSuccessItemNotFound() {
        processor().processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=ITEM_NOT_FOUND/1/UPDATE_REQUEST/PROCESSING/SUCCESS\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=ITEM_NOT_FOUND,SUCCESS,PROCESSING,1,28800.0,93600.0,48"
            ));
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=CREATE_REQUEST\t" +
                    "payload=ORDER_CHANGED_BY_PARTNER/2/CREATE_REQUEST/CREATED\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,requestId,partnerId,status\t" +
                    "extra_values=ORDER_CHANGED_BY_PARTNER,2,48,CREATED"
            ));

    }

    @Test
    @DisplayName("Успешное обновление товаров заказа после ORDER_CHANGED_BY_PARTNER changeRequest")
    @DatabaseSetup({
        "/controller/order/updateitems/updaterequeststatus/before/update_order_items_successful.xml",
        "/controller/order/updateitems/updaterequeststatus/before/change_request.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/order_update_with.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsSuccessOrderChangedByPartnerChangeRequestFlow() {
        processor().processPayload(PAYLOAD);
        assertDiff(
            "controller/order/updateitems/updaterequeststatus/after/order_history_events/event.json"
        );
    }

    @Test
    @DisplayName(
        "Успешное обновление товаров заказа через ORDER_CHANGED_BY_PARTNER changeRequest с входящими instances"
    )
    @DatabaseSetup({
        "/controller/order/updateitems/updaterequeststatus/before/update_order_items_successful.xml",
        "/controller/order/updateitems/updaterequeststatus/before/create_request_with_instance.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/order_update_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsWithNewInstancesSuccessOrderChangedByPartnerChangeRequestFlow() {
        processor().processPayload(PAYLOAD);
        assertDiff(
            "controller/order/updateitems/updaterequeststatus/after/order_history_events/event_with_instance.json"
        );
    }

    @Test
    @DisplayName(
        "Успешное обновление товаров заказа через ORDER_CHANGED_BY_PARTNER changeRequest с текущими instances и " +
            "пустыми входящими соответствующими текущим"
    )
    @DatabaseSetup({
        "/controller/order/updateitems/updaterequeststatus/before/update_order_items_successful.xml",
        "/controller/order/updateitems/updaterequeststatus/before/change_request_with_2_instances_changed.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/order_update_with_2_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsWithSameOldInstancesSuccessOrderChangedByPartnerChangeRequestFlow() {
        processor().processPayload(PAYLOAD);
        assertDiff(
            "controller/order/updateitems/updaterequeststatus/after/order_history_events/event_2_instances_changed.json"
        );
    }

    private void assertDiff(String filePath) {
        assertOrderDiff(jdbcTemplate, 1, filePath, JSONCompareMode.LENIENT);
        assertOrderHistoryEventCount(jdbcTemplate, 1, 1);
    }

    @Nonnull
    abstract AbstractUpdateMissingItemsRequestProcessor processor();
}
