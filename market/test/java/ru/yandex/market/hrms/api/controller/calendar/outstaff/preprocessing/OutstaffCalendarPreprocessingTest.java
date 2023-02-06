package ru.yandex.market.hrms.api.controller.calendar.outstaff.preprocessing;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "OutStaffCalendarControllerPreprocTest.before.csv")
public class OutstaffCalendarPreprocessingTest extends AbstractApiTest {

    @Test
    @DisplayName("Получить рассчитанную страницу с аутами")
    void shouldGetOutStaffCalendarPage() throws Exception {
        mockClock(LocalDateTime.of(2021, 8, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-08")
                        .param("activityFilter", "SHOW_ALL")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("all_outstaff_page.json"), false));
    }

    @Test
    @DisplayName("Получить рассчитанную страницу с активными аутами")
    void shouldGetActiveOutStaffCalendarPage() throws Exception {
        mockClock(LocalDateTime.of(2021, 8, 10, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff-calendar")
                        .param("date", "2021-08")
                        .param("activityFilter", "SHOW_ONLY_ACTIVE")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("preproc_active_outstaff_page.json"), false));
    }
}
