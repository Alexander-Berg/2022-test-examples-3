package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderDeliveryDateErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderDeliveryDateSuccessDto;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/order/deliverydate/async/before/setup.xml")
class DeliveryServiceUpdateDeliveryDateAsyncControllerTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-03-02T10:00:00Z");

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успешный ответ на запрос из PROCESS_WAYBILL_SEGMENT_UPDATE_DELIVERY_DATE процесса")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/async/after/success_update_delivery_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successResponseForProcessWaybillSegmentUpdateDeliveryDateBusinessProcess() throws Exception {
        UpdateOrderDeliveryDateSuccessDto request = new UpdateOrderDeliveryDateSuccessDto(10L, "1001", 1L);
        performSuccessRequest(request).andExpect(status().isOk());
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.UPDATE_ORDER_DELIVERY_DATE_REQUEST_STATUS_UPDATE);
    }

    @Test
    @DisplayName("Успешный ответ на запрос из PROCESS_WAYBILL_SEGMENT_RECALCULATED_ORDER_DATES процесса")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/async/after/success_recalculated_order_dates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successResponseForProcessWaybillSegment() throws Exception {
        UpdateOrderDeliveryDateSuccessDto request = new UpdateOrderDeliveryDateSuccessDto(12L, "1001", 2L);
        performSuccessRequest(request).andExpect(status().isOk());
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.UPDATE_RECALCULATED_ORDER_DATES_REQUEST_STATUS);
    }

    @Test
    @DisplayName("Неуспешный ответ")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/async/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failResponse() throws Exception {
        UpdateOrderDeliveryDateErrorDto request = new UpdateOrderDeliveryDateErrorDto(
            10L,
            "1001",
            1L,
            "fail",
            1000,
            false
        );
        performErrorRequest(request)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Невалидный успешный ответ: некорректный sequenseId")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/async/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidSuccessResponseNotFoundSequenseId() throws Exception {
        performSuccessRequest(new UpdateOrderDeliveryDateSuccessDto(11L, "1001", 1L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [11]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Невалидный успешный ответ")
    @MethodSource("invalidSuccessResponse")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/async/after/invalid_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidSuccessResponse(
        String name,
        UpdateOrderDeliveryDateSuccessDto request,
        ResultMatcher result
    ) throws Exception {
        performSuccessRequest(request)
            .andExpect(status().is4xxClientError())
            .andExpect(result);
    }

    @Nonnull
    private static Stream<Arguments> invalidSuccessResponse() {
        return Stream.of(
            Arguments.of(
                "некорректный barcode",
                new UpdateOrderDeliveryDateSuccessDto(10L, "1002", 1L),
                errorMessage("Incorrect barcode 1002 for segment change request 1")
            ),
            Arguments.of(
                "некорректный updateRequestId",
                new UpdateOrderDeliveryDateSuccessDto(10L, "1001", 11L),
                errorMessage("Failed to find [ORDER_CHANGE_SEGMENT_REQUEST] with id [11]")
            )
        );
    }

    @Nonnull
    private ResultActions performSuccessRequest(UpdateOrderDeliveryDateSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/ds/updateDeliveryDateSuccess", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(UpdateOrderDeliveryDateErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/ds/updateDeliveryDateError", request));
    }
}
