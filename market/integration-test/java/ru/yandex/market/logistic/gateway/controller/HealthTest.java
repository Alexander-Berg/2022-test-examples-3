package ru.yandex.market.logistic.gateway.controller;

import org.junit.Test;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class HealthTest extends AbstractIntegrationTest {
    @Test
    public void ping() throws Exception {
        String contentAsString = mockMvc.perform(get("/ping"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertEquals("0;OK", contentAsString);
    }
}
