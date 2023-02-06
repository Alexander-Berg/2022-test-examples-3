package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Пересчёт маршрута для заказа")
class RecalculateOrderRouteTest extends AbstractContextualTest {

    @Test
    @DisplayName("Заказ был в статусе VALIDATION_ERROR")
    @DatabaseSetup("/controller/admin/order/before/order-validation-error.xml")
    void validationError() throws Exception {
        recalculate()
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Заказ был в статусе VALIDATION_ERROR и есть сегмент пути")
    @DatabaseSetup("/controller/admin/order/before/order-validation-error-with-waybill.xml")
    void validationErrorWithWaybill() throws Exception {
        recalculate()
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(
                "Replace route is allowed only in state VALIDATION_ERROR and without created waybill segments"
            ));
    }

    @Test
    @DisplayName("Заказ был в статусе PROCESSING")
    @DatabaseSetup("/controller/admin/order/before/order-processing.xml")
    void processing() throws Exception {
        recalculate()
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(
                "Replace route is allowed only in state VALIDATION_ERROR and without created waybill segments"
            ));
    }

    @Nonnull
    private ResultActions recalculate() throws Exception {
        return mockMvc.perform(
            post("/admin/orders/recalculate-order-route")
                .headers(toHttpHeaders(USER_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}")
        );
    }
}
