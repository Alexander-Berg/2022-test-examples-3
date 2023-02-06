package ru.yandex.market.hrms.api.controller.calendar_v2.stats;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@DbUnitDataSet(before = {"CalendarControllerEmployeeDayStatsV2Test.schedules.csv",
        "CalendarControllerEmployeeDayStatsV2Test.before.csv"})
public class CalendarControllerEmployeeDayStatsV2Test extends AbstractApiTest {

    @Test
    @DisplayName("Стандартная проверка статистики, все данные полученны и корректны")
    @DbUnitDataSet(before = "EmployeeStatisticWithClickHouseWmsTest.before.csv")
    public void getStats() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "1")
                        .param("domainId", "1")
                        .param("oebsAssignmentId", "187197-3026")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(loadFromFile("CalendarControllerEmployeeStatV2.WithClickHouseWms.json")));
    }

    @Test
    @DisplayName("Проверка статистики, с проверкой роли, линейный менеджер видит только статистику своего домена")
    public void getStatsForLIneManager() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "1")
                        .param("domainId", "1")
                        .param("oebsAssignmentId", "187197-3026")
                        .cookie(new Cookie("yandex_login", "gaga-pol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.LINE_MANAGER).getAuthorities()
                        )))
                .andExpect(content().json(loadFromFile("CalendarControllerEmployeeStatV2V2.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTest.WithAnyTimexLogsProperty.before.csv")
    @DisplayName("Проверка статистики, с опцией любый timex логов. Отображают вход/выход в любую зону (не только опер)")
    public void getStatsWithoutOperatingAreas() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "3")
                        .param("domainId", "41")
                        .param("oebsAssignmentId", "206971-3617")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        loadFromFile("CalendarControllerEmployeeStatV2.WithAnyTimexLogsProperty.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerStatGetStats.WithPresenceAbsence.before.csv")
    @DisplayName("Проверка статистики, с отображением явки поверх отмененной неподтвержденной НН")
    public void getStatsWithPresenceAndAbsence() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "3")
                        .param("domainId", "41")
                        .param("oebsAssignmentId", "206971-3617")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        loadFromFile("CalendarControllerEmployeeStatV2.PresenceAbsence.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerStatGetStats.WithPresenceAbsence.before.csv")
    @DisplayName("Проверка статистики, по RW домену")
    public void getStatsForRw() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "4")
                        .param("domainId", "52")
                        .param("oebsAssignmentId", "206971-3617")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        loadFromFile("CalendarStatsForRW.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerEmployeeDayStatsV2Test.holiday.ordinary.before.csv")
    public void getStatsOnHoliday() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "101")
                        .param("domainId", "41")
                        .param("oebsAssignmentId", "192978-1778")
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(loadFromFile("CalendarControllerEmployeeDayStatsV2Test.holiday.ordinary.json")));
    }

    @Test
    @DbUnitDataSet(before = {
            "CalendarControllerEmployeeDayStatsV2Test.holiday.ordinary.before.csv",
            "CalendarControllerEmployeeDayStatsV2Test.npo-with-overtime.csv"
    })
    public void getStatsOnOvertime() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-12-23")
                        .param("employeeId", "101")
                        .param("domainId", "41")
                        .param("oebsAssignmentId", "192978-1778")
                        .cookie(new Cookie("yandex_login", "any"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shiftStart").value("2021-12-23T17:00:00"))
                .andExpect(jsonPath("$.firstNonProductionOperation").value("2021-12-23T17:00:00"))
                .andExpect(jsonPath("$.lastNonProductionOperation").value("2021-12-24T06:00:00"))
                .andExpect(jsonPath("$.shiftEnd").value("2021-12-24T06:00:00"));
    }
}

