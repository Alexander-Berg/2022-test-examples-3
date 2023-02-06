package ru.yandex.market.hrms.api.external.agent;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.starter.tvm.factory.TvmClientSettings.LOCAL_TVM_ID;

@TestPropertySource(properties = {
        "mj.tvm.checkServiceTicket=true",
        "mj.tvm.checkUserTicket=true"
})
@DbUnitDataSet(before = "HrmsAgentApiTest.common.before.csv")
class HrmsAgentCalendarApiTest extends AbstractApiTest {

    private final static String SERVICE_TICKET = "3:serv:TEST";
    private final static String USER_TICKET = "3:user:TEST";
    private final static String USER_LOGIN = "ivanov";
    private final static long USER_UID = -1;

    private final static String DATE = "2022-04-01";
    private final static String DATE_FROM = "2022-04-02";
    private final static String DATE_TO = "2022-04-05";

    @MockBean
    private TvmClient tvmClient;

    @BeforeEach
    public void initTvm() {
        when(tvmClient.checkServiceTicket(SERVICE_TICKET))
                .thenReturn(new CheckedServiceTicket(TicketStatus.OK, null, LOCAL_TVM_ID, USER_UID));

        when(tvmClient.checkUserTicket(USER_TICKET))
                .thenReturn(new CheckedUserTicket(TicketStatus.OK, null, null, USER_UID, null));
    }

    @Test
    @DbUnitDataSet(before = {
            "HrmsAgentApiTest.calendarData.csv",
            "HrmsAgentCalendarApiTest.happyPath.before.csv"
    })
    public void happyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/events")
                        .queryParam("start", DATE_FROM)
                        .queryParam("end", DATE_TO)
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("json/calendar.happyPath.json"), true));
    }

    @Test
    @DbUnitDataSet(before = {
            "HrmsAgentApiTest.calendarData.csv",
            "HrmsAgentCalendarApiTest.happyPath.before.csv"
    })
    public void viewOtherLogin() throws Exception {
        when(tvmClient.checkServiceTicket(SERVICE_TICKET))
                .thenReturn(new CheckedServiceTicket(TicketStatus.OK, null, LOCAL_TVM_ID, 1001));

        when(tvmClient.checkUserTicket(USER_TICKET))
                .thenReturn(new CheckedUserTicket(TicketStatus.OK, null, null, 1001, null));

        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/events")
                        .queryParam("start", DATE_FROM)
                        .queryParam("end", DATE_TO)
                        .queryParam("viewedLogin", USER_LOGIN)
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("json/calendar.happyPath.json"), true));

    }

    @Test
    @DbUnitDataSet(before = {
            "HrmsAgentCalendarApiTest.happyPath.before.csv",
            "HrmsAgentCalendarApiTest.absence.yellow.csv"
    })
    public void yellowAbsence() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/events")
                        .queryParam("start", "2022-04-05")
                        .queryParam("end", "2022-04-05")
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                            "date": "2022-04-05",
                            "category": "UNSPECIFIED",
                            "events": [{
                                "code": "НН",
                                "isPrimary": true,
                                "title": "",
                                "description": "Неявки по невыясненным причинам (до выяснения обстоятельств) - драфт"
                            }]
                        }]
                        """, true));
    }

    @Test
    @DbUnitDataSet(before = {
            "HrmsAgentCalendarApiTest.happyPath.before.csv",
            "HrmsAgentCalendarApiTest.absence.pink.csv"
    })
    public void pinkAbsence() throws Exception {
        mockClock(Instant.parse("2022-04-05T12:00:00+04:00"));

        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/events")
                        .queryParam("start", "2022-04-05")
                        .queryParam("end", "2022-04-05")
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                            "date": "2022-04-05",
                            "category": "UNSPECIFIED",
                            "events": [{
                                "code": "НН",
                                "isPrimary": true,
                                "title": "",
                                "description": "Временная неявка на период смены",
                                "timeBound": {
                                    "start": "2022-04-05T10:00:00+04:00",
                                    "end": "2022-04-05T19:00:00+04:00"
                                }
                            }]
                        }]
                        """, true));
    }


    @Test
    public void empty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/events")
                        .queryParam("start", DATE_FROM)
                        .queryParam("end", DATE_TO)
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
    }

    @Test
    @DbUnitDataSet(before = {
            "HrmsAgentApiTest.calendarData.csv",
            "HrmsAgentCalendarApiTest.happyPath.before.csv"
    })
    public void categories() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/categories")
                        .queryParam("start", DATE_FROM)
                        .queryParam("end", DATE_TO)
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                            {"date": "2022-04-02", "categories": ["HOLIDAY"]},
                            {"date": "2022-04-03", "categories": ["HOLIDAY"]},
                            {"date": "2022-04-04", "categories": ["WORK"]},
                            {"date": "2022-04-05", "categories": ["WORK"]}
                        ]
                        """, true));
    }

    @Test
    @DbUnitDataSet(before = {
            "HrmsAgentCalendarApiTest.happyPath.before.csv",
            "HrmsAgentCalendarApiTest.stats.csv",
    })
    public void shouldReturnMappedStatistics() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/calendar/stats")
                        .queryParam("date", DATE)
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "firstEnterToWarehouse": "2022-04-01T11:08:11+04:00",
                            "lastExitFromWarehouse": "2022-04-01T11:25:55+04:00",
                            "firstWmsAction": "2022-04-01T09:37:39+04:00",
                            "lastWmsAction": "2022-04-01T18:19:52+04:00",
                            "firstNpoStart": "2022-04-01T12:00:00+04:00",
                            "lastNpoEnd": "2022-04-01T18:00:00+04:00",
                            "manualEvent": "UNDEFINED"
                        }
                        """, true));
    }
}