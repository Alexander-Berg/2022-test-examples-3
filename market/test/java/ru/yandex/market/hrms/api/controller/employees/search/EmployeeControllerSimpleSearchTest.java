package ru.yandex.market.hrms.api.controller.employees.search;


import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "EmployeeControllerSimpleSearchTest.before.csv")
public class EmployeeControllerSimpleSearchTest extends AbstractApiTest {

    @Test
    void shouldDoSimpleSearch() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 1, 14, 4, 4));
        mockMvc.perform(get("/lms/employees")
                .param("name", "Адам"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("only_adam.json"), true));
    }

    @Test
    void shouldDoSimpleSearch2() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 1, 14, 4, 4));
        mockMvc.perform(get("/lms/employees")
                        .param("name", "алён"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("only_alena.json"), true));
    }
}
