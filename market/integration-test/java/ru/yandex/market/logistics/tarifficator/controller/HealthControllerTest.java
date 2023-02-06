package ru.yandex.market.logistics.tarifficator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.service.health.checker.StubPingChecker;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public class HealthControllerTest extends AbstractContextualTest {

    @Autowired
    private StubPingChecker pingChecker;

    @Test
    void pingIsHealthy() throws Exception {
        when(pingChecker.check()).thenReturn(CheckResult.OK);

        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("0;OK"));

        verify(pingChecker).check();
    }

    @Test
    void somethingWentWrong() throws Exception {
        when(pingChecker.check()).thenReturn(new CheckResult(CheckResult.Level.CRITICAL, "Все пропало"));

        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("2;Все пропало"));

        verify(pingChecker).check();
    }

    @Test
    void exceptionThrownWhileHealthChecking() throws Exception {
        when(pingChecker.check()).thenThrow(new RuntimeException("Все пропало"));
        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().string("2;Exception occurred on health checking: Все пропало"));

        verify(pingChecker).check();
    }

}
