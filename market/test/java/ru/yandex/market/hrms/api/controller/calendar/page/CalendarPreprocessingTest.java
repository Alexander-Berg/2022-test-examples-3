package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarPreprocessingService;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"schedules.csv", "environment.csv"}, schema = "public")
public class CalendarPreprocessingTest extends AbstractApiTest {

    @Autowired
    private CalendarPreprocessingService calendarPreprocessingService;

    @Test
    @DbUnitDataSet(before = "CalendarPreprocessingTest.before.csv", schema = "public")
    void shouldReturnPreprocessedCalendar() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 12, 15, 0, 0));
        calendarPreprocessingService.syncCalendarItems(YearMonth.of(2021, 2), 1L);
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_preproc_abs.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "CalendarPreprocessingTest.before.csv", schema = "public")
    void shouldRemovePinkAbsenceAfterManualPresence() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 12, 15, 0, 0));
        calendarPreprocessingService.syncCalendarItems(YearMonth.of(2021, 2), 1L);
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/absence")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .content("""
                                   {
                                       "employeeStaffLogin": "antipov93",
                                       "reason": {"type": "EMERGENCY"},
                                       "oebsAssignmentId": 480,
                                       "employeePosition": "Кладовщик",
                                       "startDate": "2021-02-12",
                                       "endDate": "2021-02-12",
                                       "working": true
                                   }
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_preproc_after_pres.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "CalendarPreprocessingTest.before.csv", schema = "public")
    @DbUnitDataSet(after = "CalendarPreprocessingTest.after.csv", schema = "public")
    void shouldStoreCalendar() {
        mockClock(LocalDate.of(2021, 2, 14));
        calendarPreprocessingService.syncCalendarItems(YearMonth.of(2021, 2), 1L);
    }
}
