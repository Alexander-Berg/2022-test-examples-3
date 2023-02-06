package ru.yandex.market.deliverycalculator.searchengine.controller;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.workflow.solomon.SolomonController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты для контроллера Соломона.
 * {@link SolomonController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
class SolomonFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Проверка, что /solomon отвечает корректно")
    void solomonTestOk() throws Exception {
        final ResultActions result = mockMvc.perform(
                get("/solomon")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Проверка, что /solomon отвечает корректно")
    void solomonJvmTestOk() throws Exception {
        final ResultActions result = mockMvc.perform(
                get("/solomon-jvm")
                        .contentType(MediaType.APPLICATION_JSON)
        );
        result.andExpect(status().isOk());
    }

}
