package ru.yandex.market.logistics.lom.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отклонение заявки на изменение опции доставки заказа")
@DatabaseSetup("/controller/order/before/confirm_change_delivery_option_request.xml")
class DenyChangeOrderDeliveryOptionRequestControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Заявка не найдена")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void denyNotFound() throws Exception {
        denyChangeOrderRequest(0)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [0]"));
    }

    @Test
    @DisplayName("Заявка на изменение уже обработана")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void denyAlreadyProcessed() throws Exception {
        denyChangeOrderRequest(1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Change order request id = 1 of type = DELIVERY_OPTION is not active"));
    }

    @Test
    @DisplayName("Успешное отклонение заявки на изменение опций доставки заказа")
    @ExpectedDatabase(
        value = "/controller/order/after/deny_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void denySuccess() throws Exception {
        denyChangeOrderRequest(2)
            .andExpect(status().isOk())
            .andExpect(jsonPath("type").value("DELIVERY_OPTION_DENY"));
    }

    @Nonnull
    private ResultActions denyChangeOrderRequest(long changeRequestId) throws Exception {
        return mockMvc.perform(
            post("/orders/changeRequests/{changeRequestId}/process", changeRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/deny_change_delivery_option_request.json"))
        );
    }
}
