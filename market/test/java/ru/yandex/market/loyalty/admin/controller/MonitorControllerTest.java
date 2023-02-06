package ru.yandex.market.loyalty.admin.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.HealthCheckProcessor;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dinyat
 * 04/09/2017
 */
@TestFor(HealthCheckProcessor.class)
public class MonitorControllerTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HealthCheckProcessor healthCheckProcessor;

    @Test
    public void testHealthCheckMonitoring() throws Exception {
        healthCheckProcessor.healthCheck();

        String result = mockMvc
                .perform(get("/monitor/health_check"))
                .andDo(log())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals("1;healthCheck was not called in last hour", result);
    }

    @Test
    public void testJugglerPushMonitoring() throws Exception {
        clock.spendTime(10, ChronoUnit.MINUTES);

        String result = mockMvc
                .perform(get("/monitor/juggler"))
                .andDo(log())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals("1;Отправка эвентов похоже сломалась: события не отправлялись уже более пяти минут", result);
    }
}
