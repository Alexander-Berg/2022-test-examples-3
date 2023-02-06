package ru.yandex.market.tpl.carrier.planner.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.RunSubtypeDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RunControllerSubtypeTest extends BasePlannerWebTest {

    private final RunGenerator runGenerator;
    private final MockMvc mockMvc;

    private Run mainRun;
    private Run suppRun;

    @BeforeEach
    void setUp() {
        mainRun = runGenerator.generate();

        suppRun = runGenerator.generate(rgp -> rgp.runSubtype(RunSubtype.SUPPLEMENTARY_1));
    }


    @SneakyThrows
    @Test
    void shouldReturnMain() {
        mockMvc.perform(get("/internal/runs").param("subtypes", RunSubtypeDto.MAIN.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(mainRun.getId()))
                .andExpect(jsonPath("$.content[0].subtype").value(RunSubtypeDto.MAIN.getValue()));
    }

    @SneakyThrows
    @Test
    void shouldNotReturnSupplementary1() {
        mockMvc.perform(get("/internal/runs").param("subtypes", RunSubtypeDto.SUPPLEMENTARY_1.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(suppRun.getId()))
                .andExpect(jsonPath("$.content[0].subtype").value(RunSubtypeDto.SUPPLEMENTARY_1.getValue()));
    }

    @SneakyThrows
    @Test
    void shouldReturnBoth() {
        mockMvc.perform(get("/internal/runs").param("subtypes", RunSubtypeDto.SUPPLEMENTARY_1.getValue(),
                        RunSubtypeDto.MAIN.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }
}
