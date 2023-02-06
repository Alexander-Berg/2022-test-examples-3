package ru.yandex.market.logistics.werewolf.controller;

import java.util.regex.Pattern;

import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.PingChecker;
import ru.yandex.market.logistics.werewolf.AbstractTest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Проверка здоровья сервиса")
class ApplicationMonitoringControllerTest extends AbstractTest {

    @Autowired
    private PingChecker mockPingChecker;

    @Test
    @DisplayName("Успешный пинг")
    void pingIsHealthy() throws Exception {
        doReturn(CheckResult.OK).when(mockPingChecker).check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string("0;OK"));
    }

    @Test
    @DisplayName("Неуспешный пинг")
    void somethingWentWrong() throws Exception {
        doReturn(new CheckResult(CheckResult.Level.CRITICAL, "Все пропало")).when(mockPingChecker).check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(regexContent("^2;CRIT \\{ru.yandex.market.common.ping.PingChecker.*: Все пропало}$"));
    }

    @Test
    @DisplayName("Исключение при проверке")
    void exceptionThrownWhileHealthChecking() throws Exception {
        doThrow(new RuntimeException("Все пропало")).when(mockPingChecker).check();

        mockMvc.perform(get("/ping"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(regexContent("^2;CRIT \\{ru.yandex.market.common.ping.PingChecker.*: Checker exception}$"));
    }

    private ResultMatcher regexContent(String regex) {
        return content().string(new MatchesPattern(Pattern.compile(regex)));
    }
}
