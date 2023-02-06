package ru.yandex.market.hrms.api.controller.ispring;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "ISpringControllerSearchEmployeesTest.before.csv")
public class ISpringControllerSearchEmployeesTest extends AbstractApiTest {

    @Test
    public void shouldFoundOneWhenMatchName() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("name", "Владимир")
                        .queryParam("page", "0")
                        .queryParam("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_name.json")));
    }

    @Test
    public void shouldFoundSeveralEmployeesWhenMatchPosition() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("position", "акте")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_position.json")));
    }

    @Test
    public void shouldNotFoundWhenDeactivatedEmployeeInSpring() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("name", "Крылов")
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_empty.json")));
    }

    @Test
    public void shouldNotFoundWhenUseManyParameters() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("areaId", "1")
                        .queryParam("companyId", "1")
                        .queryParam("employeeTypes", "CANDIDATE")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_empty.json")));
    }

    @Test
    public void shouldReturnTwoPagesWhenFoundSeveralPages() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("position", "актер")
                        .queryParam("name", "сергей")
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_position_page_1.json")));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("position", "актер")
                        .queryParam("name", "сергей")
                        .queryParam("page", "1")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_position_page_2.json")));
    }

    @Test
    public void shouldFoundWhenEmployeeIsFiredInFuture() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("name", "боярская")
                        .queryParam("employeeTypes", "STAFF")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_staff_fired_in_future.json")));
    }

    @Test
    public void shouldNotFoundWhenOutstaffIsDeactived() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("name", "малахов")
                        .queryParam("employeeTypes", "OUTSTAFF")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_empty.json")));
    }

    @Test
    public void shouldNotFoundWhenCandidateIsDeleted() throws Exception {
        mockClock(LocalDate.of(2022, 1, 1));

        mockMvc.perform(get("/lms/ispring/employees")
                        .queryParam("name", "Жора")
                        .queryParam("employeeTypes", "CANDIDATE")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_empty.json")));
    }
}
