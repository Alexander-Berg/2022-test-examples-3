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
import ru.yandex.market.logistics.lom.jobs.processor.UpdateLastMileUpdatingRequestProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

public class UpdateLastMileUpdatingRequestStatusTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private UpdateLastMileUpdatingRequestProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-05-02T22:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Заявка в статусе PROCESSING")
    @DatabaseSetup("/controller/order/lastmile/update_request_status/before/processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/lastmile/update_request_status/before/processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestInProcessing() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Заявка в статусе SUCCESS")
    @DatabaseSetup("/controller/order/lastmile/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/lastmile/update_request_status/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/lastmile/update_request_status/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestInSuccess() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=LAST_MILE/1/UPDATE_REQUEST/PROCESSING/SUCCESS\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=LAST_MILE,SUCCESS,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Комментарий обновляется до пустого при наличии заявки в статусе SUCCESS")
    @DatabaseSetup("/controller/order/lastmile/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/lastmile/update_request_status/before/success.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/order/lastmile/update_request_status/before/payload_without_comment.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/lastmile/update_request_status/after/success_empty_comment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestInSuccessWithEmptyCommentInPayload() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Заявка в статусе FAIL")
    @DatabaseSetup("/controller/order/lastmile/update_request_status/before/processing.xml")
    @DatabaseSetup(
        value = "/controller/order/lastmile/update_request_status/before/fail.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/lastmile/update_request_status/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestInFail() {
        processor.processPayload(PAYLOAD);
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=UPDATE_REQUEST\t" +
                    "payload=LAST_MILE/1/UPDATE_REQUEST/PROCESSING/FAIL\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=UPDATE_ORDER_ITEMS_STATS\t" +
                    "extra_keys=requestType,newStatus,oldStatus,requestId,timeFromCreateToUpdate,timeFromUpdate," +
                    "partnerId\textra_values=LAST_MILE,FAIL,PROCESSING,1,28800.0,93600.0,null"
            ));
    }

    @Test
    @DisplayName("Обновление с невалидным пэйлоудом")
    @DatabaseSetup(
        "/controller/order/lastmile/update_request_status/before/invalid_payload.xml"
    )
    @ExpectedDatabase(
        value = "/controller/order/lastmile/update_request_status/before/invalid_payload.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD)).isInstanceOf(IllegalStateException.class)
            .hasMessage("Payload has validation errors");
    }
}
