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
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отклонение заявки на обработку изменения заказа партнером")
class DenyChangedByPartnerRequestControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Заявка не найдена")
    @DatabaseSetup("/controller/order/before/confirm_change_order_by_partner_request_not_active.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_order_by_partner_request_not_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void denyNotFound() throws Exception {
        denyChangeOrderRequest(10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [10]"));
    }

    @Test
    @DisplayName("Заявка на изменение уже обработана")
    @DatabaseSetup("/controller/order/before/confirm_change_order_by_partner_request_not_active.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_order_by_partner_request_not_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestAlreadyProcessed() throws Exception {
        denyChangeOrderRequest(1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Unable to process change order request id = 1 as type = ORDER_CHANGED_BY_PARTNER"
            ));
    }

    @Test
    @DisplayName("Успешное отклонение заявки на изменение опций доставки заказа")
    @DatabaseSetup("/controller/order/before/deny_change_order_by_partner_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/deny_changed_order_by_partner_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void denySuccess() throws Exception {
        denyChangeOrderRequest(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/deny_changed_order_by_partner.json",
                "cancellationOrderRequestDto.updated",
                "cancellationOrderRequestDto.created"
            ));
    }

    @Nonnull
    private ResultActions denyChangeOrderRequest(long changeRequestId) throws Exception {
        return mockMvc.perform(
            post("/orders/changeRequests/{changeRequestId}/process", changeRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/deny_changed_order_by_partner.json"))
        );
    }
}
