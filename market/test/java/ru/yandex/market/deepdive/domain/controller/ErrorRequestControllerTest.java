package ru.yandex.market.deepdive.domain.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.AbstractTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorRequestControllerTest extends AbstractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void emptyRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content("{}"))
                .andExpect(status().is(500));
    }

    @Test
    public void errorRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content("{\"error500\": true}"))
                .andExpect(status().is(500));
    }

    @Test
    public void nonErrorRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content("{\"error500\": false}"))
                .andExpect(status().isOk());
    }
}
