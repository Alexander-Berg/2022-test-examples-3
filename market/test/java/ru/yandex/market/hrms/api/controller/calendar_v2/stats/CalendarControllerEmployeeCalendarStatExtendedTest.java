package ru.yandex.market.hrms.api.controller.calendar_v2.stats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"CalendarControllerEmployeeDayStatsV2Test.schedules.csv",
        "CalendarControllerEmployeeDayStatsV2Test.before.csv"})
public class CalendarControllerEmployeeCalendarStatExtendedTest extends AbstractApiTest {

    @Test
    @DisplayName("Календарная статистика по сотрудникам")
    @DbUnitDataSet(before = "CalendarControllerEmployeeCalendarStatExtendedTest.SaveStats.before.csv",
            after = "CalendarControllerEmployeeCalendarStatExtendedTest.SaveStats.after.csv")
    void saveStats() throws Exception {
        mockMvc.perform(get("/manual/get-employees-stats")
                        .param("domainId", "1")
                        .param("dateFrom", "2021-12-23")
                        .param("dateTo", "2021-12-23"))
                .andExpect(status().isOk());
    }
}
