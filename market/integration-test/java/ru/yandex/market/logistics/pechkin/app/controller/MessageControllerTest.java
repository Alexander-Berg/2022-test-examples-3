package ru.yandex.market.logistics.pechkin.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.pechkin.app.AbstractTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageControllerTest extends AbstractTest {

    @Test
    void handleMessageTest() throws Exception {
        MessageDto messageDto = new MessageDto();
        messageDto.setChannel("some");
        messageDto.setMessage("some");
        messageDto.setSender("some");
        mockMvc.perform(MockMvcRequestBuilders.post("/message")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(messageDto)))
            .andExpect(status().isOk());
    }

}
