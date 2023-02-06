package ru.yandex.market.logistics.lom.controller.order;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Подтверждение заявки на изменение заказа")
@DatabaseSetup("/controller/order/before/confirm_change_delivery_option_request.xml")
class ChangeOrderRequestControllerTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestBodyArguments")
    @DisplayName("Валидация тела запроса")
    @ExpectedDatabase(
        value = "/controller/order/before/confirm_change_delivery_option_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validateRequestBody(String typeId, String requestBody) throws Exception {
        doActionWithChangeOrderRequest(requestBody)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(startsWith(String.format("Could not resolve type id '%s'", typeId))));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestBodyArguments() {
        return Stream.of(
            Arguments.of(null, "{\"type\": null}"),
            Arguments.of("INVALID_TYPE", "{\"type\": \"INVALID_TYPE\"}")
        );
    }

    @Nonnull
    private ResultActions doActionWithChangeOrderRequest(String requestBody) throws Exception {
        return mockMvc.perform(
            post("/orders/changeRequests/1/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        );
    }
}
