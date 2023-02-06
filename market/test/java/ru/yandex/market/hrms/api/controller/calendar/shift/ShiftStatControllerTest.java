package ru.yandex.market.hrms.api.controller.calendar.shift;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "ShiftStatControllerTest.before.csv")
class ShiftStatControllerTest extends AbstractApiTest {

    @Test
    void currentShiftStat() throws Exception {
        mockClock(LocalDateTime.of(2021, 4, 4, 12, 0));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar/currentShift/stat")
                .header("X-Admin-Roles", String.join(",",
                        HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("currentShiftStat.response.json"), true));
    }

}
