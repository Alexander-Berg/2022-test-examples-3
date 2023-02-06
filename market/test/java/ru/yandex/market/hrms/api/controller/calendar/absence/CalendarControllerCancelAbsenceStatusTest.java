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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.startrek.IssueGetAnswer;
import ru.yandex.market.hrms.model.calendar.AbsenceRequest;
import ru.yandex.market.hrms.model.calendar.CorrectionReasonType;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.Transition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = {"CalendarAbsence.schedules.csv", "CalendarAbsence.environment.csv"})
public class CalendarControllerCancelAbsenceStatusTest extends AbstractApiTest {
    @Autowired
    private ObjectMapper hrmsObjectMapper;
    @Autowired
    private Session trackerSession;


    @Captor
    private ArgumentCaptor<IssueRef> issueRefCaptor;
    @Captor
    private ArgumentCaptor<Transition> transitionCaptor;
    @Captor
    private ArgumentCaptor<IssueUpdate> issueUpdateCaptor;

    @BeforeEach
    public void setUp() {
        super.setUp();

        Mockito.when(trackerSession.issues().get(Mockito.anyString()))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.anyString(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueGetAnswer(trackerSession));
        Mockito.when(trackerSession.issues().get(Mockito.<String>isNull()))
                .thenThrow(new IllegalArgumentException());

        Mockito.when(trackerSession.transitions().execute(issueRefCaptor.capture(), transitionCaptor.capture(),
                        issueUpdateCaptor.capture()))
                .thenReturn(Cf.wrap(List.of()));
    }

    @DbUnitDataSet(
            after = "CalendarControllerCancelAbsenceStatusTest.after.csv",
            before = "CalendarControllerCancelAbsenceStatusTest.before.csv"
    )
    @Test
    void shouldAllowToCancelAbsenceInValidStatuses() throws Exception {
        mockClock(LocalDate.of(2021, 2, 1));
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("ssasddasdx");
        for (LocalDate localDate : List.of(
                LocalDate.of(2021, 2, 1),
                LocalDate.of(2021, 2, 2),
                LocalDate.of(2021, 2, 3),
                LocalDate.of(2021, 2, 4)
        )) {
            postForActions(AbsenceRequest.builder()
                    .employeeStaffLogin("antipov93")
                    .reason(new AbsenceRequest.Reason(CorrectionReasonType.EMERGENCY, ""))
                    .oebsAssignmentId("480")
                    .startDate(localDate)
                    .endDate(localDate)
                    .working(true)
                    .build());
        }

        Mockito.verify(trackerSession.transitions(), Mockito.times(3))
                .execute(Mockito.<IssueRef>any(), Mockito.<Transition>any(), Mockito.any());

        MatcherAssert.assertThat(StreamEx.of(issueRefCaptor.getAllValues()).map(IssueRef::getKey).toList(),
                Matchers.contains("HRMS-100", "HRMS-101", "HRMS-102")
        );
    }

    private ResultActions postForActions(AbsenceRequest absenceRequest) throws Exception {
        return mockMvc.perform(post("/lms/calendar/absence")
                .param("date", "2021-02")
                .param("domainId", "1")
                .cookie(new Cookie("yandex_login", "antipov931"))
                .content(hrmsObjectMapper.writeValueAsString(absenceRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

}
