package ru.yandex.market.hrms.api.controller.calendar.outstaff;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.before.csv")
public class OutStaffCalendarControllerTest extends AbstractApiTest {

    @BeforeEach
    public void setUpThis() {
        mockClock(LocalDate.of(2021, 9, 3));
    }

    @Test
    @DisplayName("Получить страницу с аутами")
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldGetOutStaffCalendarPage() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_page.json"), false));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldGetDomainNotSetError() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().json(loadFromFile("domain_not_set.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {"OutStaffCalendarControllerTest.timex.csv"})
    void shouldGetOnlyActiveOutStaff() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 9, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("activityFilter", "SHOW_ONLY_ACTIVE")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("active_outstaff_page.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.currentTimex.csv")
    void shouldShowOutStaffOnWorkingDate() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-08")
                        .param("showOnlyActive", "true")
                        .param("domainId", "1")
                        .param("workingDate", "2021-08-01")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("working_date_20210801.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.currentTimex.csv")
    void shouldFailWorkingDateNotSet() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-08")
                        .param("showOnlyActive", "true")
                        .param("domainId", "1")
                        .param("shiftIds", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().json(loadFromFile("working_date_not_set.json")));
    }


    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.currentTimex.csv")
    void shouldShowCurrentShiftsForActive() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 1, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("activityFilter", "SHOW_ONLY_ACTIVE")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("current_shift_active_outstaff_page.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldShowOnlyBiometryShiftForMonth() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 10, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("activityFilter", "SHOW_ONLY_BIOMETRY")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("only_biometry_outstaff_page.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldShowOnlyBiometryShiftForDate() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 10, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("activityFilter", "SHOW_ONLY_BIOMETRY")
                        .param("domainId", "1")
                        .param("workingDate", "2021-09-01")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("only_biometry_outstaff_page_20210901.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldShowActivitiesWithoutBiometry() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 10, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("activityFilter", "SHOW_ACTIVITIES_WITHOUT_BIOMETRY")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("activity_without_biometry_page.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.absence.csv")
    void shouldShowAbsences() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 10, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("domainId", "1")
                        .queryParam("outstaffName", "user-100")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_absence.json")));
    }

    @Test
    void getDomainShiftsTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/shifts")
                        .param("domainId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("domain1_shifts.json")));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/shifts")
                        .param("domainId", "3")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("domain3_shifts.json")));

    }

    @Test
    void getDomainActivityFilters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/activity-filters")
                        .param("domainId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("domain1_activity_filters.json")));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/activity-filters")
                        .param("domainId", "3")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("domain3_activity_filters.json")));

    }

    @Test
    void shouldFailDomainNotSetTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/shifts")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().json(loadFromFile("domain_not_set.json"), false));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldShowOnlyTimexStat() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/stats")
                        .param("date", "2021-09-03")
                        .param("domainId", "1")
                        .param("outstaffId", "107")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_timex_only_stat.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldShowAllStatTimexOnlyEnter() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/stats")
                        .param("date", "2021-08-01")
                        .param("domainId", "1")
                        .param("outstaffId", "100")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_timex_only_enter.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldShowStatNowInOperZone() throws Exception {
        mockClock(LocalDateTime.of(2021, 8, 1, 12, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/stats")
                        .param("date", "2021-08-01")
                        .param("domainId", "1")
                        .param("outstaffId", "100")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_timex_only_enter.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.sc.csv")
    void shouldShowOutstaffScInfo() throws Exception {
        mockClock(LocalDateTime.of(2021, 10, 5, 12, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/stats")
                        .param("date", "2021-10-03")
                        .param("domainId", "43")
                        .param("outstaffId", "115")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_sc_info.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.sc.csv")
    void shouldShowOutstaffScInfoWithScLogs() throws Exception {
        mockClock(LocalDateTime.of(2021, 10, 5, 12, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar/stats")
                        .param("date", "2021-10-03")
                        .param("domainId", "52")
                        .param("outstaffId", "116")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("sc_action_stat.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.currentTimex.csv")
    void shouldSearchOutstaffByScLogin() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 1, 10, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .queryParam("outstaffName", "andy")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].outstaff.scLogin")
                        .value("andy101@hrms-sc.ru"));
    }

    @Test
    void shouldGetOneCompanyOutstaff() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("outstaffCompanyId", "326611129")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_page_one_company.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldGetTwoCompaniesOutstaff() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("outstaffCompanyId", "326611127")
                        .param("outstaffCompanyId", "326611129")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_page.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerTest.timex.csv")
    void shouldGetFirstShiftOnDate() throws Exception {
        mockClock(LocalDateTime.of(2021, 9, 3, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-09")
                        .param("workingDate", "2021-09-02")
                        .param("shiftTypes", "SECOND_SHIFT")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("outstaff_page_shift_filter.json")));
    }
}
