package ru.yandex.market.logistics.management.controller.health;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.HealthCheckResult;
import ru.yandex.market.logistics.management.service.health.ping.HealthCheckerStub;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingTest extends AbstractContextualTest {

    @Autowired
    private HealthCheckerStub healthCheckerStub;

    @Test
    void somethingWentWrong() throws Exception {
        when(healthCheckerStub.checkHealth())
            .thenReturn(HealthCheckResult.failure("Накрылось всё медным тазом"));

        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("2;Накрылось всё медным тазом"));

        Mockito.verify(healthCheckerStub).checkHealth();
    }

    @Test
    void exceptionThrownOnChecking() throws Exception {
        when(healthCheckerStub.checkHealth())
            .thenThrow(new RuntimeException("Накрылось всё медным тазом"));
        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("2;Exception acquired on health checking: Накрылось всё медным тазом"));

        Mockito.verify(healthCheckerStub).checkHealth();
    }

    @Test
    void pingIsHealthy() throws Exception {
        when(healthCheckerStub.checkHealth())
            .thenReturn(HealthCheckResult.ok());

        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("0;OK"));

        Mockito.verify(healthCheckerStub).checkHealth();
    }
}
