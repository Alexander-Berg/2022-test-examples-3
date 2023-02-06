package ru.yandex.market.tsum.pipelines.common.jobs.release;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.pipelines.common.resources.SuitableIssuesQuery;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.FixVersionService;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Links;
import ru.yandex.startrek.client.model.CollectionUpdate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.LinkDirection;
import ru.yandex.startrek.client.model.LocalLink;
import ru.yandex.startrek.client.model.Version;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.09.17
 */
@RunWith(MockitoJUnitRunner.class)
public class AddIssuesToReleaseJobTest {
    private final Version version = TestVersionBuilder.aVersion().withId(1).withName("2017.9.13").build();
    @Mock
    private StartrekSettings startrekSettings;
    @Mock
    private FixVersionName fixVersionName;
    @Mock
    private FixVersionService fixVersionService;
    @Mock
    private SuitableIssuesQuery suitableIssuesQuery;
    @Mock
    private Issues startrekIssues;
    @Mock
    private Links links;
    @Mock
    private TelegramNotificationTarget telegramNotificationTarget;
    @Mock
    private NotificationCenter notificationCenter;

    @InjectMocks
    private AddIssuesToReleaseJob job;

    @Before
    public void setUp() {
        Mockito.when(startrekSettings.getQueue()).thenReturn("TST");
        Mockito.when(fixVersionName.getName()).thenReturn("2017.9.13");

        Mockito.when(fixVersionService.getVersion(startrekSettings.getQueue(), fixVersionName.getName()))
            .thenReturn(version);

        Mockito.when(suitableIssuesQuery.getQuery()).thenReturn("");
    }

    @Test(expected = RuntimeException.class)
    public void noSuitableIssuesForReleaseTest() throws Exception {
        @SuppressWarnings("unchecked")
        IteratorF<Issue> issueIterator = (IteratorF<Issue>) Mockito.mock(IteratorF.class);

        Mockito.when(issueIterator.toList()).thenReturn(new ArrayListF<>());
        Mockito.when(startrekIssues.find(Matchers.anyString())).thenReturn(issueIterator);

        job.execute(null);

        Mockito.verify(notificationCenter).notify(job.noSuitableIssuesForReleaseNotification(version),
            telegramNotificationTarget);
    }

    @Test
    public void setFixVersionTest() throws Exception {
        //инициализирем первый тикет. Он должен будет выбран для релиза.
        Issue issue1 = Mockito.mock(Issue.class);
        LocalLink localLinkIssue1 = Mockito.mock(LocalLink.class, Answers.RETURNS_DEEP_STUBS.get());
        Mockito.when(localLinkIssue1.getDirection()).thenReturn(LinkDirection.OUTWARD);
        Mockito.when(localLinkIssue1.getType().getOutward()).thenReturn("Зависит от");
        Mockito.when(localLinkIssue1.getStatus().getKey()).thenReturn("closed");
        Mockito.when(links.getLocal(issue1)).thenReturn(new ArrayListF<>(Collections.singletonList(localLinkIssue1)));

        //инициализируем второй тикет. Он должен быть отброшен.
        Issue issue2 = Mockito.mock(Issue.class);
        LocalLink localLinkIssue2 = Mockito.mock(LocalLink.class, Answers.RETURNS_DEEP_STUBS.get());
        Mockito.when(localLinkIssue2.getDirection()).thenReturn(LinkDirection.OUTWARD);
        Mockito.when(localLinkIssue2.getType().getOutward()).thenReturn("Зависит от");
        Mockito.when(localLinkIssue2.getStatus().getKey()).thenReturn("open");
        Mockito.when(links.getLocal(issue2)).thenReturn(new ArrayListF<>(Collections.singletonList(localLinkIssue2)));

        @SuppressWarnings("unchecked")
        IteratorF<Issue> issueIterator = (IteratorF<Issue>) Mockito.mock(IteratorF.class);
        Mockito.when(startrekIssues.find(Matchers.anyString())).thenReturn(issueIterator);
        Mockito.when(issueIterator.toList()).thenReturn(new ArrayListF<>(Arrays.asList(issue1, issue2)));

        job.execute(new TestJobContext(false));

        ArgumentCaptor<IssueUpdate> issueUpdateArgumentCaptor = ArgumentCaptor.forClass(IssueUpdate.class);

        Mockito.verify(issue1, Mockito.times(1)).update(issueUpdateArgumentCaptor.capture());
        CollectionUpdate fixVersionOfIssue1 =
            (CollectionUpdate) issueUpdateArgumentCaptor.getValue().getValues().getTs("fixVersions");
        Assert.assertThat("Установлена не верная фикс версия",
            ((Long) fixVersionOfIssue1.getSet().get(0)).intValue(), org.hamcrest.Matchers.equalTo(1));

        Mockito.verify(issue2, Mockito.times(0)).update(Matchers.any());
    }
}
