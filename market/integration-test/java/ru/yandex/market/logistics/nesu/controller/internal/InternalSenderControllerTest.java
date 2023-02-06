package ru.yandex.market.logistics.nesu.controller.internal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Внутренняя ручка получения сендеров")
class InternalSenderControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить информацию обо всех сендерах")
    @DatabaseSetup("/controller/internal/sender/get_senders.xml")
    void getSenders() throws Exception {
        mockMvc.perform(get("/internal/senders"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("response/senders_response.json"));
    }

    @Test
    @DisplayName("Получить информацию обо всех сендерах когда их нет")
    void getSendersEmpty() throws Exception {
        mockMvc.perform(get("/internal/senders"))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Получить информацию о сендере")
    @DatabaseSetup("/controller/internal/sender/get_senders.xml")
    void getSender() throws Exception {
        mockMvc.perform(get("/internal/senders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("response/sender_response.json"));
    }

    @Test
    @DisplayName("Получить информацию об удаленном сендере")
    @DatabaseSetup("/controller/internal/sender/get_senders.xml")
    void getDeletedSender() throws Exception {
        mockMvc.perform(get("/internal/senders/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("response/sender_deleted_response.json"));
    }

    @Test
    @DisplayName("Получить информацию о несуществующем сендере")
    void getSenderNotFound() throws Exception {
        mockMvc.perform(get("/internal/senders/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("response/sender_not_found_response.json"));
    }

}
