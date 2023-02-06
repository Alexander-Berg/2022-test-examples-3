package ru.yandex.market.hrms.api.controller.calendar.page;

import java.time.LocalDate;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "CalendarControllerPagePaginationTest.before.csv", schema = "public")
public class CalendarControllerPagePaginationTest extends AbstractApiTest {

    @BeforeEach
    public void initClock() {
        mockClock(LocalDate.of(2021, 2, 15));
    }

    @Test
    public void shouldReturnFullGroupIfItsSizeIsBelowThreshold() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("size", "3")
                .queryParam("groupId", "2")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.size()").value(CoreMatchers.is(3)))
                .andExpect(jsonPath("$.pageInfo.totalPage").value(CoreMatchers.is(2)))
                .andExpect(jsonPath("$.pageInfo.totalItems").value(CoreMatchers.is(5)));
    }

    @Test
    public void shouldNotReturnFullGroupIfItsSizeIsAboveThreshold() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                .queryParam("size", "3")
                .queryParam("groupId", "1")
                .queryParam("domainId", "1")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.size()").value(CoreMatchers.is(3)))
                .andExpect(jsonPath("$.pageInfo.totalPage").value(CoreMatchers.is(4)))
                .andExpect(jsonPath("$.pageInfo.totalItems").value(CoreMatchers.is(11)));
    }

}
