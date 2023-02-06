package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.core.domain.run.RunType;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.tpl.carrier.core.domain.run.Run.INTAKE_DELIVERY_SERVICE_ID;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerTypeSubtypeTest extends BasePlannerWebTest {

    private final RunGenerator runGenerator;
    private final RunRepository runRepository;

    private Run run1;
    private Run run2;
    private Run run3;

    @BeforeEach
    void setUp() {
        run1 = runGenerator.generate(rgp -> rgp.runSubtype(RunSubtype.MAIN));
        run2 = runGenerator.generate(rgp -> rgp.runType(RunType.LINEHAUL).runSubtype(RunSubtype.SUPPLEMENTARY_1));
        run3 = runGenerator.generate(
                rgp -> rgp
                    .deliveryServiceId(INTAKE_DELIVERY_SERVICE_ID)
                    .runType(RunType.INTAKE)
                    .runSubtype(null),
                List.of(
                    Pair.of(
                        Function.identity(),
                        movementCreate -> movementCreate
                            .subtype(null)
                            .type(MovementType.ORDERS_OPERATION)
                    )
                ));
    }

    @SneakyThrows
    @Test
    void shouldFilterByTypeAndSubtype() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs")
                .param("typeAndSubtypes", RunType.LINEHAUL + "/" + RunSubtype.MAIN))
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run1.getId()));
    }

    @SneakyThrows
    @Test
    @Transactional
    void shouldFilterByTypeAndSubtype2() {
        var runs = runRepository.findAll();
        var runItems = runs.stream().filter(r -> r.getRunType().equals(RunType.INTAKE)).flatMap(Run::streamRunItems).collect(Collectors.toList());
        var movements = runItems.stream().map(RunItem::getMovement).collect(Collectors.toList());
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs")
                        .param("typeAndSubtypes", RunType.LINEHAUL + "/" + RunSubtype.SUPPLEMENTARY_1))
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run2.getId()));
    }

    @SneakyThrows
    @Test
    void shouldFilterByTypeAndSubtype3() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs")
                        .param("typeAndSubtypes", RunType.INTAKE.name()))
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run3.getId()));
    }

    @SneakyThrows
    @Test
    void shouldFilterByTypeAndSubtype4() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs")
                        .param("typeAndSubtypes", RunType.LINEHAUL.name() + "/" + RunSubtype.MAIN)
                        .param("typeAndSubtypes", RunType.LINEHAUL.name() + "/" + RunSubtype.SUPPLEMENTARY_1)
                )
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(run1.getId()))
                .andExpect(jsonPath("$.content[1].id").value(run2.getId()));
    }


}
