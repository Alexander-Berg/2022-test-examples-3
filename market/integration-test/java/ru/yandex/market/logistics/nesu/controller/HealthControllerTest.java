package ru.yandex.market.logistics.nesu.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.service.health.StubPingChecker;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Проверка здоровья сервиса")
class HealthControllerTest extends AbstractContextualTest {

    @Autowired
    private StubPingChecker pingChecker;

    @BeforeEach
    void setup() {
        when(pingChecker.check()).thenReturn(CheckResult.OK);
    }

    @Test
    @DisplayName("Успешный пинг")
    void pingIsHealthy() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));

        verify(pingChecker).check();
    }

    @Test
    @DisplayName("Успешный пинг OpenAPI")
    void apiPing() throws Exception {
        mockMvc.perform(get("/api/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));
    }

    @Test
    @DisplayName("Неуспешный пинг")
    void somethingWentWrong() throws Exception {
        doReturn(new CheckResult(CheckResult.Level.CRITICAL, "Все пропало"))
            .when(pingChecker)
            .check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("2;Все пропало"));
    }

    @Test
    @DisplayName("Исключение при проверке")
    void exceptionThrownWhileHealthChecking() throws Exception {
        doThrow(new RuntimeException("Все пропало"))
            .when(pingChecker)
            .check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("2;Exception occurred on health checking: Все пропало"));
    }
}
