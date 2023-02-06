package ru.yandex.market.tsum.release;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Links;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.LocalLink;
import ru.yandex.startrek.client.model.QueueRef;
import ru.yandex.startrek.client.model.Version;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 17.05.18
 */
public class ReleaseIssueServiceTest {
    private static final String RELEASE_ISSUE_KEY = "MARKETINFRA-100500";

    private Issue releaseIssue;
    private ReleaseIssueService releaseIssueService;
    private Issues startrekIssues;
    private Links startrekLinks;
    private final ReleaseInfo releaseInfo = new ReleaseInfo(new FixVersion(), RELEASE_ISSUE_KEY);

    @Before
    public void setUp() {
        releaseIssue = Mockito.mock(Issue.class);

        QueueRef queueRef = Mockito.mock(QueueRef.class);
        when(queueRef.getKey()).thenReturn("MARKETINFRA");
        when(releaseIssue.getQueue()).thenReturn(queueRef);

        startrekIssues = Mockito.mock(Issues.class);
        when(startrekIssues.find(Mockito.anyString()))
            .thenReturn(new ArrayListF<Issue>(Collections.emptyList()).iterator());

        when(startrekIssues.find(RELEASE_ISSUE_KEY))
            .thenReturn(new ArrayListF<>(Collections.singletonList(releaseIssue)).iterator());

        when(startrekIssues.get(RELEASE_ISSUE_KEY)).thenReturn(releaseIssue);

        startrekLinks = Mockito.mock(Links.class);
        Session startrekSession = Mockito.mock(Session.class);
        when(startrekSession.issues()).thenReturn(startrekIssues);
        when(startrekSession.links()).thenReturn(startrekLinks);

        releaseIssueService = new ReleaseIssueService(startrekSession);
    }

    @Test
    public void testGetIssuesByChangelogInfoList() throws Exception {
        // arrange
        ChangelogInfo changelogInfo = new ChangelogInfo(
            Arrays.asList(
                createChangelogEntry(1, "MARKETINFRA-42: Поправил всё на свете"),
                createChangelogEntry(2, "IRRELEVANT-42: Сломал всё на свете"),
                createChangelogEntry(3, "MARKETINFRA-42: Поправил всё на свете ещё разок"),
                createChangelogEntry(4, "MARKETINFRA-43, MARKETINFRA-44"),
                createChangelogEntry(5, null)
            )
        );

        Issue issue42 = createIssue("MARKETINFRA-42", StartrekConstants.TASK_ISSUE_TYPE, "test");
        Issue issue43 = createIssue("MARKETINFRA-43", StartrekConstants.TASK_ISSUE_TYPE, "test");
        Issue issue44 = createIssue("MARKETINFRA-44", StartrekConstants.TASK_ISSUE_TYPE, "test");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        when(startrekIssues.find(queryCaptor.capture()))
            .thenReturn(new ArrayListF<>(Arrays.asList(issue42, issue43, issue44)).iterator());

        // act
        List<Issue> issues = releaseIssueService.getIssuesByChangelogInfoList(
            Collections.singletonList(changelogInfo), releaseInfo
        );

        // assert
        Assert.assertThat(queryCaptor.getValue(), CoreMatchers.containsString("MARKETINFRA-42"));
        Assert.assertThat(queryCaptor.getValue(), CoreMatchers.containsString("MARKETINFRA-43"));
        Assert.assertThat(queryCaptor.getValue(), CoreMatchers.containsString("MARKETINFRA-44"));
        Assert.assertThat(queryCaptor.getValue(), CoreMatchers.not(CoreMatchers.containsString("IRRELEVANT")));

        Assert.assertEquals(
            Arrays.asList("MARKETINFRA-42", "MARKETINFRA-43", "MARKETINFRA-44"),
            issues.stream().map(IssueRef::getKey).collect(Collectors.toList())
        );
    }

    @Test
    public void unlinkReleaseIssue() {
        String testQuue = "testQuue";
        String filteredOutQueue = "filteredOutQueue";

        QueueRef queueMock = Mockito.mock(QueueRef.class);
        when(queueMock.getKey()).thenReturn(testQuue);
        Version versionMock = Mockito.mock(Version.class);
        when(versionMock.getQueue()).thenReturn(queueMock);

        List<Issue> issuesToRemove = Arrays.asList(
            createIssue("MARKETINFRATEST-12", StartrekConstants.RELEASE_ISSUE_TYPE, testQuue),
            createIssue("MARKETINFRATEST-13", StartrekConstants.RELEASE_ISSUE_TYPE, testQuue));

        List<Issue> allIssues = new ArrayList<>(issuesToRemove);
        allIssues.addAll(Arrays.asList(
            createIssue("MARKETINFRATEST-13", StartrekConstants.TASK_ISSUE_TYPE, filteredOutQueue),
            createIssue("MARKETINFRATEST-11", StartrekConstants.TASK_ISSUE_TYPE, testQuue)
        ));

        String releaseIssueKey = "MARKETINFRATEST-666";
        Issue releaseIssueToUnlink = createIssue(releaseIssueKey, StartrekConstants.RELEASE_ISSUE_TYPE, testQuue,
            allIssues.subList(0, 2)
        );
        when(startrekIssues.get(releaseIssueKey)).thenReturn(
            releaseIssueToUnlink
        );
        ReleaseInfo releaseInfoToUnlink = new ReleaseInfo(null, releaseIssueKey);

        releaseIssueService.unlinkReleaseIssue(allIssues, releaseInfoToUnlink, versionMock, Collections.emptyList());

        ArgumentCaptor<Issue> firstIssueCaptor = ArgumentCaptor.forClass(Issue.class);
        ArgumentCaptor<Issue> secondIssueCaptor = ArgumentCaptor.forClass(Issue.class);

        Mockito.verify(
            startrekLinks,
            times(2)
        ).delete(firstIssueCaptor.capture(), secondIssueCaptor.capture());

        Assert.assertEquals(
            Arrays.asList(
                releaseIssueToUnlink, releaseIssueToUnlink
            ), secondIssueCaptor.getAllValues()
        );

        Assert.assertEquals(
            issuesToRemove,
            firstIssueCaptor.getAllValues()
        );
    }

    private Issue createIssue(String issueKey, String issueType, String queue, Issue... linkedIssues) {
        return createIssue(issueKey, issueType, queue, Arrays.asList(linkedIssues));
    }

    private Issue createIssue(String issueKey, String issueType, String queue, List<Issue> linkedIssues) {
        Issue issue = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);
        when(issue.getType().getKey()).thenReturn(issueType);
        when(issue.getKey()).thenReturn(issueKey);
        when(issue.load()).thenReturn(issue);

        QueueRef queueMock = Mockito.mock(QueueRef.class);
        when(queueMock.getKey()).thenReturn(queue);
        when(issue.getQueue()).thenReturn(queueMock);

        if (linkedIssues != null && !linkedIssues.isEmpty()) {
            when(issue.getLinks()).thenReturn(
                linkedIssues.stream()
                    .map(linkedIssue -> new LocalLink(
                        null, -1, null, null, linkedIssue,
                        null, null, null,
                        null, null, null
                    ))
                    .collect(Collectors.toCollection(ArrayListF::new))
            );
        }
        return issue;
    }

    private ChangelogEntry createChangelogEntry(int revision, String change) {
        return new ChangelogEntry(Integer.toString(revision), 0L, change, "algebraic", null);
    }
}
