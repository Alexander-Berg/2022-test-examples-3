package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.monitoring.beans.JugglerEventsPushExecutor;
import ru.yandex.market.loyalty.monitoring.beans.MonitorController;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 05.04.17
 */
@TestFor(MonitorController.class)
public class MonitorControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JugglerEventsPushExecutor jugglerEventsPushExecutor;

    @Test
    public void testDefaultMonitorSuccess() throws Exception {
        jugglerEventsPushExecutor.reset();

        String response = mockMvc
                .perform(get("/monitor/juggler"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals("0;OK", response);
    }
}
