package ru.yandex.market.deepdive.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.deepdive.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorRequestControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Case of nullable request")
    public void testWithNullRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Case of empty request")
    public void testWithEmptyRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
               .content("[]")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Case of 500 request")
    public void testWithFailureRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
               .content("{\"isError\":true}")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Case of 200 request")
    public void testWithSuccessRequest() throws Exception {
        mockMvc.perform(put("/api/error500")
               .content("{\"isError\":false}")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is2xxSuccessful());
    }
}
