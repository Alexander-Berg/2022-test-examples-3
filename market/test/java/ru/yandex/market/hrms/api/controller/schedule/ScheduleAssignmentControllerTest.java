package ru.yandex.market.hrms.api.controller.schedule;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.api.facade.schedule.ScheduleAssignmentTicketData;
import ru.yandex.market.hrms.api.facade.schedule.ScheduleAssignmentTicketService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.startrek.client.model.Issue;

@DbUnitDataSet(schema = "public", before = "ScheduleAssignmentControllerTest.before.csv")
public class ScheduleAssignmentControllerTest extends AbstractApiTest {

    @MockBean
    private ScheduleAssignmentTicketService ticketFactory;

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayAndNightScheduleDays.before.csv"
    })
    void getEmployeeScheduleInfo() throws Exception {
        mockClock(LocalDate.of(2022, 1, 24));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/lms/schedule-assignment")
                .queryParam("domainId", "1")
                .queryParam("employeeId", "1")
        )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        loadFromFile("ScheduleAssignmentControllerTest.GetEmployeeScheduleInfo.after.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayAndNightScheduleDays.before.csv"
    })
    void getEmployeeTransferDates() throws Exception {
        mockClock(LocalDate.of(2022, 1, 24));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/lms/schedule-assignment/dates")
                .queryParam("domainId", "1")
                .queryParam("employeeId", "1")
                .queryParam("oebsScheduleId", "8648")
                .queryParam("shift", "1")
                .queryParam("offset", "0")
        )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        loadFromFile("ScheduleAssignmentControllerTest.GetEmployeeTransferDates.after.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayAndNightScheduleDays.before.csv",
            "ScheduleAssignmentControllerTest.GetTransferDates.before.csv"
    })
    void getEmployeeTransferDatesForSchedule5_2() throws Exception {
        mockClock(LocalDate.of(2022, 1, 24));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/lms/schedule-assignment/dates")
                        .queryParam("domainId", "1")
                        .queryParam("employeeId", "2")
                        .queryParam("oebsScheduleId", "7754")
                        .queryParam("shift", "1")
                        .queryParam("offset", "0")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                   loadFromFile("ScheduleAssignmentControllerTest.GetEmployeeTransferDatesForSchedule5_2.after.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayAndNightScheduleDays.before.csv"
    })
    void getScheduleTreeForDayAndNightScheduleDays() throws Exception {
        mockClock(LocalDate.of(2022, 1, 24));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/lms/schedule-assignment/tree")
                .queryParam("domainId", "1")
        )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                loadFromFile("ScheduleAssignmentControllerTest.GetScheduleTreeForDayAndNightScheduleDays.after.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayOrNightScheduleDays.before.csv"
    })
    void getScheduleTreeForDayOrNightScheduleDays() throws Exception {
        mockClock(LocalDate.of(2022, 1, 24));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/lms/schedule-assignment/tree")
                        .queryParam("domainId", "1")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                 loadFromFile("ScheduleAssignmentControllerTest.GetScheduleTreeForDayOrNightScheduleDays.after.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayAndNightScheduleDays.before.csv"
    })
    void assignSchedule() throws Exception {
        mockClock(LocalDate.of(2022, 1, 24));

        ScheduleAssignmentTicketData expectedTicketData = new ScheduleAssignmentTicketData(
                1L,
                "name1",
                "login1",
                LocalDate.of(2022, 2, 1),
                "СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ",
                1,
                0,
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
                1L,
                "ФФЦ Софьино",
                "login-head",
                Set.of("login-deputy-head-1", "login-deputy-head-2")
        );
        String ticketKey = "HRMSTESTGRAFIK-1";
        String ticketTitle = "HRMSTESTGRAFIK-1: name1 (login1) СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ 2022-02-01";
        Issue issue = new Issue(
                "1",
                new URI(""),
                ticketKey,
                ticketTitle,
                1L,
                new EmptyMap<>(),
                null);
        Mockito
                .when(ticketFactory.createTicket(expectedTicketData))
                .thenReturn(new StartrekTicket(issue, null, null));

        mockMvc.perform(
                MockMvcRequestBuilders.post("/lms/schedule-assignment")
                        .queryParam("domainId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "employeeId": 1,
                                    "oebsScheduleId": 8648,
                                    "shift": 1,
                                    "offset": 0,
                                    "fromDate": "2022-02-01"
                                  }
                                ]
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        [
                          {
                            "ticketKey": "HRMSTESTGRAFIK-1",
                       "ticketTitle": "HRMSTESTGRAFIK-1: name1 (login1) СОФЬИНО_2/2 ДЕНЬ\\\\НОЧЬ ПО 11 ЧАСОВ 2022-02-01"
                          }
                        ]
                        """, true));

        Mockito.verify(ticketFactory).createTicket(expectedTicketData);
    }

    @Test
    @DbUnitDataSet(schema = "public", before = {
            "ScheduleAssignmentControllerTest.DayAndNightScheduleDays.before.csv",
            "ScheduleAssignmentControllerTest.GetEmployeeTransferDatesAndSkipWeekdays.before.csv"
    })
    void getEmployeeTransferDatesAndSkipWeekdays() throws Exception {
        LocalDate today = LocalDate.of(2022, 1, 28);
        mockClock(today);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/lms/schedule-assignment/dates")
                        .queryParam("domainId", "1")
                        .queryParam("employeeId", "1")
                        .queryParam("oebsScheduleId", "8648")
                        .queryParam("shift", "1")
                        .queryParam("offset", "0")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                  loadFromFile("ScheduleAssignmentControllerTest.GetEmployeeTransferDatesAndSkipWeekdays.after.json")));
    }
}
