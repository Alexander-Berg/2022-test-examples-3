package ru.yandex.market.checkout.carter.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthControllerTest extends CarterMockedDbTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHealthControllerSmoke() throws Exception {
        mockMvc.perform(get("/health/solomon"))
                .andDo(log())
                .andExpect(status().isOk());
    }
}
