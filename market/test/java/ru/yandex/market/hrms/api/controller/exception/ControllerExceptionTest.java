package ru.yandex.market.hrms.api.controller.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ControllerExceptionTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "../calendar_v2/CalendarControllerPage.before.csv")
    // можно писать и так: classpath:ru/yandex/market/hrms/api/controller/calendar_v2/CalendarControllerPage.before.csv
    void shouldHandleRuntimeException() throws Exception {
        RequestContextHolder.setContext(new RequestContext("12/34"));

        mockMvc.perform(MockMvcRequestBuilders.get("/lms/calendar")
                        .param("date", "2022-06")
                        .param("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.VIEWER).getAuthorities()
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("""
                    {
                        "status": 400,
                        "error": "BAD_REQUEST",
                        "message": "не найден пользователь с логином unknown_user",
                        "tsum_link": "https://tsum.yandex-team.ru/trace/12/34"
                    }
                """));
    }
}
