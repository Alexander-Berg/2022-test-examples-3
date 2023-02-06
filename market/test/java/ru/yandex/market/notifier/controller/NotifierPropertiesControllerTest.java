package ru.yandex.market.notifier.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.notifier.application.AbstractWebTestBase;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotifierPropertiesControllerTest extends AbstractWebTestBase {

    @Test
    void shouldReturnDefaultProperties() throws Exception {
        mockMvc.perform(get("/properties"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minifyOutputLogs", is(true)));
    }

    @Test
    void shouldSetMinifyOutputLogsProperty() throws Exception {
        mockMvc
                .perform(put("/properties/minifyOutputLogs")
                        .content("false")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minifyOutputLogs", is(false)));
    }

    @Test
    void shouldReturnBeruPostomatDeliveryServiceIds() throws Exception {
        notifierProperties.setEventProcessingRetryDelayMs(10203040);

        mockMvc.perform(get("/properties"))
                .andDo(log())
                .andExpect(jsonPath("$.eventProcessingRetryDelayMs", is(10203040)));
    }
}
