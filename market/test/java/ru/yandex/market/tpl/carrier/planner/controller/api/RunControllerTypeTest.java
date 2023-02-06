package ru.yandex.market.tpl.carrier.planner.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.mj.generated.server.model.RunTypeDto;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerTypeTest extends BasePlannerWebTest {

    private final RunGenerator runGenerator;
    private final MockMvc mockMvc;

    private Run linehaulRun;
    private Run intakeRun;

    @BeforeEach
    void setUp() {
        linehaulRun = runGenerator.generate();

        intakeRun = runGenerator.generate(rgp -> rgp.deliveryServiceId(Run.INTAKE_DELIVERY_SERVICE_ID));
    }


    @SneakyThrows
    @Test
    void shouldReturnIntake() {
        mockMvc.perform(get("/internal/runs").param("types", RunTypeDto.INTAKE.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(intakeRun.getId()))
                .andExpect(jsonPath("$.content[0].type").value(RunTypeDto.INTAKE.getValue()));
    }

    @SneakyThrows
    @Test
    void shouldNotReturnIntake() {
        mockMvc.perform(get("/internal/runs").param("types", RunTypeDto.LINEHAUL.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(linehaulRun.getId()))
                .andExpect(jsonPath("$.content[0].type").value(RunTypeDto.LINEHAUL.getValue()));
    }

    @SneakyThrows
    @Test
    void shouldReturnBothLinehaulAndIntake() {
        mockMvc.perform(get("/internal/runs").param("types", RunTypeDto.INTAKE.getValue(), RunTypeDto.LINEHAUL.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }
}
