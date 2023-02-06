package ru.yandex.market.logistics.nesu.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.objectError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class OfferControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить список офферов, удовлетворяющих текстовому запросу")
    @DatabaseSetup("/service/offer/before/offers-db.xml")
    void searchOffers() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .param("substring", "Ameri")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent("controller/offer/search_offers_response.json")));
    }

    @Test
    @DisplayName("Получить пустой список офферов")
    @DatabaseSetup("/service/offer/before/offers-db.xml")
    void getEmptyList() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .param("substring", "лиса")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Отсутствует обязательный параметр")
    void missingRequiredParameter() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("substring", "String"));
    }

    @Test
    @DisplayName("Параметр запроса substring пустой")
    void substringIsEmpty() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .param("substring", "")
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(objectError(
                "substring",
                "must not be blank",
                "NotBlank"
            )));
    }

    @Test
    @DisplayName("Не буквенно-цифровые символы фильтруются")
    @DatabaseSetup("/service/offer/before/offers-db.xml")
    void nonAlphaNumericSymbolsFiltered() throws Exception {
        mockMvc.perform(
            get("/back-office/offers")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .param("substring", "';!$")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }
}
