package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "CalendarControllerPage.environment.csv")
public class CalendarControllerPageTest extends AbstractApiTest {
    @BeforeEach
    public void setUpThis() {
        mockClock(LocalDate.of(2021, 1, 29));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.redNNPriority.before.csv")
    void redNNPriority() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_february_unclosed_sof.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DisplayName("Февраль. Отображать в табеле Софьино за февраль сотрудника, переведенного в Томилино с марта")
    void shouldGetCalendarPageCurrentMonth() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_february_unclosed_sof.json")));
    }


    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DisplayName("Март. Отображать в табеле Софьино за февраль сотрудника, переведенного в Томилино с марта")
    void shouldGetCalendarPagePrevMonthPrevDomain() throws Exception {
        mockClock(LocalDateTime.of(2021, 3, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_february_closed_sof.json")));
    }


    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DisplayName("Март. Не отображать в табеле Софьино за март сотрудника, переведенного в Томилино с марта")
    void shouldGetCalendarPagePrevDomain() throws Exception {
        mockClock(LocalDateTime.of(2021, 3, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-03")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_sof.json"), false));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DisplayName("Февраль. Не отображать в табеле Томилино за февраль сотрудника, переведенного в Томилино с марта")
    void shouldGetCalendarPageCurrentMonthNextDomain() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_february_unclosed_tml.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DisplayName("Март. Не отображать в табеле Томилино за февраль сотрудника, переведенного в Томилино с марта")
    void shouldGetCalendarPagePrevMonthNewDomain() throws Exception {
        mockClock(LocalDateTime.of(2021, 3, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_february_closed_tml.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "CalendarControllerPageTest.before.csv",
            "CalendarControllerPageTest.before.march.tml.csv",
    })
    @DisplayName("Март. Отображать в табеле Томилино за март сотрудника, переведенного в Томилино с марта")
    void shouldGetCalendarPageCurrentMonthNewDomain() throws Exception {
        mockClock(LocalDateTime.of(2021, 3, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-03")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_tml.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "2021-02-18.before.csv")
    void shouldGetCalendarPageTooEarly() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 18, 9, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-18.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "2021-02-19.before.csv")
    void shouldGetCalendarPageHasScanAndHasNotActionsEarly() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 19, 11, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-19.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "2021-02-22.before.csv")
    void shouldGetCalendarPageHasScanAndHasNotActionsNotEarly() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 22, 13, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "2021-02-22-evening.before.csv")
    void shouldGetCalendarPageWorkShiftEnded() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 22, 18, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22-evening.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "2021-02-26.before.csv")
    void shouldGetCalendarWithPresence() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 26, 11, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-26.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "2021-02-28.before.csv")
    void shouldGetCalendarPageWeekend() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 28, 13, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-28.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldGetCalendarWithSearch() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 18, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeeName", " Андр   Антипо  ")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_query.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldGetCalendarWithPosition() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeePosition", "Брига")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_position.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldGetCalendarWithPositionWithSpaces() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeePosition", "   Начальник склада  ")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_position_spaces.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldGetCalendarWithGroupId() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("groupId", "2")
                        .cookie(new Cookie("yandex_login", "timursha"))
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_group.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldGetEmptyCalendar() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeePosition", "Бригадирка")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("shouldGetEmptyCalendar.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldFailOnInvalidInput() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeePosition", "%%")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldHideFired() throws Exception {
        mockClock(LocalDate.of(2021, 3, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("showFired", "false")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_hide_fired.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldShowFired() throws Exception {
        mockClock(LocalDate.of(2021, 3, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("showFired", "true")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_show_fired.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "CalendarControllerLost.before.csv")
    void shouldShowFiredNotLost() throws Exception {
        mockClock(LocalDate.of(2021, 3, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("showFired", "true")
                        .queryParam("showLost", "false")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_show_fired_not_lost.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "CalendarControllerLost.before.csv"})
    void shouldShowLost() throws Exception {
        mockClock(LocalDate.of(2021, 3, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("showLost", "true")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_show_lost.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "CalendarControllerLost.before.csv"})
    void shouldHideLost() throws Exception {
        mockClock(LocalDate.of(2021, 3, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("showLost", "false")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_march_hide_lost.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "show_group.before.csv")
    void shouldShowGroups() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("groupId", "2")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("show_group.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "show_group_abc.before.csv", "show_group.before.csv"})
    void shouldShowGroupsInAlphabetOrder() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("groupId", "2")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                        .cookie(new Cookie("yandex_login", "kukabara"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("show_group_abc.json"), true));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "show_gap_vacations.before.csv")
    void shouldShowGapVacations() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("gap_vacation_page.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "show_oebs_vacations.before.csv")
    void shouldShowOebsVacations() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_february_unclosed_sof.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    @DbUnitDataSet(schema = "public", before = "CalendarControllerTestGapAssignments.before.csv")
    void shouldShowGapOnlyOnActiveAssignment() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_gap_assignments.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv"})
    @DisplayName("ФФЦ. Отображать розовые НН у кладовщиков в течение смены, если нет логов wms")
    void shouldShowPinkNNDuringShiftFfc() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 22, 12, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.ffc.pink-nn.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "NNTest.employee_without_wms.csv"})
    @DisplayName("ФФЦ. Не отображать розовые НН у кладовщиков, если не задан wms-логин")
    void shouldNotShowPinkNNWithoutLoginFfc() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 22, 12, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.ffc.not-show-pink-nn.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "NNTest.employee_with_wms.csv"})
    @DisplayName("ФФЦ. Отображать розовые НН у кладовщиков в течение 30 минут после окончания смены")
    void shouldShowPinkNNDuring30MinutesFfc() throws Exception {
        // смена закончилась в 18:00
        mockClock(LocalDateTime.of(2021, 2, 22, 18, 20, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.ffc.show-pink-nn-30min.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv",
                    "NNTest.employee_with_wms.csv", "NNTest.employee_absence.csv"})
    @DisplayName("ФФЦ. Отображать желтые НН у кладовщиков если смена закончена 30 минут назад, но нет логов")
    void shouldShowYellowNNAfter30MinutesFfc() throws Exception {
        // смена закончилась в 18:00
        mockClock(LocalDateTime.of(2021, 2, 22, 18, 40, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "2")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.ffc.show-yellow-nn-30min.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "NNTest.employee_with_wms.csv"})
    @DisplayName("СЦ. Отображать розовые НН у кладовщиков в течение смены, если нет логов wms")
    void shouldShowPinkNNDuringShiftSc() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 22, 12, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "3")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.sc.pink-nn.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "NNTest.employee_without_wms.csv"})
    @DisplayName("СЦ. Не отображать розовые НН у кладовщиков, если не задан sc-логин")
    void shouldNotShowPinkNNWithoutLoginSc() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 22, 12, 45, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "3")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.sc.not-show-pink-nn.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv", "NNTest.employee_with_wms.csv"})
    @DisplayName("СЦ. Отображать розовые НН у кладовщиков в течение 30 минут после окончания смены")
    void shouldShowPinkNNDuring30MinutesSc() throws Exception {
        // смена закончилась в 18:00
        mockClock(LocalDateTime.of(2021, 2, 22, 18, 20, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "3")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.sc.pink-nn.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = {"CalendarControllerPageTest.before.csv",
                    "NNTest.employee_with_wms.csv", "NNTest.employee_absence.csv"})
    @DisplayName("СЦ. Отображать желтые НН у кладовщиков если смена закончена 30 минут назад, но нет логов")
    void shouldShowYellowNNAfter30MinutesSc() throws Exception {
        // смена закончилась в 18:00
        mockClock(LocalDateTime.of(2021, 2, 22, 18, 40, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2021-02")
                        .param("domainId", "3")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("2021-02-22.sc.show-yellow-nn.result.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "CalendarControllerPageTest.before.csv")
    void shouldGetCalendarWithSearchByScLogin() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 18, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("employeeName", " andr ")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_query.json")));
    }
}
