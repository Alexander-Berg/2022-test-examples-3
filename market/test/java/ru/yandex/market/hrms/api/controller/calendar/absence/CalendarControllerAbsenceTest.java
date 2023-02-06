package ru.yandex.market.hrms.api.controller.calendar.absence;

import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
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
import ru.yandex.bolts.collection.MapF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueGetAnswer;
import ru.yandex.market.hrms.core.service.startrek.IssueUpdateAnswer;
import ru.yandex.market.hrms.model.calendar.AbsenceRequest;
import ru.yandex.market.hrms.model.calendar.CorrectionReasonType;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"CalendarAbsence.schedules.csv", "CalendarControllerAbsenceTest.before.csv"})
public class CalendarControllerAbsenceTest extends AbstractApiTest {
    private static final String EMPLOYEE_LOGIN = "timursha";

    @Autowired
    private ObjectMapper hrmsObjectMapper;
    @Autowired
    private Session trackerSession;

    @Captor
    private ArgumentCaptor<IssueCreate> issueCreateCaptor;
    @Captor
    private ArgumentCaptor<IssueUpdate> issueUpdateCaptor;

    private static final String STARTREK_QUEUE = "TESTHRMSVIRV";

    @BeforeEach
    public void setUp() {
        super.setUp();
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture()))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE));

        Mockito.when(trackerSession.issues().update(Mockito.anyString(), issueUpdateCaptor.capture()))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));
        Mockito.when(trackerSession.issues().update(Mockito.anyString(), issueUpdateCaptor.capture(),
                        Mockito.any(ListF.class)))
                .thenAnswer(new IssueUpdateAnswer(trackerSession));

        Mockito.when(trackerSession.issues().get(Mockito.anyString()))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.anyString(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.<String>isNull()))
                .thenThrow(new IllegalArgumentException());
    }

    @DbUnitDataSet(after = "CalendarControllerAbsenceTest.shouldCreateAbsence.after.csv")
    @Test
    void shouldCreateAbsence() throws Exception {
        mockClock(LocalDate.of(2021, 2, 14));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 15))
                .endDate(LocalDate.of(2021, 2, 28))
                .working(false)
                .build())
                .andExpect(content().json(loadFromFile("absence_result.json"), true));

        Mockito.verify(trackerSession.issues(), Mockito.times(2))
                .create(Mockito.any(IssueCreate.class));

        List<IssueCreate> issueCreates = issueCreateCaptor.getAllValues();

        MatcherAssert.assertThat(issueCreates, Matchers.hasSize(2));

        List<MapF<String, Object>> issueCreateValues = StreamEx.of(issueCreates).map(IssueCreate::getValues).toList();

        MatcherAssert.assertThat(issueCreateValues, Matchers.containsInAnyOrder(
                Matchers.allOf(
                        Matchers.hasEntry("start", "2021-02-15"),
                        Matchers.hasEntry("end", "2021-02-19")
                ),
                Matchers.allOf(
                        Matchers.hasEntry("start", "2021-02-22"),
                        Matchers.hasEntry("end", "2021-02-26")
                )
        ));
    }

    @DbUnitDataSet(after = "CalendarControllerAbsenceTest.shouldCreateAbsenceAtStartOfMonth.after.csv")
    @Test
    void shouldCreateAbsenceAtFirstDayOfMonth() throws Exception {
        mockClock(LocalDate.of(2021, 2, 1));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 2))
                .endDate(LocalDate.of(2021, 2, 2))
                .working(false)
                .build());
    }

    @Test
    void shouldEditAbsence() throws Exception {
        mockClock(LocalDate.of(2021, 2, 1));
        postForActions(AbsenceRequest.builder()
                .employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 1))
                .endDate(LocalDate.of(2021, 2, 4))
                .working(false)
                .build())
                .andExpect(content().json(loadFromFile("absence_result_add.json"), true));

        Mockito.clearInvocations(trackerSession, trackerSession.issues());

        postForActions(AbsenceRequest.builder().employeeStaffLogin(EMPLOYEE_LOGIN)
                .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                .oebsAssignmentId("481")
                .startDate(LocalDate.of(2021, 2, 2))
                .endDate(LocalDate.of(2021, 2, 3))
                .working(true)
                .build())
                .andExpect(content().json(loadFromFile("absence_result_update1.json"), true));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .update(Mockito.anyString(), Mockito.any(IssueUpdate.class));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .create(Mockito.any(IssueCreate.class));
    }

    @DbUnitDataSet(
            before = "CalendarControllerAbsenceTest.shouldRemoveAbsenceCorrectly.before.csv",
            after = "CalendarControllerAbsenceTest.shouldRemoveAbsenceCorrectly.after.csv"
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

        Mockito.verifyZeroInteractions(trackerSession.transitions());
    }

    private ResultActions postForActions(AbsenceRequest absenceRequest) throws Exception {
        return mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "antipov93"))
                .content(hrmsObjectMapper.writeValueAsString(absenceRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }
}
