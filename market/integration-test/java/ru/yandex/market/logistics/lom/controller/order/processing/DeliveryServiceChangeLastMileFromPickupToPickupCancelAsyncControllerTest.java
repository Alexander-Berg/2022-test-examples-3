package ru.yandex.market.logistics.lom.controller.order.processing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.async.CancelOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CancelOrderSuccessDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Обработка cor CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP после получения ds-cancel-order коллбэка")
@DatabaseSetup(value = {
    "/controller/order/processing/change_last_mile_from_pickup_to_pickup/setup.xml",
    "/controller/order/processing/change_last_mile_from_pickup_to_pickup/cancel/before/segment_requests.xml"
})
@DatabaseSetup(
    value = "/controller/order/processing/change_last_mile_from_pickup_to_pickup/cancel/before/segment_statuses.xml",
    type = DatabaseOperation.REFRESH
)
@ParametersAreNonnullByDefault
public class DeliveryServiceChangeLastMileFromPickupToPickupCancelAsyncControllerTest extends AbstractContextualTest {

    private static final Long PICKUP_PARTNER_ID = 50L;
    private static final String BARCODE = "1001";

    @Test
    @DisplayName("Успешный ответ и обработка cosr на INACTIVE сегменте PICKUP")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_from_pickup_to_pickup/cancel/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successResponse() {
        performSuccessRequest(new CancelOrderSuccessDto(BARCODE, PICKUP_PARTNER_ID, 10L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_from_pickup_to_pickup/cancel/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void errorResponse() {
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
