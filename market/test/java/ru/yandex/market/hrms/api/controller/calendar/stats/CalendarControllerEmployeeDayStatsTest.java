package ru.yandex.market.hrms.api.controller.calendar.stats;

import java.time.LocalDate;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class CalendarControllerEmployeeDayStatsTest extends AbstractApiTest {

    @Test
    @Disabled("should rewrite to stats-v2")
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTestGetActionStat.before.csv", schema = "public")
    public void shouldReturnStats() throws Exception {
        performGet(1, LocalDate.of(2021, 7, 3))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("CalendarControllerEmployeeStat.json")));
    }

    @Test
    @Disabled("should rewrite to stats-v2")
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTestGetActionStat.before.csv", schema = "public")
    public void shouldReturnNoStats() throws Exception {
        performGet(1, LocalDate.of(2021, 6, 3))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("CalendarControllerEmployeeStatNoInfo.json")));
    }

    @Test
    @Disabled("should rewrite to stats-v2")
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTestGetActionStat.before.csv", schema = "public")
    public void shouldReturnBadRequestWhenEmployeeNotExists() throws Exception {
        performGet(1111, LocalDate.of(2021, 7, 3))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTestGetStats.before.csv", schema = "public")
    public void getStats() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-09-01")
                        .param("employeeId", "1")
                        .param("position", "Кладовщик")
                        .param("domainId", "1")
                        .param("assignmentId", "101")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(loadFromFile("CalendarControllerEmployeeStatV2.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTestGetStats.before.csv", schema = "public")
    public void getStatsForLIneManager() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-09-01")
                        .param("employeeId", "1")
                        .param("position", "Кладовщик")
                        .param("domainId", "1")
                        .param("assignmentId", "101")
                        .cookie(new Cookie("yandex_login", "dev.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.LINE_MANAGER).getAuthorities()
                )))
                .andExpect(content().json(loadFromFile("CalendarControllerEmployeeStatV2.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerEmployeeStatTestGetStatsWithoutOperatingAreas.before.csv", schema = "public")
    public void getStatsWithoutOperatingAreas() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-09-01")
                        .param("employeeId", "1")
                        .param("position", "Диспетчер")
                        .param("domainId", "1")
                        .param("assignmentId", "101")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(loadFromFile("CalendarControllerEmployeeStatV2WithoutOperatingAreas.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerStatGetStatsWithPresenceAbsence.before.csv", schema = "public")
    public void getStatsWithPresenceAndAbsence() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats-v2")
                        .param("date", "2021-09-01")
                        .param("employeeId", "1")
                        .param("domainId", "1")
                        .param("assignmentId", "101")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(loadFromFile("CalendarStatsWithPresenceAbsence.json")));
    }

    @Deprecated(forRemoval = true)
    private ResultActions performGet(long employeeId, LocalDate date) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/stats")
                .param("date", date.toString())
                .param("employeeId", Long.toString(employeeId))
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
        );
    }
}

