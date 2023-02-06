package ru.yandex.market.pers.notify.staff;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.external.sender.SenderClient;
import ru.yandex.market.pers.notify.test.MailProcessorInvoker;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static ru.yandex.market.pers.notify.test.TestUtil.withSuccessJson;

public class AbstractStaffWelcomeMailTest extends MarketMailerMockedDbTest {
    static final LocalDate DATE_2015_11_03 = LocalDate.of(2015, 11, 03);
    static final String EXPECTED_EMAIL = "a-danilov@yandex-team.ru";
    private static final String JOINED_AT_2015_11_03_URL =
            "http://localhost/staff/persons?_fields=login,name.first.ru,name.last.ru&_limit=1000" +
            "&official.join_at=2015-11-03&official.is_homeworker=false" +
            "&department_group.ancestors.department.url=yandex_monetize_market";

    static final String STAFF_ONE_PERSON_RESPONSE_JSON = "/data/staff/staff_one_person_response.json";

    @Autowired
    MailProcessorInvoker mailProcessorInvoker;
    @Autowired
    SenderClient senderClient;
    @Autowired
    StaffWelcomeService staffWelcomeService;
    @Autowired
    EventSourceDAO eventSourceDAO;
    @Autowired
    @Qualifier("staffRestTemplate")
    private RestTemplate staffRestTemplate;

    private MockRestServiceServer staffMock;

    private static RequestMatcher header(String name, String value) {
        return request -> {
            List<String> headerValues = request.getHeaders().get(name);
            assertNotNull(headerValues);
            assertTrue(headerValues.contains(value));
        };
    }

    @BeforeEach
    public void setUpStaffMock() {
        staffMock = MockRestServiceServer.createServer(staffRestTemplate);
    }

    void expectCallToStaff(String responseFileName) throws IOException {
        staffMock.reset();
        staffMock.expect(once(), requestTo(JOINED_AT_2015_11_03_URL))
                .andExpect(header("Authorization", "OAuth token"))
                .andRespond(withSuccessJson(responseFileName));
    }

    void scheduleAndProcessWelcomeMails() throws IOException {
        expectCallToStaff(STAFF_ONE_PERSON_RESPONSE_JSON);

        staffWelcomeService.scheduleStaffWelcomeMails(DATE_2015_11_03);
        mailProcessorInvoker.processAllMail();
    }
}
