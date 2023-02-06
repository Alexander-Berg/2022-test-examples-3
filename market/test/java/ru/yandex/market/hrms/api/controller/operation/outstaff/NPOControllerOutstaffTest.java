package ru.yandex.market.hrms.api.controller.operation.outstaff;

import java.time.Instant;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "NPOControllerOutstaffTest.outstaff.csv")
public class NPOControllerOutstaffTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(schema = "public", after = "NPOControllerOutstaffTest.shouldCreateFor2Outs.after.csv")
    void shouldCreateFor2Outs() throws Exception {
        mockClock(Instant.parse("2022-07-11T10:00:00Z"));
        mockMvc.perform(createRequest("master", 1L, """
                {
                    "startDateTime": "2022-07-11T10:00:00",
                    "endDateTime": "2022-07-11T12:00:00",
                    "isFullShift": false,
                    "outstaffIds": [1, 2]
                }
                """))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "NPOControllerOutstaffTest.shouldCreateFor2Outs.after.csv")
    void shouldFailToCreateWhenOverlapping() throws Exception {
        mockClock(Instant.parse("2022-07-11T10:00:00Z"));
        mockMvc.perform(createRequest("master", 1L, """
                {
                    "startDateTime": "2022-07-11T11:00:00",
                    "endDateTime": "2022-07-11T13:00:00",
                    "isFullShift": false,
                    "outstaffIds": [1]
                }
                """))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Для Outstaff[1] уже существует НПО в запрошенном временном интервале"));
    }

    private MockHttpServletRequestBuilder createRequest(String by, long domainId, String body) {
        return MockMvcRequestBuilders.post("/lms/non-production-operations")
                .cookie(new Cookie("yandex_login", by))
                .param("domainId", String.valueOf(domainId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
