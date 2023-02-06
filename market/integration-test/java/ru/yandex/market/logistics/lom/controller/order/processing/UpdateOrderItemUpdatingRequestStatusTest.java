package ru.yandex.market.logistics.lom.controller.order.processing;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderUpdatingRequestProcessor;

class UpdateOrderItemUpdatingRequestStatusTest extends AbstractUpdateMissingItemsRequestTest {
    @Autowired
    private UpdateOrderUpdatingRequestProcessor processor;

    @Test
    @DisplayName("Все сегменты в статусе PROCESSING")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/all_processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/before/all_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allProcessing() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Все сегменты в статусе SUCCESS")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/updaterequeststatus/before/all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/all_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccess() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=ORDER_ITEM_IS_NOT_SUPPLIED/1/UPDATE_REQUEST/PROCESSING/SUCCESS\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=ORDER_ITEM_IS_NOT_SUPPLIED,SUCCESS,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Все сегменты в статусе FAIL")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/updaterequeststatus/before/all_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/all_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allFail() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=ORDER_ITEM_IS_NOT_SUPPLIED/1/UPDATE_REQUEST/PROCESSING/FAIL\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=ORDER_ITEM_IS_NOT_SUPPLIED,FAIL,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Один из сегментов в статусе FAIL")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/updaterequeststatus/before/single_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/single_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void singleFail() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=ORDER_ITEM_IS_NOT_SUPPLIED/1/UPDATE_REQUEST/PROCESSING/REQUIRED_SEGMENT_FAIL\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\t" +
                    "extra_values=ORDER_ITEM_IS_NOT_SUPPLIED,REQUIRED_SEGMENT_FAIL,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Попытка обновить успешную заявку")
    @DatabaseSetup("/controller/order/updateitems/updaterequeststatus/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/updaterequeststatus/before/all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/updateitems/updaterequeststatus/before/change_order_request_already_succeeded.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/after/change_order_request_already_succeeded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateSuccessRequest() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "payload=Change request of order 1 status SUCCESS not changed, " +
                    "after segment status SUCCESS, received.\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "entity_types=order,lom_order,changeOrderRequest,changeOrderSegmentRequest\t" +
                    "entity_values=order:1001,lom_order:1,changeOrderRequest:1,changeOrderSegmentRequest:1"
            ));
    }

    @Test
    @DisplayName("Обновление товаров с ошибкой валидации в пэйлоаде")
    @DatabaseSetup({
        "/controller/order/updateitems/updaterequeststatus/before/update_order_items_successful.xml",
        "/controller/order/updateitems/updaterequeststatus/before/change_request_wrong_payload.xml"
    })
    @ExpectedDatabase(
        value = "/controller/order/updateitems/updaterequeststatus/before/update_order_items_successful.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateItemsOfOrderWithValidationErrorInPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Payload has validation errors");
    }

    @Nonnull
    @Override
    UpdateOrderUpdatingRequestProcessor processor() {
        return processor;
    }
}
