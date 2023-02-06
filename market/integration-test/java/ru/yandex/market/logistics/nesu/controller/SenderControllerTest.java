package ru.yandex.market.logistics.nesu.controller;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.dto.SenderDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class SenderControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить информацию о сендере")
    @DatabaseSetup("/service/sender/before/get_sender.xml")
    void getSender() throws Exception {
        mockMvc.perform(
            get("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/sender/get_sender_response.json"));
    }

    @Test
    @DisplayName("Получить информацию о несуществующем сендере")
    void getSenderNotFound() throws Exception {
        mockMvc.perform(
            get("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/sender/sender_not_found_response.json"));
    }

    @Test
    @DisplayName("Получить информацию о недоступном сендере")
    @DatabaseSetup("/service/sender/before/get_sender.xml")
    void getUnavailableSender() throws Exception {
        mockMvc.perform(
            get("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "2")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/sender/sender_not_found_response.json"));
    }

    @Test
    @DisplayName("Получить информацию обо всех сендерах магазина")
    @DatabaseSetup("/service/sender/before/get_senders.xml")
    void getSenders() throws Exception {
        mockMvc.perform(
            get("/back-office/senders")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/sender/get_senders_response.json"));
    }

    @Test
    @DisplayName("Получить информацию обо всех сендерах несуществующего магазина")
    void getSendersShopNotFound() throws Exception {
        mockMvc.perform(
            get("/back-office/senders")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/sender/get_senders_not_found_response.json"));
    }

    @Test
    @DisplayName("Создать новый сендер для магазина")
    @DatabaseSetup("/service/sender/before/create_sender.xml")
    @ExpectedDatabase(value = "/service/sender/after/create_sender.xml", assertionMode = NON_STRICT_UNORDERED)
    void createSender() throws Exception {
        mockMvc.perform(
            post("/back-office/senders")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/sender/create_sender_request.json"))
        )
            .andExpect(status().isMethodNotAllowed())
            .andExpect(errorMessage("Method is no longer allowed"));
    }

    @Test
    @DisplayName("Валидация полей создаваемого сендера")
    void createInvalidSender() throws Exception {
        mockMvc.perform(
            post("/back-office/senders")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SenderDto.builder().build()))
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "contact",
                "must not be null",
                "senderDto",
                "NotNull"
            )));
    }

    @Test
    @DisplayName("Обновить контактную информацию о сендере")
    @DatabaseSetup("/service/sender/before/update_sender.xml")
    @ExpectedDatabase(value = "/service/sender/after/update_sender.xml", assertionMode = NON_STRICT)
    void updateSender() throws Exception {
        mockMvc.perform(
            patch("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/sender/update_sender_request.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/sender/update_sender_response.json"));
    }

    @Test
    @DisplayName("Обновить контактную информацию о недоступном сендере")
    @DatabaseSetup("/service/sender/before/update_sender.xml")
    @ExpectedDatabase(value = "/service/sender/before/update_sender.xml", assertionMode = NON_STRICT)
    void updateUnavailableSender() throws Exception {
        mockMvc.perform(
            patch("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/sender/update_sender_request.json"))
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/sender/sender_not_found_response.json"));
    }

    @Test
    @DisplayName("Невалидные параметры в контактной информации при обновлении")
    void updateSenderBadRequest() throws Exception {
        mockMvc.perform(
            patch("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/sender/update_sender_400_request.json"))
        ).andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError(
                    "emails[0]",
                    "must be a well-formed email address",
                    "contactDto",
                    "Email",
                    Map.of("regexp", ".*")
                ),
                fieldError("firstName", "must not be blank", "contactDto", "NotBlank"),
                fieldError("lastName", "must not be blank", "contactDto", "NotBlank")
            )));
    }

    @Test
    @DisplayName("Пустой email в контактной информации при обновлении")
    void updateSenderEmptyEmail() throws Exception {
        mockMvc.perform(
            patch("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/sender/update_sender_empty_emails_request.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fieldError("emails", "must not be empty", "contactDto", "NotEmpty")
            ));
    }

    @Test
    @DisplayName("Удалить информацию о сендере")
    @DatabaseSetup("/service/sender/before/delete_sender.xml")
    @ExpectedDatabase(value = "/service/sender/after/delete_sender.xml", assertionMode = NON_STRICT)
    void deleteSender() throws Exception {
        mockMvc.perform(
            delete("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удалить информацию о последнем сендере")
    @DatabaseSetup("/service/sender/before/delete_last_sender.xml")
    @ExpectedDatabase(value = "/service/sender/before/delete_last_sender.xml", assertionMode = NON_STRICT)
    void deleteLastSender() throws Exception {
        mockMvc.perform(
            delete("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Last sender 1 of shop 1 cannot be deleted"));
    }

    @Test
    @DisplayName("Удалить информацию о недоступном сендере")
    @DatabaseSetup("/service/sender/before/delete_sender.xml")
    @ExpectedDatabase(value = "/service/sender/before/delete_sender.xml", assertionMode = NON_STRICT)
    void deleteUnavailableSender() throws Exception {
        mockMvc.perform(
            delete("/back-office/senders/1")
                .param("userId", "1")
                .param("shopId", "2")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/sender/sender_not_found_response.json"));
    }
}
