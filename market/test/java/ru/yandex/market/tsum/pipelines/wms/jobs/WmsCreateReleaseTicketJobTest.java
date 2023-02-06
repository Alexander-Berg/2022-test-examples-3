package ru.yandex.market.tsum.pipelines.wms.jobs;

import java.util.List;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.EmptyIterator;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.misc.test.Assert;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.Versions;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.QueueRef;
import ru.yandex.startrek.client.model.SearchRequest;
import ru.yandex.startrek.client.model.Version;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WmsCreateReleaseTicketJobTest {

    private static final String DUMMY_USER = "dummy-user";
    private static final String ISSUE_DESCRIPTION = "**Состав релиза**\n\n"
        + "Функциональные задачи:\n\nMARKETWMS-123\n\n"
        + "Технические задачи:\n\nMARKETWMS-223\n\n"
        + "Не определено:\n\nMARKETWMS-323\n\n"
        + "**Выложено в прод:**";
    private static final String RELEASE_SUMMARY = "[2021.03.01]";
    private static final String QUEUE = "MARKETWMSTEST";
    private static final String WMS_TESTING_BOARD = "10967";

    private ReleaseIssueService releaseIssueService;

    private final TestJobContext jobContext = new TestJobContext();
    private Versions versionsMock;
    private Issues issuesMock;
    private Issue issueMock;


    @Before
    public void setup() {
        Version versionMock = Mockito.mock(Version.class);
        QueueRef queueMock = Mockito.mock(QueueRef.class);
        when(versionMock.getName()).thenReturn("2021.03.01");
        when(versionMock.getQueue()).thenReturn(queueMock);

        when(queueMock.getKey()).thenReturn("MARKETWMSTEST");

        versionsMock = mock(Versions.class);
        when(versionsMock.get(1234)).thenReturn(versionMock);

        Session session = mock(Session.class);
        issuesMock = mock(Issues.class);
        issueMock = mock(Issue.class);
        when(session.issues()).thenReturn(issuesMock);
        when(issuesMock.find(any(SearchRequest.class))).thenReturn(new EmptyIterator());
        when(issuesMock.get(anyString())).thenReturn(issueMock);
        when(issueMock.isSet("manualTesting"))
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(false);
        when(issueMock.get("manualTesting"))
            .thenReturn(Option.of("Требуется проверка"))
            .thenReturn(Option.of("Не требуется проверка"))
            .thenReturn(Option.empty());

        releaseIssueService = new ReleaseIssueService(session);
    }

    private ReleaseInfo createReleaseInfo() {
        return new ReleaseInfo(
            new FixVersion(1234, "2021.03.01"), "MARKETWMS-TEST", "MARKETWMSTEST",
            List.of("MARKETWMS-123",
                "MARKETWMS-223",
                "MARKETWMS-323"));
    }

    @Test
    public void shouldCreateReleaseTicket() throws Exception {
        WmsCreateReleaseTicketJob wmsCreateReleaseTicketJob = new WmsCreateReleaseTicketJob();

        wmsCreateReleaseTicketJob.setReleaseIssueService(releaseIssueService);
        wmsCreateReleaseTicketJob.setVersions(versionsMock);
        wmsCreateReleaseTicketJob.setReleaseInfo(createReleaseInfo());

        wmsCreateReleaseTicketJob.execute(jobContext);

        ArgumentCaptor<IssueCreate> issuesCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issuesMock, times(1)).create(issuesCaptor.capture());

        IssueCreate issue = issuesCaptor.getValue();
        MapF<String, Object> values = issue.getValues();


        Assert.equals(RELEASE_SUMMARY, values.getO("summary").get());
        Assert.equals(ISSUE_DESCRIPTION, values.getO("description").get());
        Assert.equals(DUMMY_USER, values.getO("qaEngineer").get());
        Assert.equals(DUMMY_USER, values.getO("assignee").get());
        Assert.equals("release", values.getO("type").get());
        Assert.equals(QUEUE, values.getO("queue").get());
        Assert.equals(List.of(WMS_TESTING_BOARD), values.getO("boards").get());

        Duration dur = new Duration((Instant) values.getO("start").get(), (Instant) values.getO("deadline").get());
        Assert.equals(3L, dur.getStandardDays());
    }

}
