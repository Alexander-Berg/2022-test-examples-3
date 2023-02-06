package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerDetailDutyTest extends BasePlannerWebTest {

    private final TestUserHelper testUserHelper;
    private final DutyGenerator dutyGenerator;

    private Run run;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        testUserHelper.deliveryService(123L, Set.of(company));
        Duty duty1 = dutyGenerator.generate();
        run = duty1.getRun();

    }

    @SneakyThrows
    @Test
    void shouldReturnPalletsFromRunDetails() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/runs/{id}", run.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pallets").value(33));
    }
}
