package ru.yandex.market.tpl.carrier.lms.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsRunItemControllerGetItemsTest extends LmsControllerTest {
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;

    private Run run;

    @BeforeEach
    void setUp() {
        run = runGenerator.generate();
    }

    @SneakyThrows
    @Test
    void shouldGetRunItems() {
        Movement movement = run.streamMovements().findFirst().orElseThrow();

        mockMvc.perform(get("/LMS/carrier/runs/{id}/items", run.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].values.movement.id").value(movement.getId()))
                .andExpect(jsonPath("$.items[0].values.movement.displayName").value(movement.getId() + "/" + movement.getExternalId()));
    }
}
