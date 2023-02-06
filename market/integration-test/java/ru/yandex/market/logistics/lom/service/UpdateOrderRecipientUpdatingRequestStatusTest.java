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
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderRecipientUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

class UpdateOrderRecipientUpdatingRequestStatusTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private UpdateOrderRecipientUpdatingRequestProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Сегмент в статусе PROCESSING")
    @DatabaseSetup("/controller/order/recipient/update_request_status/before/all_processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/recipient/update_request_status/before/all_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allProcessing() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Сегмент в статусе SUCCESS")
    @DatabaseSetup("/controller/order/recipient/update_request_status/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/recipient/update_request_status/before/all_success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recipient/update_request_status/after/all_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allSuccess() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=RECIPIENT/1/UPDATE_REQUEST/PROCESSING/SUCCESS\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=RECIPIENT,SUCCESS,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Сегмент в статусе FAIL")
    @DatabaseSetup("/controller/order/recipient/update_request_status/before/all_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/recipient/update_request_status/before/all_fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/recipient/update_request_status/after/all_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allFail() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=RECIPIENT/1/UPDATE_REQUEST/PROCESSING/FAIL\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=RECIPIENT,FAIL,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Обновление с ошибкой валидации в пэйлоаде")
    @DatabaseSetup(
        "/controller/order/recipient/update_request_status/before/update_successful_with_wrong_payload.xml"
    )
    @ExpectedDatabase(
        value = "/controller/order/recipient/update_request_status/before/update_successful_with_wrong_payload.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wrongPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD)).isInstanceOf(IllegalStateException.class)
            .hasMessage("Payload has validation errors");
    }
}
