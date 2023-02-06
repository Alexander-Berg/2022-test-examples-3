package ru.yandex.market.logistics.nesu.controller.interceptor;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ArgumentResolverTest extends AbstractContextualTest {
    @Test
    @DisplayName("Отсутствует обязательный параметр userId")
    void missingRequiredParameter() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("shopId", "1")
                .param("senderId", "1")
                .param("substring", "лиса")
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "userId",
                "Failed to convert value of type 'null' to required type 'long'",
                "senderIdHolder",
                "typeMismatch")
            ));
    }

    @Test
    @DisplayName("Значение параметров невозможно распарсить")
    void parameterTypeMismatch() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("shopId", "first")
                .param("userId", "first")
                .param("senderId", "first")
                .param("substring", "лиса")
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                senderHolderArgumentError("senderId"),
                senderHolderArgumentError("shopId"),
                senderHolderArgumentError("userId")
            )));
    }

    @Nonnull
    private ValidationErrorData senderHolderArgumentError(String argument) {
        return fieldError(
            argument,
            "Failed to convert value of type 'java.lang.String' to required type 'long'",
            "senderIdHolder",
            "typeMismatch"
        );
    }
}
