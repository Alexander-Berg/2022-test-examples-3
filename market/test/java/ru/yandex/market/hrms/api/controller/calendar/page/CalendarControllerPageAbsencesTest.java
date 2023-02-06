package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CalendarControllerPageAbsencesTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageAbsencesTest.before.csv", schema = "public")
    void shouldShowAbsencesInValidStatusesFFc() throws Exception {
        mockClock(LocalDate.of(2021, 2, 14));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_absences.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerPageAbsencesTestSc.before.csv", schema = "public")
    void shouldShowAbsencesInValidStatusesSc() throws Exception {
        mockClock(LocalDate.of(2021, 2, 14));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("page_absences_sc.json"), true));
    }
}
