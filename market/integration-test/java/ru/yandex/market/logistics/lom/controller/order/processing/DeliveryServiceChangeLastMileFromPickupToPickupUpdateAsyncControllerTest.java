package ru.yandex.market.logistics.lom.controller.order.processing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработка cor CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP после получения ds-update-order коллбэка")
@DatabaseSetup(value = {
    "/controller/order/processing/change_last_mile_from_pickup_to_pickup/setup.xml",
    "/controller/order/processing/change_last_mile_from_pickup_to_pickup/update/before/segment_requests.xml"
})
@ParametersAreNonnullByDefault
public class DeliveryServiceChangeLastMileFromPickupToPickupUpdateAsyncControllerTest extends AbstractContextualTest {

    private static final String BARCODE = "1001";
    private static final Long MK_PARTNER_ID = 49L;

    @Test
    @DisplayName("Успешный ответ и обработка cosr на сегменте MOVEMENT")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_from_pickup_to_pickup/update/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successResponse() {
        performSuccessRequest(new UpdateOrderSuccessDto(BARCODE, MK_PARTNER_ID, 10L))
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP_REQUEST_STATUS_UPDATE,
            PayloadFactory.createChangeOrderSegmentRequestPayload(3, "1", 1)
        );
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_from_pickup_to_pickup/update/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void errorResponse() {
        performErrorRequest(new UpdateOrderErrorDto(BARCODE, MK_PARTNER_ID, 10L, null, null, false))
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE
        );
    }

    @Nonnull
    private ResultActions performSuccessRequest(UpdateOrderSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/updateSuccess", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(UpdateOrderErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/updateError", request));
    }
}
