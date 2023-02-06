package ru.yandex.market.checkout.carter.web;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class PagematchControllerTest extends CarterMockedDbTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void printResult() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pagematch"))
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.not(Matchers.startsWith("\""))));

    }

}
