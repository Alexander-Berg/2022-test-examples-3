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
import ru.yandex.market.logistics.lom.model.async.CancelOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CancelOrderSuccessDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Обработка заявок на изменение типа доставки на курьерскую на сегменте PICKUP"
    + " после получения ответа на асинхронный запрос ds-cancel-order.")
@DatabaseSetup(value = {
    "/controller/order/processing/change_last_mile_to_courier/setup.xml",
    "/controller/order/processing/change_last_mile_to_courier/cancel/before/change_order_request.xml",
    "/controller/order/processing/change_last_mile_to_courier/cancel/before/segment_requests.xml"
})
public class DeliveryServiceChangeLastMileToCourierCancelAsyncControllerTest extends AbstractContextualTest {

    private static final Long PICKUP_PARTNER_ID = 50L;
    private static final String BARCODE = "1001";

    @Test
    @DisplayName("Успешный ответ и обработка cosr на сегменте PICKUP")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_courier/cancel/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successResponse() throws Exception {
        performSuccessRequest(new CancelOrderSuccessDto(BARCODE, PICKUP_PARTNER_ID, 10L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_courier/cancel/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorResponse() throws Exception {
        performErrorRequest(new CancelOrderErrorDto(BARCODE, PICKUP_PARTNER_ID, null, true, null, 10L))
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions performSuccessRequest(CancelOrderSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/ds/cancel/success", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(CancelOrderErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/ds/cancel/error", request));
    }
}
