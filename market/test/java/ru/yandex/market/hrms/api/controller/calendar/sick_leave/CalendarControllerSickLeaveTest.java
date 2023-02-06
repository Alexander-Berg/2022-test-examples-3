package ru.yandex.market.hrms.api.controller.calendar.sick_leave;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueGetAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueUpdateAnswer;
import ru.yandex.market.hrms.model.calendar.CommonAbsenceRequest;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "CalendarControllerSickLeaveTest.before.csv")
public class CalendarControllerSickLeaveTest extends AbstractApiTest {
    @Autowired
    private ObjectMapper hrmsObjectMapper;
    @Autowired
    private Session trackerSession;

    @Captor
    private ArgumentCaptor<IssueCreate> issueCreateCaptor;
    @Captor
    private ArgumentCaptor<String> issueUpdateTicketKeyCaptor;
    @Captor
    private ArgumentCaptor<IssueUpdate> issueUpdateCaptor;

    private static final String STARTREK_QUEUE = "TESTHRMSVIRV";
    private static final String EMPLOYEE_LOGIN = "timursha";
    private static final String EMPLOYEE_POSITION = "Кладовщик";

    @BeforeEach
    public void setUp() {
        super.setUp();
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture()))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        Mockito.when(trackerSession.issues().update(issueUpdateTicketKeyCaptor.capture(), issueUpdateCaptor.capture()))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));
        Mockito.when(trackerSession.issues().update(issueUpdateTicketKeyCaptor.capture(), issueUpdateCaptor.capture()
                        , Mockito.any(ListF.class)))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));
        Mockito.when(trackerSession.issues().update(Mockito.<String>isNull(), issueUpdateCaptor.capture()))
                .thenThrow(IllegalArgumentException.class);
        Mockito.clearInvocations(trackerSession.issues());

        Mockito.when(trackerSession.issues().get(Mockito.anyString()))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.anyString(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.<String>isNull()))
                .thenThrow(IllegalArgumentException.class);
    }

    @Test
    @DbUnitDataSet(after = "CalendarControllerSickLeaveCreatedTest.after.csv")
    void shouldCreateSickLeaveForFuture() throws Exception {
        mockClock(LocalDate.of(2021, 2, 2));
        postForActions(CommonAbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 2))
                .endDate(LocalDate.of(2021, 2, 4))
                .build())
                .andExpect(content().json(loadFromFile("future_sick_leave_created.json"), true));

    }

    @Test
    @DbUnitDataSet(
            before = "CalendarControllerSickLeaveCreatedTest.past.before.csv",
            after = "CalendarControllerSickLeaveCreatedTest.past.after.csv")
    void shouldCreateSickLeaveForPast() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 4,10,0));
        postForActions(CommonAbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 13))
                .endDate(LocalDate.of(2021, 2, 16))
                .build())
                .andExpect(content().json(loadFromFile("past_sick_leave_created.json"), true));
    }

    @Test
    @DbUnitDataSet(
            before = "CalendarControllerSickLeaveCreatedTest.cancel.before.csv",
            after = "CalendarControllerSickLeaveCreatedTest.cancel.after.csv")
    void shouldCancelSickLeave() throws Exception {
        mockClock(LocalDate.of(2021, 2, 17));
        deleteSickLeave(CommonAbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 13))
                .endDate(LocalDate.of(2021, 2, 13))
                .build())
                .andExpect(content().json(loadFromFile("past_sick_leave_cancelled.json"), true));

    }

    private ResultActions postForActions(CommonAbsenceRequest request) throws Exception {
        return mockMvc.perform(post("/lms/calendar/sick-leave")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "admin"))
                .content(hrmsObjectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    private ResultActions deleteSickLeave(CommonAbsenceRequest request) throws Exception {
        return mockMvc.perform(delete("/lms/calendar/sick-leave")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "admin"))
                .content(hrmsObjectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }
}

