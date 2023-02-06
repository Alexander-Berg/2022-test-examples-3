package ru.yandex.market.hrms.api.controller.calendar_v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"environment.csv", "schedules.csv", "CalendarControllerPage.before.csv"})
public class CalendarControllerV2PageTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.page.before.csv")
    @DisplayName("Выводим календарь за декабрь с учетом расписания и полученных hr_details")
    void shouldGetCalendarPage() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_december.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.page_changed_assignment.before.csv")
    @DisplayName("Выводим календарь за декабрь с преломлениями в расписаниях и сменой назначений в рамках домена")
    void shouldGetCalendarPageForEmployeeWithDifferentSchedulesAndAssignments() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_december_changed_assignment.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.page_transferred.before.csv")
    @DisplayName("Выводим календарь за декабрь с переводами между доменами")
    void shouldGetCalendarPageForEmployeeTransferredAmongMonth() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_december_assignment_transferred.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.page_transferred.before.csv")
    @DisplayName("Выводим календарь за декабрь с переводами между доменами")
    void shouldGetCalendarPageForEmployeeTransferredAmongMonthSecondDomain() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 15, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_december_assignment_transferred_second.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.assignments.before.csv")
    void shouldGetCalendarWithSearch() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 15, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeeName", "Поли Га")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_search_name.json")));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.assignments.before.csv")
    void shouldGetCalendarWithGroupId() throws Exception {
        mockClock(LocalDate.of(2021, 12, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("groupId", "12")
                        .cookie(new Cookie("yandex_login", "catboss"))
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_by_group.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv", "show_gap_vacations.before.csv",
            "CalendarControllerPage.gaps.before.csv"})
    void shouldShowGapVacations() throws Exception {
        mockClock(LocalDate.of(2021, 12, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("gap_vacation_page.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv", "show_oebs_vacations.before.csv",
            "CalendarControllerPage.gaps.before.csv"})
    void shouldShowOebsVacations() throws Exception {
        mockClock(LocalDate.of(2021, 12, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("oebs_vacation_page.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv", "wms_user_logins.csv"})
    @DisplayName("ФФЦ. Отображать розовые НН у кладовщиков в течение смены, если нет логов wms")
    void shouldShowPinkNNDuringShiftFfc() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 22, 11, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("pink_absence_page_when_not_wms_logs.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv", "wms_user_logins.csv"})
    @DisplayName("ФФЦ. Отображать розовые НН у кладовщиков в течение смены, если нет логов wms")
    void shouldShowPinkNNDuringShiftFfcInSamara() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 22, 10, 1, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "37")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("pink_absence_page_when_not_wms_logs_in_samara.json"), false));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPage.assignments.before.csv")
    @DisplayName("ФФЦ. Не отображать розовые НН у кладовщиков, если не задан wms-логин")
    void shouldNotShowPinkNNWithoutLoginFfc() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 22, 12, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("not_pink_absence_when_wms_logins_not_exists.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv", "wms_user_logins.csv"})
    @DisplayName("ФФЦ. Отображать розовые НН у кладовщиков в течение 30 минут после окончания смены")
    void shouldShowPinkNNDuring30MinutesFfc() throws Exception {
        // смена закончилась в 19:00
        mockClock(LocalDateTime.of(2021, 12, 22, 19, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("pink_absence_page_when_not_wms_logs.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv",
            "wms_user_logins.csv", "employee_absence.csv"})
    @DisplayName("ФФЦ. Отображать желтые НН у кладовщиков если смена закончена 30 минут назад, но нет логов")
    void shouldShowYellowNNAfter30MinutesFfc() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 22, 19, 31, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("yellow_absence_page_after_shift_ends.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.not_primary_primary.before.csv"})
    @DisplayName("Не основное назначение превращается в основное среди месяца")
    void notPrimaryAssignmentBecamePrimaryOne() throws Exception {
        mockClock(LocalDateTime.of(2021, 12, 22, 19, 31, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerPage.assignments.before.csv", "wms_user_logins.csv",
            "CannotChangeAbsencePresence.csv"})
    @DisplayName("")
    void cannotChangeAbsenceStateForSpecificPositions() throws Exception {
        // смена закончилась в 19:00
        mockClock(LocalDateTime.of(2021, 12, 22, 19, 30, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("disable_absence_corrections_by_positions.json")));
    }

    @Test
    @DbUnitDataSet(before = {"CalendarControllerV2PageTest.different_months_in_oebs_employee_info.csv"})
    @DisplayName("Показывать должность из oebs_employee_info в табеле")
    void shouldShowPositionFromOebsEmployeeInfo() throws Exception {
        mockClock(LocalDateTime.of(2022, 1, 1, 10, 0));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-12")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                             HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(
                       loadFromFile("CalendarControllerV2PageTest.shouldShowPositionFromOebsEmployeeInfo.after.json")));
    }

}
