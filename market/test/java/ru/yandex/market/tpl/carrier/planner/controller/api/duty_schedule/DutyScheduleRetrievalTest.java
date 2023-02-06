package ru.yandex.market.tpl.carrier.planner.controller.api.duty_schedule;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.duty_schedule.DutyScheduleGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class DutyScheduleRetrievalTest extends BasePlannerWebTest {

    private final DutyScheduleGenerator dutyScheduleGenerator;
    private final TestUserHelper testUserHelper;

    @BeforeEach
    void setUp() {
        testUserHelper.deliveryService(DutyScheduleGenerator.DEFAULT_DS_ID);
    }

    @SneakyThrows
    @Test
    void shouldGetDutySchedule() {

        dutyScheduleGenerator.generate();

        mockMvc.perform(get("/internal/duty-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({
            "123,1",
            "124,0"
    })
    void shouldGetDutyScheduleByDeliveryServiceId(long deliveryServiceId, int count) {
        dutyScheduleGenerator.generate();

        mockMvc.perform(get("/internal/duty-schedules")
                .param("dutyDeliveryServiceId", String.valueOf(deliveryServiceId))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(count)));

    }

}
