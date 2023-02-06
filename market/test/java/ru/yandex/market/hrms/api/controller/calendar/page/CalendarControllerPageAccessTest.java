package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDate;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "CalendarControllerPageAccessTest.before.csv")
public class CalendarControllerPageAccessTest extends AbstractApiTest {

    @Test
    void getPageForOperationManager() throws Exception {
        mockClock(LocalDate.of(2021, 8, 16));
        mockMvc.perform(get("/lms/calendar")
                .queryParam("groupId", "4")
                .queryParam("domainId", "1")
                .cookie(new Cookie("yandex_login", "dev.1"))
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        loadFromFile("CalendarControllerPageAccessTest.GetPageForOperationManager.after.json")));
    }

    @Test
    void getPageForUnknownUser() throws Exception {
        mockClock(LocalDate.of(2021, 8, 16));
        mockMvc.perform(get("/lms/calendar")
                        .queryParam("groupId", "4")
                        .queryParam("domainId", "1")

                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getPageForLineManagerNoAccess() throws Exception {
        mockClock(LocalDate.of(2021, 8, 16));
        mockMvc.perform(get("/lms/calendar")
                .queryParam("groupId", "8")
                .queryParam("domainId", "1")
                .cookie(new Cookie("yandex_login", "login3"))
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.LINE_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void getPageForLineManager() throws Exception {
        mockClock(LocalDate.of(2021, 8, 16));
        mockMvc.perform(get("/lms/calendar")
                .queryParam("groupId", "4")
                .queryParam("domainId", "1")
                .cookie(new Cookie("yandex_login", "login3"))
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.LINE_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        loadFromFile("CalendarControllerPageAccessTest.GetPageForLineManager.after.json")));
    }
}
