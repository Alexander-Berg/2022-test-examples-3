package ru.yandex.market.hrms.api.controller.calendar.page.overtimes;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitDataSets;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSets({
        @DbUnitDataSet(before= "CalendarControllerPageTest.common.csv"),
        @DbUnitDataSet(before = "CalendarControllerPageTest.employees.csv")
})
public class CalendarControllerPageWithOvertimesTest extends AbstractApiTest {
    private static final String CALENDAR_CELL_TYPE_JSON_PATH = "$.items[?(@.employee.id==1)].cells[14].type";


    @Test
    public void initialCalendar() throws Exception {
        mockClock(Instant.parse("2021-12-15T12:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("base.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.approvalRequired.csv")
    public void showCreatedWithRequiredApprovalOvertime() throws Exception {
        mockClock(Instant.parse("2021-12-15T06:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("410"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.approved.csv")
    public void showApprovedOvertime() throws Exception {
        mockClock(Instant.parse("2021-12-15T06:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("411"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.before.csv")
    public void showCreatedOvertime() throws Exception {
        mockClock(Instant.parse("2021-12-15T06:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("411"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.withStats.before.csv")
    public void showStartedOvertimesWithActivities() throws Exception {
        mockClock(Instant.parse("2021-12-15T12:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("41"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.noStats.before.csv")
    public void showStartedOvertimesWithNoActivities() throws Exception {
        mockClock(Instant.parse("2021-12-15T12:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("415"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.rejected.before.csv")
    public void doNotShowRejected() throws Exception {
        mockClock(Instant.parse("2021-12-15T22:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("26"));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageTest.overtime.deleted.before.csv")
    public void doNotShowDeleted() throws Exception {
        mockClock(Instant.parse("2021-12-15T22:00:00+03:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.EXTENDED_VIEWER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath(CALENDAR_CELL_TYPE_JSON_PATH).value("26"));
    }
}
