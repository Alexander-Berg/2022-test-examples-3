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

@DbUnitDataSet(before = {"CalendarPreprocessingOebsTest.schedules.csv", "CalendarPreprocessingOebsTest.before.csv"})
public class CalendarPreprocessingWithOebsTest extends AbstractApiTest {

    @Autowired
    private CalendarPreprocessingService calendarPreprocessingService;

    @Test
    void shouldReturnPreprocessedCalendar() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 13, 12, 0, 0));
        calendarPreprocessingService.syncCalendarItems(YearMonth.of(2021, 12), 1L);
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_preprocessing_oebs.json"), true));
    }

    @Test
    void shouldRemovePinkAbsenceAfterManualPresence() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 13, 15, 0, 0));
        calendarPreprocessingService.syncCalendarItems(YearMonth.of(2021, 12), 1L);
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/calendar/absence")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .content("""
                                   {
                                       "employeeStaffLogin": "gaga-pol",
                                       "reason": {"type": "EMERGENCY"},
                                       "oebsAssignmentId": "187197-3026",
                                       "employeePosition": "Кладовщик",
                                       "startDate": "2021-12-13",
                                       "endDate": "2021-12-13",
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
                .andExpect(content().json(loadFromFile("page_preprocessing_oebs_presence.json"), true));
    }

    @Test
    @DbUnitDataSet(after = "CalendarPreprocessingOebsTest.after.csv")
    void shouldStoreCalendar() {
        mockClock(LocalDate.of(2021, 12, 13));
        calendarPreprocessingService.syncCalendarItems(YearMonth.of(2021, 12), 1L);
    }
}
