package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CompanyControllerTest extends BasePlannerWebTest {
    private final TestUserHelper testUserHelper;

    private Company firstCompany;
    private Company secondCompany;

    @BeforeEach
    void init() {
        firstCompany = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Company1")
                .campaignId(22L)
                .deliveryServiceIds(Set.of(123L, 9001L))
                .login("login1")
                .build());
        secondCompany = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Company2")
                .campaignId(23L)
                .deliveryServiceIds(Set.of(124L))
                .login("login2")
                .build());
    }

    @Test
    void testGet() throws Exception {
        mockMvc.perform(get("/internal/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));


        mockMvc.perform(get("/internal/companies")
                        .param("id", firstCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value(firstCompany.getName()))
                .andExpect(jsonPath("$.content[0].id").value(firstCompany.getId()))
                .andExpect(jsonPath("$.content[0].deliveryServiceId").value(123L));


        mockMvc.perform(get("/internal/companies")
                        .param("deliveryServiceId", "124"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value(secondCompany.getName()))
                .andExpect(jsonPath("$.content[0].id").value(secondCompany.getId()))
                .andExpect(jsonPath("$.content[0].deliveryServiceId").value(124L));


        mockMvc.perform(get("/internal/companies")
                        .param("nameSubstring", "Company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));


    }
}
