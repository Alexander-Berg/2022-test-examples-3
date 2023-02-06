package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RunControllerExternalIdTest extends BasePlannerWebTest {

    private final RunGenerator runGenerator;
    private final ObjectMapper tplObjectMapper;

    private Run run1;
    private Run run2;

    @BeforeEach
    void setUp() {
        run1 = runGenerator.generate(r -> r.externalId("TMT123"));
        run2 = runGenerator.generate(r -> r.externalId("TMT321"));
    }

    @Test
    @SneakyThrows
    void shouldReturnMapOfIds() {
        var requestBody = tplObjectMapper.writeValueAsString(
                List.of(run1.getExternalId(), run2.getExternalId())
        );
        mockMvc.perform(put("/internal/runs/by-external-id")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].key").value(run1.getExternalId()))
                .andExpect(jsonPath("$[0].value").value(run1.getId()))
                .andExpect(jsonPath("$[1].key").value(run2.getExternalId()))
                .andExpect(jsonPath("$[1].value").value(run2.getId()));
        ;
    }


    @Test
    @SneakyThrows
    void shouldReturnMapOfIdsPartial() {
        var requestBody = tplObjectMapper.writeValueAsString(
                List.of(run1.getExternalId(), "TMT4000")
        );
        mockMvc.perform(put("/internal/runs/by-external-id")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].key").value(run1.getExternalId()))
                .andExpect(jsonPath("$[0].value").value(run1.getId()));
    }

    @Test
    @SneakyThrows
    void shouldReturnMapOfIdsEmpty() {
        var requestBody = tplObjectMapper.writeValueAsString(
                List.of("TMT3000", "TMT4000")
        );
        mockMvc.perform(put("/internal/runs/by-external-id")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(0)));
    }

}
