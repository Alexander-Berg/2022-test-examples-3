package ru.yandex.market.loyalty.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.back.config.MarketLoyaltyBack;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.WarmUpEndEvent;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 05.04.17
 */
@TestFor(PingController.class)
public class PingControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    @MarketLoyaltyBack
    private ObjectMapper objectMapper;

    @Test
    public void testPing() throws Exception {
        String response = ping();
        assertEquals("2;NOT_WARMED", response);

        applicationEventPublisher.publishEvent(new WarmUpEndEvent(new Object()));
        response = ping();
        assertEquals("0;OK", response);
    }

    private String ping() throws Exception {
        return mockMvc
                .perform(get("/ping"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testNoHandlerFound() throws Exception {
        String response = mockMvc
                .perform(get("/non-existent-handler"))
                .andDo(log())
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        MarketLoyaltyError marketLoyaltyError = objectMapper.readValue(response, MarketLoyaltyError.class);
        assertNotNull(marketLoyaltyError);
    }
}
