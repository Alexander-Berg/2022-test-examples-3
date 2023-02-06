package ru.yandex.market.hrms.api.controller.violation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.startrek.IssueAnswer;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.IssueCreate;

import javax.servlet.http.Cookie;
import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "ViolationControllerTest.before.csv")
public class ViolationControllerTest extends AbstractApiTest {
    @Autowired
    private ObjectMapper hrmsObjectMapper;
    @Autowired
    private Session trackerSession;

    @Captor
    private ArgumentCaptor<IssueCreate> issueCreateCaptor;

    private static final String STARTREK_QUEUE = "TESTHRMSFFM";
    private static final String ISSUE_SUMMARY = "Складом выявлено нарушение!";
    private static final String STAFF_LOGIN = "first";
    private static final String WMS_LOGIN_1 = "sof-second";
    private static final String WMS_LOGIN_2 = "sof-third";
    private static final LocalDate VIOLATION_DATE = LocalDate.of(2021, 4, 12);

    @BeforeEach
    public void setUp() {
        super.setUp();

        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture()))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE).withSummary(ISSUE_SUMMARY));
        Mockito.when(trackerSession.issues().create(issueCreateCaptor.capture(), Mockito.any(ListF.class)))
                .thenAnswer(new IssueAnswer(trackerSession, STARTREK_QUEUE).withSummary(ISSUE_SUMMARY));
    }

    @Test
    void registerViolationOnScheduledShift() throws Exception {
        var expectedResponse = getExpectedResponse();
        var request = new RegisterViolationRequest("-1", "-1", false, WMS_LOGIN_1, VIOLATION_DATE, STAFF_LOGIN);
        String jsonContent = hrmsObjectMapper.writeValueAsString(request);
        mockMvc.perform(post("/lms/employees/violation")
                .cookie(new Cookie("yandex_login", STAFF_LOGIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andExpect(status().isOk())
                .andExpect(content().string(equalTo(expectedResponse)));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .create(Mockito.any(IssueCreate.class));
    }

    @Test
    void registerViolationOnOvertimeShift() throws Exception {
        var expectedResponse = getExpectedResponse();
        var request = new RegisterViolationRequest("-1", "-1", false, WMS_LOGIN_2, VIOLATION_DATE, STAFF_LOGIN);
        String jsonContent = hrmsObjectMapper.writeValueAsString(request);
        mockMvc.perform(post("/lms/employees/violation")
                .cookie(new Cookie("yandex_login", STAFF_LOGIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
        ).andExpect(status().isOk())
                .andExpect(content().string(equalTo(expectedResponse)));

        Mockito.verify(trackerSession.issues(), Mockito.times(1))
                .create(Mockito.any(IssueCreate.class));
    }

    private String getExpectedResponse() throws Exception {
        RegisterViolationResponse response = new RegisterViolationResponse(
                String.format("https://st.yandex-team.ru/%s-1", STARTREK_QUEUE),
                String.format("%s-1: %s", STARTREK_QUEUE, ISSUE_SUMMARY)
        );
        return hrmsObjectMapper.writeValueAsString(response);
    }
}
