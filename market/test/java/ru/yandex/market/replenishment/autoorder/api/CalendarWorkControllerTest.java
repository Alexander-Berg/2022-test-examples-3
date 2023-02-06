package ru.yandex.market.replenishment.autoorder.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WithMockLogin
public class CalendarWorkControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv")
    public void testGetCalendarYear() throws Exception {
        mockMvc.perform(get("/api/v2/calendar?year=2022"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(122));
    }

    @Test
    public void testSaveCalendar() throws Exception {
        mockMvc.perform(post("/api/v1/calendars")
                .content("{\"name\": \"test calendar\"}")
                .contentType(APPLICATION_JSON_UTF8)
            )
            .andExpect(status().isOk())
            .andExpect(mvcResult -> log.debug("response - {}", mvcResult.getResponse().getContentAsString()))
            .andExpect(content().json("{\"id\":1,\"name\":\"test calendar\"}"));

        mockMvc.perform(post("/api/v1/calendars")
                .content("{\"name\": \"test calendar #2\"}")
                .contentType(APPLICATION_JSON_UTF8)
            )
            .andExpect(status().isOk())
            .andExpect(mvcResult -> log.debug("response - {}", mvcResult.getResponse().getContentAsString()))
            .andExpect(content().json("{\"id\":2,\"name\":\"test calendar #2\"} "));
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest_testGetCalendars.csv")
    public void testGetCalendars() throws Exception {
        String response = super.readFile("CalendarWorkControllerTest_testGetCalendars.json");
        mockMvc.perform(get("/api/v1/calendars"))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> log.debug("response - {}", mvcResult.getResponse().getContentAsString()))
            .andExpect(content().json(response));
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv")
    public void testGetPrevCalendarYear() throws Exception {
        mockMvc.perform(get("/api/v2/calendar?year=2021"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(112));
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv", after = "CalendarWorkControllerTest.addEvent" +
        ".after.csv")
    public void testAddEvent() throws Exception {
        String content = "{ \"from\": \"2022-03-10\", \"calendarWorkId\": 2, " +
            "\"repetition\": {\"each\": 3, \"type\":\"DAILY\"} }";
        mockMvc.perform(put("/api/v2/calendar")
            .contentType(APPLICATION_JSON_UTF8)
            .content(content)
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv")
    public void testGetCalendarDate() throws Exception {
        mockMvc.perform(get("/api/v2/calendar/date?date=2021-02-01&supplier=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(1))
            .andExpect(jsonPath("$.excludes.length()").value(0));
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv",
        after = "CalendarWorkControllerTest.updateEvent.after.csv"
    )
    public void testUpdateCalendarEvent() throws Exception {
        String content = "{ \"from\": \"2022-02-01\", \"repetition\": { \"each\": 2, \"type\": \"MONTHLY_NUMBER\" } }";

        mockMvc.perform(post("/api/v2/calendar/101")
                .contentType(APPLICATION_JSON_UTF8)
                .content(content)
            )
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv",
        after = "CalendarWorkControllerTest.deleteEvent.after.csv")
    public void testDeleteEvent() throws Exception {
        mockMvc.perform(delete("/api/v2/calendar/101")
            .contentType(APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv",
        after = "CalendarWorkControllerTest.addExclude.after.csv")
    public void testAddExclude() throws Exception {
        mockMvc.perform(put("/api/v2/calendar/exclude/1?date=2022-03-12&working=false")
            .contentType(APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkControllerTest.before.csv",
        after = "CalendarWorkControllerTest.deleteExclude.after.csv")
    public void testDeleteExclude() throws Exception {
        mockMvc.perform(delete("/api/v2/calendar/exclude/1?date=2022-03-02")
            .contentType(APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());
    }
}
