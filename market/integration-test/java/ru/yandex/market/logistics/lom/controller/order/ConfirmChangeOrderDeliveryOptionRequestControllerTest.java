package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Подтверждение заявки на изменение опции доставки заказа")
@DatabaseSetup("/controller/order/before/confirm_change_delivery_option_request.xml")
class ConfirmChangeOrderDeliveryOptionRequestControllerTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2020-11-03T00:00:00.00Z");

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L, 2L)));
    }

    @Test
    @DisplayName("Заявка не найдена")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestNotFound() throws Exception {
        confirmChangeOrderRequest(0)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [0]"));
    }

    @Test
    @DisplayName("Тип заявки не совпадает с обработчиком")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestTypeMismatch() throws Exception {
        confirmChangeOrderRequest(3)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unable to process change order request id = 3 as type = DELIVERY_OPTION"));
    }

    @Test
    @DisplayName("Заявка на изменение уже обработана")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestAlreadyProcessed() throws Exception {
        confirmChangeOrderRequest(1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Change order request id = 1 of type = DELIVERY_OPTION is not active"));
    }

    @Test
    @DisplayName("Успешное подтверждение заявки на изменение опций доставки заказа")
    @ExpectedDatabase(
        value = "/controller/order/after/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmSuccess() throws Exception {
        confirmChangeOrderRequest(2)
            .andExpect(status().isOk())
            .andExpect(jsonPath("order.id").value(4));
    }

    @Nonnull
    private ResultActions confirmChangeOrderRequest(long changeRequestId) throws Exception {
        return mockMvc.perform(
            post("/orders/changeRequests/{changeRequestId}/process", changeRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/confirm_change_delivery_option_request.json"))
        );
    }
}
