package ru.yandex.market.tsum.pipelines.common.jobs.issues;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.core.notify.common.ContextBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipelines.common.jobs.release.IssueStatus;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.common.resources.TicketsList;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ValidateIssuesJobTest {

    private final ReleaseIssueService releaseIssueService = mock(ReleaseIssueService.class);
    private final ValidateIssuesJobConfig validateIssuesJobConfig =
        new ValidateIssuesJobConfig.Builder()
            .setIssueStatusList(List.of(IssueStatus.READY_FOR_RELEASE))
            .setNotify(true)
            .setIssuesSource(ValidateIssuesJobConfig.IssuesSource.TICKET_LIST)
            .build();

    @Test
    public void testOkLinks() throws Exception {
        String key = "TEST-1";
        OpenHashMap<String, Object> values = new OpenHashMap<>();
        values.put("status", createStatusRef(IssueStatus.READY_FOR_RELEASE));
        when(releaseIssueService.getIssue(key)).thenReturn(createIssue(key, values));
        JobInstanceBuilder.create(ValidateIssuesJob.class)
            .withBeans(releaseIssueService)
            .withResources(
                new ReleaseInfo(new FixVersion(), key),
                validateIssuesJobConfig,
                new TicketsList(List.of(key))
            )
            .create()
            .execute(new TestTsumJobContext("me"));
    }

    @Test
    public void testIncorrectStatusLinks() throws Exception {
        String key1 = "TEST-1";
        String key2 = "TEST-2";
        OpenHashMap<String, Object> values = new OpenHashMap<>();
        values.put("status", createStatusRef(IssueStatus.OPEN));
        when(releaseIssueService.getIssue(key1)).thenReturn(createIssue(key1, values));
        when(releaseIssueService.getIssue(key2)).thenReturn(createIssue(key2, values));
        try {
            JobInstanceBuilder.create(ValidateIssuesJob.class)
                .withBeans(releaseIssueService)
                .withResources(
                    new ReleaseInfo(new FixVersion(), "123"),
                    validateIssuesJobConfig,
                    new TicketsList(List.of(key1, key2))
                )
                .create()
                .execute(new TestTsumJobContext("me"));
        } catch (IllegalStateException e) {
            assertEquals(
                "Tickets:\n" +
                    "https://st.yandex-team.ru/TEST-1 open\n" +
                    "https://st.yandex-team.ru/TEST-2 open\n" +
                    "are not in statuses: READY_FOR_RELEASE",
                e.getMessage()
            );
        }
    }

    @Test
    public void testIncorrectStatusLinksFromReleaseInfo() throws Exception {
        String key1 = "TEST-1";
        OpenHashMap<String, Object> openTicketValues = new OpenHashMap<>();
        openTicketValues.put("status", createStatusRef(IssueStatus.OPEN));
        String key2 = "TEST-2";
        OpenHashMap<String, Object> readyForReleaseTicketValues = new OpenHashMap<>();
        readyForReleaseTicketValues.put("status", createStatusRef(IssueStatus.READY_FOR_RELEASE));
        when(releaseIssueService.getIssue(key1)).thenReturn(createIssue(key1, openTicketValues));
        when(releaseIssueService.getIssue(key2)).thenReturn(createIssue(key2, readyForReleaseTicketValues));

        ValidateIssuesJobConfig releaseInfoValidateIssuesJobConfig = new ValidateIssuesJobConfig.Builder()
            .setIssueStatusList(List.of(IssueStatus.READY_FOR_RELEASE))
            .setIssuesSource(ValidateIssuesJobConfig.IssuesSource.RELEASE_INFO)
            .setNotify(true)
            .build();
        try {
            JobInstanceBuilder.create(ValidateIssuesJob.class)
                .withBeans(releaseIssueService)
                .withResources(
                    new ReleaseInfo(new FixVersion(), "123", "MBI", List.of("TEST-1", "TEST-2")),
                    releaseInfoValidateIssuesJobConfig,
                    new TicketsList(List.of())
                )
                .create()
                .execute(new TestTsumJobContext("me"));
        } catch (IllegalStateException e) {
            assertEquals(
                "Tickets:\n" +
                    "https://st.yandex-team.ru/TEST-1 open\n" +
                    "are not in statuses: READY_FOR_RELEASE",
                e.getMessage()
            );
        }
    }

    @Test
    public void testNotificationMessage() throws Exception {
        Issue openIssue = IssueBuilder.newBuilder("TEST-1")
            .setStatus("open")
            .setDisplay("Открытый тикет")
            .setAssignee("nickname", "Nick Name")
            .build();
        Issue closedIssue = IssueBuilder.newBuilder("TEST-2")
            .setStatus("closed")
            .setDisplay("Закрытый тикет")
            .setAssignee("nickname", "Nick Name")
            .build();
        String renderedMessage = NotificationUtils.render(
            ValidateIssuesJob.TICKETS_WRONG_STATE.getDefaultMessages().getTelegramDefault(),
            ContextBuilder.create()
                .with("issuesInWrongStatus", List.of(openIssue, closedIssue))
                .with("allowedStatuses", List.of(IssueStatus.FIXED, IssueStatus.CLOSED))
                .with(
                    "testScopedIssues",
                    ImmutableList.of(openIssue, closedIssue)
                )
                .build()
        );
        assertEquals("Тикеты в некорректном статусе:\n" +
                "[TEST-1](https://st.yandex-team.ru/TEST-1) ```Открытый тикет}``` open\n" +
                "[TEST-2](https://st.yandex-team.ru/TEST-2) ```Закрытый тикет}``` closed\n" +
                "\n" +
                "Необходим один из статусов: [FIXED, CLOSED]",
            renderedMessage
        );
    }

    private StatusRef createStatusRef(IssueStatus status) {
        StatusRef statusRefMock = mock(StatusRef.class);
        when(statusRefMock.getKey()).thenReturn(status.getIssueKey());
        return statusRefMock;
    }

    private Issue createIssue(String key, MapF<String, Object> values) throws URISyntaxException {
        return new Issue(
            key, new URI(key), key, key, 123L, values, null
        );
    }
}
