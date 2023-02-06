package ru.yandex.market.hrms.api.controller.calendar.absence;

import java.time.LocalDate;
import java.time.YearMonth;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.api.controller.calendar.CalendarController;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueGetAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueUpdateAnswer;
import ru.yandex.market.hrms.model.calendar.AbsenceRequest;
import ru.yandex.market.hrms.model.calendar.CorrectionReasonType;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"CalendarAbsence.environment.csv", "CalendarAbsence.schedules.csv"})
public class CalendarControllerTest extends AbstractApiTest {

    private static final String EMPLOYEE_POSITION = "Кладовщик";
    @Autowired
    CalendarController calendarController;

    @Autowired
    private Session trackerSession;

    @Captor
    private ArgumentCaptor<IssueCreate> issueCreateCaptor;
    @Captor
    private ArgumentCaptor<String> issueUpdateTicketKeyCaptor;
    @Captor
    private ArgumentCaptor<IssueUpdate> issueUpdateCaptor;

    private static final String STARTREK_QUEUE = "TESTHRMSVIRV";
    private static final String EMPLOYEE_ID = "timursha";

    @BeforeEach
    public void setUp() {
        super.setUp();
        when(trackerSession.issues().create(issueCreateCaptor.capture()))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        when(trackerSession.issues().create(issueCreateCaptor.capture(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        when(trackerSession.issues().update(issueUpdateTicketKeyCaptor.capture(), issueUpdateCaptor.capture()))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));
        when(trackerSession.issues().update(issueUpdateTicketKeyCaptor.capture(), issueUpdateCaptor.capture()
                , Mockito.any(ListF.class)))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));
        when(trackerSession.issues().update(Mockito.<String>isNull(), issueUpdateCaptor.capture()))
                .thenThrow(IllegalArgumentException.class);
        Mockito.clearInvocations(trackerSession.issues());

        when(trackerSession.issues().get(Mockito.anyString()))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        when(trackerSession.issues().get(Mockito.anyString(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        when(trackerSession.issues().get(Mockito.<String>isNull()))
                .thenThrow(IllegalArgumentException.class);
    }

    @Test
    @DbUnitDataSet(
            after = "CalendarControllerTest.after.csv",
            before = "CalendarControllerTest.before.csv"
    )
    void createOrDeleteCalendarAbsenceTest() {
        mockClock(LocalDate.of(2021, 8, 9));
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("staffLogin");
        calendarController.createOrDeleteCalendarAbsence(
                YearMonth.of(2021, 8),
                AbsenceRequest.builder()
                        .employeeStaffLogin("Example")
                        .reason(new AbsenceRequest.Reason(CorrectionReasonType.ANOTHER_REASON, "Example"))
                        .oebsAssignmentId("87066-1268")
                        .startDate(LocalDate.of(2021, 8, 9))
                        .endDate(LocalDate.of(2021, 8, 10))
                        .working(false)
                        .build(),
                "Example",
                1L,
                1L
        );
    }

    @Test
    void getCorrectionReasonTypesWorkingTrueTest() throws Exception {
        mockMvc.perform(get("/lms/calendar/correction-reasons")
                        .param("working", "true")
                ).andExpect(status().isOk())
                .andExpect((content().json(loadFromFile("correction_reasons_working_true.json"))));
    }

    @Test
    void getCorrectionReasonTypesWorkingFalseTest() throws Exception {
        mockMvc.perform(get("/lms/calendar/correction-reasons")
                        .param("working", "false")
                ).andExpect(status().isOk())
                .andExpect((content().json(loadFromFile("correction_reasons_working_false.json"))));
    }

    @Test
    void createSelfAbsence() throws Exception {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("staffLogin");
        mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("yandex_login", "staffLogin"))
                .content("""
                          {"employeeStaffLogin": "staffLogin",
                                                "reason": {
                                                "type": "SICKNESS",
                                                "text": ""
                                                 },
                                                "employeePosition": "position",
                                                "startDate": "2021-08-01",
                                                "endDate": "2021-08-01",
                                                "working": "true"
                                                }
                        """)
        ).andExpect(status().is4xxClientError());

    }

}
