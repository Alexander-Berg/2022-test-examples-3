package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.core.domain.movement.MovementSubtype;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.core.domain.run.RunType;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.RunTypeDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerTypeSubtypeInMultiTypedRunTest extends BasePlannerWebTest {

    private final RunGenerator runGenerator;
    private final RunRepository runRepository;
    private final MockMvc mockMvc;

    private Run linehaulRun;
    private Run intakeRun;
    private Run linehaulRunWithInterwarehouseMovement;

    @BeforeEach
    void setUp() {
        linehaulRun = runGenerator.generate(
                runTuner -> runTuner.runType(RunType.LINEHAUL).runSubtype(RunSubtype.SUPPLEMENTARY_1),
                List.of(
                    Pair.of(
                        riTuner -> riTuner,
                        movementTuner -> movementTuner.type(MovementType.LINEHAUL).subtype(MovementSubtype.SUPPLEMENTARY_1)
                    ),
                    Pair.of(
                        riTuner -> riTuner,
                        movementTuner -> movementTuner.type(MovementType.LINEHAUL).subtype(MovementSubtype.SUPPLEMENTARY_2)
                    )
                )
        );
        intakeRun = runGenerator.generate(
                runTuner -> runTuner.runType(RunType.INTAKE).runSubtype(null),
                List.of(
                    Pair.of(
                        riTuner -> riTuner,
                        movementTuner -> movementTuner.type(MovementType.ORDERS_OPERATION).subtype(null)
                    ),
                    Pair.of(
                        riTuner -> riTuner,
                        movementTuner -> movementTuner.type(MovementType.ORDERS_OPERATION).subtype(null)
                    )
                )
        );
        linehaulRunWithInterwarehouseMovement = runGenerator.generate(
                runTuner -> runTuner.runType(RunType.LINEHAUL).runSubtype(RunSubtype.SUPPLEMENTARY_1),
                List.of(
                    Pair.of(
                        riTuner -> riTuner,
                        movementTuner -> movementTuner.type(MovementType.LINEHAUL).subtype(MovementSubtype.SUPPLEMENTARY_1)
                    ),
                    Pair.of(
                        riTuner -> riTuner,
                        movementTuner -> movementTuner.type(MovementType.INTERWAREHOUSE).subtype(null)
                    )
                )
        );
        Assertions.assertEquals(3, runRepository.findAll().size());
    }


    @SneakyThrows
    @Test
    void shouldReturnLinehauls() {
        mockMvc.perform(get("/internal/runs")
                    .param("types", RunType.LINEHAUL.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].type").value(RunTypeDto.LINEHAUL.getValue()));
    }

    @SneakyThrows
    @Test
    void shouldReturnInterwarehouse() {

        mockMvc.perform(get("/internal/runs")
                        .param("types", RunType.INTERWAREHOUSE.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value(RunTypeDto.LINEHAUL.getValue()));
    }

    @SneakyThrows
    @Test
    void shouldReturnIntake() {
        mockMvc.perform(get("/internal/runs")
                        .param("types", RunType.INTAKE.name()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value(RunTypeDto.INTAKE.getValue()));
    }
}
