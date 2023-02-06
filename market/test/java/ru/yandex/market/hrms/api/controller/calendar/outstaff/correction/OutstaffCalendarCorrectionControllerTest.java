package ru.yandex.market.hrms.api.controller.calendar.outstaff.correction;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.outstaff.correction.ticket.WorkAnotherLoginCorrectionTicketService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.startrek.client.model.Issue;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@DbUnitDataSet(schema = "public", before = "OutstaffCalendarCorrectionControllerTest.before.csv")
public class OutstaffCalendarCorrectionControllerTest extends AbstractApiTest {

    @MockBean
    private WorkAnotherLoginCorrectionTicketService ticketService;

    @AfterEach
    public void clearMocks() {
        Mockito.clearInvocations(ticketService);
        Mockito.reset(ticketService);
    }

    @Test
    @DbUnitDataSet(schema = "public",
            before = "OutstaffCalendarCorrectionControllerTest.CreateNotConfirmedAbsence.before.csv",
            after = "OutstaffCalendarCorrectionControllerTest.CreateNotConfirmedAbsence.after.csv")
    public void shouldCreateNotConfirmedAbsenceWhenAbsenceNotExists() throws Exception {
        mockClock(LocalDateTime.of(2022, 3, 10, 10, 45, 0));
        mockCreateTicket("TESTQUEUE-1");

        mockMvc.perform(post("/lms/outstaff-calendar/corrections")
                        .param("month", "2022-01")
                        .param("domainId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                               {
                                  "outstaffId": "100",
                                   "date": "2022-01-01",
                                   "reason": {
                                        "reasonType": "WORK_ANOTHER_LOGIN"
                                   }
                               }""")
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.cells[0].type", is("MAYBE_ABSENCE")));
    }


    @Test
    @DbUnitDataSet(schema = "public", before = "OutstaffCalendarCorrectionControllerTest.AlreadyExistAbsence.before.csv")
    public void shouldNotCreateAbsenceWhenAbsenceAlreadyExists() throws Exception {
        mockClock(LocalDateTime.of(2022, 3, 10, 10, 45, 0));
        mockCreateTicket("TESTQUEUE-3");

        mockMvc.perform(MockMvcRequestBuilders.post("/lms/outstaff-calendar/corrections")
                        .param("month", "2022-01")
                        .param("domainId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                               {
                                  "outstaffId": "100",
                                   "date": "2022-01-01",
                                   "reason": {
                                        "reasonType": "WORK_ANOTHER_LOGIN"
                                   }
                               }""")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", containsString("Уже существует корректировка на дату")));
    }

    private void mockCreateTicket(String issueKey) {
        var issue = Mockito.mock(Issue.class);
        when(issue.getKey()).thenReturn(issueKey);
        var ticket =  new StartrekTicket(issue, null, null);
        when(ticketService.createTicket(any(), any(), any(), any())).thenReturn(ticket);
    }
}
