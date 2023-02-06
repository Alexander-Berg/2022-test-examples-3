package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отклонение заявки на изменение даты доставки заказа")
@DatabaseSetup("/controller/order/before/deny_update_delivery_date_request.xml")
class DenyUpdateDeliveryDateRequestControllerTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2020-11-03T00:00:00.00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Заявка не найдена")
    @ExpectedDatabase(
        value = "/controller/order/before/deny_update_delivery_date_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestNotFound() throws Exception {
        sendRequest(0)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_CHANGE_REQUEST] with id [0]"));
    }

    @Test
    @DisplayName("Тип заявки не совпадает с обработчиком")
    @ExpectedDatabase(
        value = "/controller/order/before/deny_update_delivery_date_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestTypeMismatch() throws Exception {
        sendRequest(2)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unable to process change order request id = 2 as type = DELIVERY_DATE"));
    }

    @Test
    @DisplayName("Заявка на изменение уже обработана")
    @ExpectedDatabase(
        value = "/controller/order/before/deny_update_delivery_date_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestAlreadyProcessed() throws Exception {
        sendRequest(1)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Change order request id = 1 of type = DELIVERY_DATE is in wrong state to reject"
            ));
    }

    @Test
    @DisplayName("Успешное отклонение заявки")
    @ExpectedDatabase(
        value = "/controller/order/after/deny_update_delivery_date_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmSuccess() throws Exception {
        sendRequest(3)
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions sendRequest(long changeRequestId) throws Exception {
        return mockMvc.perform(
            post("/orders/changeRequests/{changeRequestId}/process", changeRequestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/order/request/deny_update_delivery_date_request.json"
                ))
        );
    }
}
