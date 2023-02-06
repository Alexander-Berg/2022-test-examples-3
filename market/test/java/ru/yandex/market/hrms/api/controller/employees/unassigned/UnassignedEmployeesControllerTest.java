package ru.yandex.market.hrms.api.controller.employees.unassigned;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "UnassignedEmployeesControllerTest.before.csv")
class UnassignedEmployeesControllerTest extends AbstractApiTest {

    @Test
    @DisplayName("Получить всех нераспределенных сотрудников")
    void getUnassigned() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        mockMvc.perform(get("/lms/employees/unassigned"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("UnassignedEmployeesControllerTest.getUnassigned.json"), true));
    }


    @Test
    @DisplayName("Получить первую страницу нераспределенных сотрудников")
    void getUnassignedFirstPage() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        mockMvc.perform(get("/lms/employees/unassigned")
                .queryParam("page", "0")
                .queryParam("size", "1")
        )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        loadFromFile("UnassignedEmployeesControllerTest.getUnassignedFirstPage.json"), true));
    }

    @Test
    @DisplayName("Получить всех нераспределенных сотрудников")
    void getUnassignedV2() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        mockMvc.perform(get("/lms/employees/unassignedV2").queryParam("domainId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("UnassignedEmployeesControllerTest.getUnassignedV2.json"), true));
    }

    @Test
    @DisplayName("Получить всех нераспределенных сотрудников по имени")
    void getUnassignedFirstPageV2Name() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        mockMvc.perform(get("/lms/employees/unassignedV2")
                        .queryParam("domainId", "1")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("employeeName", "Катя")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("UnassignedEmployeesControllerTest.getUnassignedV2.Name.json"), true));
    }

    @Test
    @DisplayName("Получить всех нераспределенных сотрудников по должности")
    void getUnassignedFirstPageV2Position() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        mockMvc.perform(get("/lms/employees/unassignedV2")
                        .queryParam("domainId", "1")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("position", "Позиция")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("UnassignedEmployeesControllerTest.getUnassignedV2.Position.json"), true));
    }

    @Test
    @DisplayName("Получить всех нераспределенных сотрудников только 1 страница")
    void getUnassignedFirstPageV2() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        mockMvc.perform(get("/lms/employees/unassignedV2")
                        .queryParam("domainId", "1")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("UnassignedEmployeesControllerTest.getUnassignedV2.Page.json"), true));
    }
}
