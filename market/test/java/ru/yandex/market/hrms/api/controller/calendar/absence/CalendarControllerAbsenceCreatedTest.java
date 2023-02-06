package ru.yandex.market.hrms.api.controller.calendar.absence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueGetAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueUpdateAnswer;
import ru.yandex.market.hrms.model.calendar.AbsenceRequest;
import ru.yandex.market.hrms.model.calendar.CorrectionReasonType;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.Transition;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"CalendarAbsence.schedules.csv", "CalendarControllerAbsenceTest.before.csv"})
public class CalendarControllerAbsenceCreatedTest extends AbstractApiTest {
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
    @DbUnitDataSet(
            before = "CalendarControllerAbsenceCreatedTest.before.csv",
            after = "CalendarControllerAbsenceCreatedTest.shouldEditAbsence.after.csv"
    )
    void shouldEditAbsence() throws Exception {
        mockClock(LocalDate.of(2021, 2, 1));
        postForActions(
                AbsenceRequest.builder()
                        .employeeStaffLogin(EMPLOYEE_LOGIN)
                        .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                        .oebsAssignmentId("481")
                        .startDate(LocalDate.of(2021, 2, 2))
                        .endDate(LocalDate.of(2021, 2, 3))
                        .working(true)
                        .build())
                .andExpect(content().json(loadFromFile("absence_result_update1.json"), true));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .create(Mockito.any(IssueCreate.class));

        MatcherAssert.assertThat(issueCreateCaptor.getValue().getValues(), Matchers.allOf(
                Matchers.hasEntry(
                        Matchers.is("start"),
                        Matchers.is("2021-02-01")
                ),
                Matchers.hasEntry(
                        Matchers.is("end"),
                        Matchers.is("2021-02-01")
                )
        ));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .update(Mockito.anyString(), Mockito.any(IssueUpdate.class));

        MatcherAssert.assertThat(issueUpdateTicketKeyCaptor.getValue(), CoreMatchers.is("HRMS-100"));
        MatcherAssert.assertThat(issueUpdateCaptor.getValue().getValues(), Matchers.allOf(
                Matchers.hasEntry(
                        Matchers.is("start"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-04")))
                ),
                Matchers.hasEntry(
                        Matchers.is("end"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-04")))
                )
        ));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerAbsenceCreatedTest.before.csv")
    void shouldEditAbsenceLeft() throws Exception {
        mockClock(LocalDate.of(2021, 2, 1));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 1))
                .endDate(LocalDate.of(2021, 2, 2))
                .working(true)
                .build())
                .andExpect(content().json(loadFromFile("absence_result_update_left.json"), true));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .update(Mockito.anyString(), Mockito.any());

        MatcherAssert.assertThat(issueUpdateTicketKeyCaptor.getValue(), CoreMatchers.is("HRMS-100"));
        MatcherAssert.assertThat(issueUpdateCaptor.getValue().getValues(), Matchers.allOf(
                Matchers.hasEntry(
                        Matchers.is("start"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-03")))
                ),
                Matchers.hasEntry(
                        Matchers.is("end"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-04")))
                )
        ));
    }

    @Test
    @DbUnitDataSet(before = "CalendarControllerAbsenceCreatedTest.before.csv")
    void shouldEditAbsenceRight() throws Exception {
        mockClock(LocalDate.of(2021, 2, 3));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 3))
                .endDate(LocalDate.of(2021, 2, 4))
                .working(true)
                .build())
                .andExpect(content().json(loadFromFile("absence_result_update_right.json"), true));

        MatcherAssert.assertThat(issueUpdateTicketKeyCaptor.getValue(), CoreMatchers.is("HRMS-100"));
        MatcherAssert.assertThat(issueUpdateCaptor.getValue().getValues(), Matchers.allOf(
                Matchers.hasEntry(
                        Matchers.is("start"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-01")))
                ),
                Matchers.hasEntry(
                        Matchers.is("end"),
                        Matchers.hasProperty("set", Matchers.is(Option.of("2021-02-02")))
                )
        ));
    }

