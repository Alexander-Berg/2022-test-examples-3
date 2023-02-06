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

@DisplayName("Подтверждение заявки на обработку изменения заказа партнером")
public class ConfirmOrderChangedByPartnerRequestControlledTest extends AbstractContextualTest {
    @Test
    @DisplayName("Заявка не найдена")
    @DatabaseSetup("/controller/order/before/confirm_change_order_by_partner_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_order_by_partner_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestNotFound() throws Exception {
        confirmChangeOrderRequest(3)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [3]"));
    }

    @Test
    @DisplayName("Тип заявки не совпадает с обработчиком")
    @DatabaseSetup("/controller/order/before/confirm_change_delivery_option_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestTypeMismatch() throws Exception {
        confirmChangeOrderRequest(3)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Unable to process change order request id = 3 as type = ITEM_NOT_FOUND"
            ));
    }

    @Test
    @DisplayName("Заявка на изменение уже обработана")
    @DatabaseSetup("/controller/order/before/confirm_change_order_by_partner_request_not_active.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_order_by_partner_request_not_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestAlreadyProcessed() throws Exception {
        confirmChangeOrderRequest(1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Unable to process change order request id = 1 as type = ORDER_CHANGED_BY_PARTNER"
            ));
    }

    @Test
    @DisplayName("Заявка на изменение невалидна")
    @DatabaseSetup("/controller/order/before/confirm_change_order_by_partner_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_order_by_partner_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestInvalid() throws Exception {
        mockMvc.perform(
            post("/orders/changeRequests/{changeRequestId}/process", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/order/request/confirm_change_order_by_partner_invalid_request.json"
                ))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "[FieldError(propertyPath=items[0].dimensions.length, message=must be greater than 0)]"
            ));
    }

    @Test
    @DisplayName("Успешное подтверждение заявки на обработку изменений заказа партнером")
    @DatabaseSetup("/controller/order/before/confirm_change_order_by_partner_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/confirm_change_order_by_partner_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmSuccess() throws Exception {
        confirmChangeOrderRequest(1)
            .andExpect(status().isOk())
            .andExpect(jsonPath("type").value("ORDER_CHANGED_BY_PARTNER_CONFIRM"));
    }

    @Nonnull
    private ResultActions confirmChangeOrderRequest(long changeRequestId) throws Exception {
        return mockMvc.perform(
            post("/orders/changeRequests/{changeRequestId}/process", changeRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/confirm_change_order_by_partner_request.json"))
        );
    }
}
