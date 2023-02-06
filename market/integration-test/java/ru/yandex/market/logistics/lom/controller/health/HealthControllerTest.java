package ru.yandex.market.logistics.lom.controller.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.PingChecker;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Проверка здоровья сервиса")
class HealthControllerTest extends AbstractContextualTest {

    @Autowired
    private PingChecker mockPingChecker;

    @Test
    @DisplayName("Успешный пинг")
    void pingIsHealthy() throws Exception {
        doReturn(CheckResult.OK).when(mockPingChecker).check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));
    }

    @Test
    @DisplayName("Неуспешный пинг")
    void somethingWentWrong() throws Exception {
        doReturn(new CheckResult(CheckResult.Level.CRITICAL, "Все пропало")).when(mockPingChecker).check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("2;Все пропало"));
    }

    @Test
    @DisplayName("Исключение при проверке")
    void exceptionThrownWhileHealthChecking() throws Exception {
        doThrow(new RuntimeException("Все пропало")).when(mockPingChecker).check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("2;Exception occurred on health checking: Все пропало"));
    }
}