    @DbUnitDataSet(
            before = "CalendarControllerAbsenceCreatedTest.shouldRemoveAbsenceCorrectly.before.csv",
            after = "CalendarControllerAbsenceCreatedTest.shouldRemoveAbsenceCorrectly.after.csv"
    )
    @Test
    void shouldRemoveAbsenceCorrectly() throws Exception {
        mockClock(LocalDate.of(2021, 2, 3));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 3))
                .endDate(LocalDate.of(2021, 2, 26))
                .working(true)
                .build())
                .andExpect(content().json(loadFromFile("absence_result_edit.json"), true));

        List<String> issueUpdateTicketKeys = issueUpdateTicketKeyCaptor.getAllValues();

        Mockito.verify(trackerSession.issues(), Mockito.times(2))
                .update(Mockito.anyString(), Mockito.any(IssueUpdate.class));

        MatcherAssert.assertThat(issueUpdateTicketKeys, Matchers.hasItems(
                "HRMS-201", "HRMS-204"
        ));

        ArgumentCaptor<IssueRef> refCaptor = ArgumentCaptor.forClass(IssueRef.class);

        Mockito.verify(trackerSession.transitions(), Mockito.times(2))
                .execute(refCaptor.capture(), Mockito.<Transition>any(), Mockito.any());

        List<String> refs = StreamEx.of(refCaptor.getAllValues())
                .map(IssueRef::getKey)
                .toList();
        MatcherAssert.assertThat(refs, Matchers.containsInAnyOrder(
                "HRMS-202", "HRMS-203"
        ));
    }

    @Test
    @DbUnitDataSet(after = "CalendarControllerAbsenceCreatedTest.shouldCreatePresence.after.csv")
    void shouldCreatePresence() throws Exception {
        mockClock(LocalDate.of(2021, 2, 3));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.ANOTHER_REASON, "Example"))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 3))
                .endDate(LocalDate.of(2021, 2, 3))
                .working(true)
                .build());
    }

    @Test
    @DbUnitDataSet(after = "CalendarControllerAbsenceCreatedTest.shouldCreatePresence.after.csv")
    void shouldCreatePresenceAtTheShiftEnd() throws Exception {
        // смена для текущего юзера на 2021-02-03 заканчивается в 18:00
        // если прошло более 30 минут с завершения смены операции производить нельзя
        mockClock(LocalDateTime.of(2021, 2, 3, 18, 30));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.ANOTHER_REASON, "Example"))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 3))
                .endDate(LocalDate.of(2021, 2, 3))
                .working(true)
                .build());
    }

    @Test
    @DbUnitDataSet(after = "CalendarControllerAbsenceCreatedTest.shouldCreateAbsence.after.csv")
    void shouldCreateAbsenceAtTheShiftEnd() throws Exception {
        // смена для текущего юзера на 2021-02-05 заканчивается в 18:00
        // если прошло более 30 минут с завершения смены операции производить нельзя
        mockClock(LocalDateTime.of(2021, 2, 5, 18, 30));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.SICKNESS, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 5))
                .endDate(LocalDate.of(2021, 2, 5))
                .working(true)
                .build());
    }

    private ResultActions postForActions(AbsenceRequest absenceRequest) throws Exception {
        return mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "sinner"))
                .content(hrmsObjectMapper.writeValueAsString(absenceRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    void noTypeException() throws Exception {
        mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "sinner"))
                .content("""
                        {"employeeStaffLogin": "staffLogin",
                        "reason": {
                        "text": "123"
                         },
                        "employeePosition": "position",
                        "startDate": "2021-08-01",
                        "endDate": "2021-08-01",
                        "working": "true"
                        }
                        """)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void noReasonException() throws Exception {
        mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "sinner"))
                .content("""
                        {"employeeStaffLogin": "staffLogin",
                        "employeePosition": "position",
                        "startDate": "2021-08-01",
                        "endDate": "2021-08-01",
                        "working": "true"
                        }
                        """)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void closedShiftException() throws Exception {
        mockClock(LocalDate.of(2021, 2, 10));
        AbsenceRequest absenceRequest = AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 3))
                .endDate(LocalDate.of(2021, 2, 26))
                .working(true)
                .build();

        mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .cookie(new Cookie("yandex_login", "sinner"))
                .content(hrmsObjectMapper.writeValueAsString(absenceRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void closedShiftPlus31MinutesException() throws Exception {
        // смена для текущего юзера на 2021-02-03 заканчивается в 18:00
        // если прошло более 30 минут с завершения смены операции производить нельзя
        mockClock(LocalDateTime.of(2021, 2, 3, 18, 31));
        AbsenceRequest absenceRequest = AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 3))
                .endDate(LocalDate.of(2021, 2, 26))
                .working(true)
                .build();

        mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .cookie(new Cookie("yandex_login", "sinner"))
                .content(hrmsObjectMapper.writeValueAsString(absenceRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError());
    }
}
