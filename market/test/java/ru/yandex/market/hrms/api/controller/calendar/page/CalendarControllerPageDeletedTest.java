package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "CalendarControllerPageDeletedTest.before.csv")
public class CalendarControllerPageDeletedTest extends AbstractApiTest {

    @Test
    void shouldGetEmptyCalendarIfDeleted() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .param("date", "2021-02")
                .param("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("empty.json"), true));
    }
}
