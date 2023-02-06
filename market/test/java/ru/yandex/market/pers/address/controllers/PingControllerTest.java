package ru.yandex.market.pers.address.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingControllerTest extends BaseWebTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PingController pingController;

    @Test
    void testPing() throws Exception {
        pingController.onServerStart(null);
        String response = mockMvc
            .perform(get("/ping"))
            .andDo(log())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertEquals("0;OK", response);
    }
}
