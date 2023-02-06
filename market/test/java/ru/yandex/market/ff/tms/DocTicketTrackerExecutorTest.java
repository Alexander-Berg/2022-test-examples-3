package ru.yandex.market.ff.tms;


import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.config.properties.StartrekProperties;
import ru.yandex.market.ff.model.dto.TicketQueueMap;
import ru.yandex.market.ff.repository.JobLogJdbcRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.StartrekIssueService;
import ru.yandex.market.ff.service.implementation.PublishToLogbrokerCalendarShopRequestChangeService;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocTicketTrackerExecutorTest extends IntegrationTest {

    @Autowired
    private StartrekIssueService startrekIssueService;

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private StartrekProperties startrekProperties;

    private DocTicketTrackerExecutor docTicketTrackerExecutor;

    @Autowired
    private PublishToLogbrokerCalendarShopRequestChangeService publishToLogbrokerCalendarShopRequestChangeService;

    @Autowired
    private TicketQueueMap ticketQueueMap;

    @BeforeEach
    public void init() {

        JobLogJdbcRepository jobLogJdbcRepository = mock(JobLogJdbcRepository.class);

        when(jobLogJdbcRepository.getLastSuccessfulExecution(any())).thenReturn(LocalDateTime.of(2021, 9, 1, 12, 0));

        docTicketTrackerExecutor = new DocTicketTrackerExecutor(
                jobLogJdbcRepository,
                startrekIssueService,
                shopRequestRepository,
                publishToLogbrokerCalendarShopRequestChangeService,
                startrekProperties, ticketQueueMap);

    }

    @Test
    @DatabaseSetup("classpath:tms/doc-ticket-tracker-executor/before.xml")
    @ExpectedDatabase(value = "classpath:tms/doc-ticket-tracker-executor/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void doJobTest() {

        String statusKey = "open";
        StatusRef statusMock = mock(StatusRef.class);
        when(statusMock.getKey()).thenReturn(statusKey);
        Issue issueMock = mock(Issue.class);
        when(issueMock.getKey()).thenReturn("TEST-1");
        when(issueMock.getStatus()).thenReturn(statusMock);

        when(startrekIssueService.getIssuesWhichStatusChangedFrom(any(), eq("TEST1p"))).thenReturn(List.of(issueMock));
        when(startrekIssueService.getIssuesWhichStatusChangedFrom(any(), eq("TEST3p"))).thenReturn(List.of());

        docTicketTrackerExecutor.doJob(null);

    }

    @Test
    @DatabaseSetup("classpath:tms/doc-ticket-tracker-executor/before.xml")
    @ExpectedDatabase(value = "classpath:tms/doc-ticket-tracker-executor/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void doJobTestWithDuplicateTickets() {

        String statusKey = "open";
        StatusRef statusMock = mock(StatusRef.class);
        when(statusMock.getKey()).thenReturn(statusKey);

        MapF<String, Object> values1 = Cf.hashMap();
        values1 = values1.plus1("status", statusMock);

        Issue issueMock1 = new Issue("1", URI.create(""), "TEST-1", "summary", 1L, values1, null);

        MapF<String, Object> values2 = Cf.hashMap();
        values2 = values2.plus1("status", statusMock);
        Issue issueMock2 = new Issue("1", URI.create(""), "TEST-1", "summary", 2L, values2, null);

        when(startrekIssueService.getIssuesWhichStatusChangedFrom(any(), eq("TEST1p"))).thenReturn(
                List.of(issueMock1, issueMock2));
        when(startrekIssueService.getIssuesWhichStatusChangedFrom(any(), eq("TEST3p"))).thenReturn(List.of());

        docTicketTrackerExecutor.doJob(null);

    }

    @Test
    @ExpectedDatabase(value = "classpath:empty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void doJobWhenNoRequestTest() {

        String statusKey = "open";
        StatusRef statusMock = mock(StatusRef.class);
        when(statusMock.getKey()).thenReturn(statusKey);
        Issue issueMock = mock(Issue.class);
        when(issueMock.getKey()).thenReturn("TEST-1");
        when(issueMock.getStatus()).thenReturn(statusMock);

        when(startrekIssueService.getIssuesWhichStatusChangedFrom(any(), eq("TEST1p")))
                .thenReturn(List.of(issueMock));
        when(startrekIssueService.getIssuesWhichStatusChangedFrom(any(), eq("TEST3p"))).thenReturn(List.of());

        docTicketTrackerExecutor.doJob(null);

    }


}
