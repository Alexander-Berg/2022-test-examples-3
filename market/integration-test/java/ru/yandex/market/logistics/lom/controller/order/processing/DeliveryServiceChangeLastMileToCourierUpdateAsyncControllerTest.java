package ru.yandex.market.logistics.lom.controller.order.processing;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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

@DisplayName("Обработка заявок на изменение типа доставки на курьерскую на сегменте COURIER"
    + " после получения ответа на асинхронный запрос ds-update-order.")
@DatabaseSetup(value = {
    "/controller/order/processing/change_last_mile_to_courier/setup.xml",
    "/controller/order/processing/change_last_mile_to_courier/update/before/change_order_request.xml",
    "/controller/order/processing/change_last_mile_to_courier/update/before/segment_requests.xml"
})
public class DeliveryServiceChangeLastMileToCourierUpdateAsyncControllerTest extends AbstractContextualTest {
    private static final Long MK_PARTNER_ID = 49L;
    private static final String BARCODE = "1001";

    @Test
    @DisplayName("Успешный ответ и обработка cosr на сегменте COURIER")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_courier/update/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successResponse() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto(BARCODE, MK_PARTNER_ID, 10L))
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_LAST_MILE_TO_COURIER_REQUEST_STATUS_UPDATE,
            PayloadFactory.createChangeOrderSegmentRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_courier/update/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorResponse() throws Exception {
        performErrorRequest(new UpdateOrderErrorDto(BARCODE, MK_PARTNER_ID, 10L, null, null, false))
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskNotCreated(QueueType.PROCESS_WAYBILL_SEGMENT_CHANGE_LAST_MILE);
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
