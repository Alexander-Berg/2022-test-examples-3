package ru.yandex.market.hrms.api.controller.timex;

import java.time.Clock;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "TimexEmployeeControllerTest.GetEmployeeRelocation.csv")
public class TimexEmployeeControllerTest extends AbstractApiTest {

    @Autowired
    Clock clock;

    @Test
    void getEmployeeRelocation() throws Exception {
        mockMvc.perform(get("/lms/getEmployeeRelocation/SOF")
                        .param("from", "2021-10-10T10:00:00.00Z")
                        .param("to", "2021-10-10T23:59:59.00Z")
                ).andExpect(status().isOk())
                .andExpect((content().json(loadFromFile("isOk.json"))));
    }

    @Test
    void getEmployeeRelocationWithoutTo() throws Exception {
        mockMvc.perform(get("/lms/getEmployeeRelocation/SOF")
                        .param("from", "2021-10-10T10:00:00.00Z")
                ).andExpect(status().isOk())
                .andExpect((content().json(loadFromFile("isOk.json"))));
    }

    @Test
    void getEmployeeRelocationWithoutTime() throws Exception {
        mockClock(LocalDateTime.of(2021, 10, 10,23,00));
        mockMvc.perform(get("/lms/getEmployeeRelocation/SOF")
                ).andExpect(status().isOk())
                .andExpect((content().json(loadFromFile("isOk.json"))));
    }

    @Test
    void getEmployeeRelocationException() throws Exception {
        mockMvc.perform(get("/lms/getEmployeeRelocation/SOF")
                        .param("to", "2021-10-10T23:59:59.00Z")
                ).andExpect(status().is4xxClientError());
    }
}
