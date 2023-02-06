package ru.yandex.market.tpl.carrier.planner.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunProcessedByCoordinatorStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.RunProcessedByCoordinatorStatusDto;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerProcessedByCoordinatorStatusTest extends BasePlannerWebTest {

    private final RunGenerator runGenerator;
    private final RunRepository runRepository;
    private final RunCommandService runCommandService;

    private Run run1;
    private Run run2;
    private Run run3;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        run1 = runGenerator.generate();
        run2 = runGenerator.generate();
        run3 = runGenerator.generate();

        runCommandService.markRunProcessedByCoordinator(run1.getId());
    }

    @Test
    @SneakyThrows
    @Transactional
    void shouldBeUnmarkedByDefault() {
        var run4 = runGenerator.generate();
        run4 = runRepository.findById(run4.getId()).orElseThrow();
        Assertions.assertEquals(RunProcessedByCoordinatorStatus.NOT_PROCESSED_BY_COORDINATOR, run4.getProcessedByCoordinatorStatus());
    }

    @Test
    @SneakyThrows
    @Transactional
    void shouldMarkProcessed() {
        var run4 = runGenerator.generate();
        mockMvc.perform(MockMvcRequestBuilders.post("/internal/runs/" + run4.getId() + "/mark-processed-by-coordinator"));
        run4 = runRepository.findById(run4.getId()).orElseThrow();
        Assertions.assertEquals(RunProcessedByCoordinatorStatus.PROCESSED_BY_COORDINATOR, run4.getProcessedByCoordinatorStatus());
    }

    @Test
    @SneakyThrows
    @Transactional
    void shouldUnmarkProcessed() {
        var run4 = runGenerator.generate();
        mockMvc.perform(MockMvcRequestBuilders.post("/internal/runs/" + run4.getId() + "/mark-processed-by-coordinator"));
        mockMvc.perform(MockMvcRequestBuilders.post("/internal/runs/" + run4.getId() + "/unmark-processed-by-coordinator"));

        run4 = runRepository.findById(run4.getId()).orElseThrow();
        Assertions.assertEquals(RunProcessedByCoordinatorStatus.NOT_PROCESSED_BY_COORDINATOR, run4.getProcessedByCoordinatorStatus());
    }

    @Test
    @SneakyThrows
    void shouldNotFilterRunsOnEmptyFilter() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs"))
                .andDo(print())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)));
    }

    @Test
    @SneakyThrows
    void shouldFilterMarkedRuns() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs")
                        .param("processedByCoordinatorStatusDto", RunProcessedByCoordinatorStatusDto.PROCESSED_BY_COORDINATOR.name()))
                .andDo(print())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @Test
    @SneakyThrows
    void shouldFilterUnmarkedRuns() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs")
                    .param("processedByCoordinatorStatusDto", RunProcessedByCoordinatorStatusDto.NOT_PROCESSED_BY_COORDINATOR.name()))
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }
}
