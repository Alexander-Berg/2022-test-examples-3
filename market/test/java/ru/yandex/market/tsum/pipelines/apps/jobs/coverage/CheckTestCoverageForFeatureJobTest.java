package ru.yandex.market.tsum.pipelines.apps.jobs.coverage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipelines.apps.resources.CheckTestCoverageConfigResource;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Links;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueTypeRef;
import ru.yandex.startrek.client.model.ResolutionRef;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.bolts.collection.Option.empty;

@RunWith(MockitoJUnitRunner.class)
public class CheckTestCoverageForFeatureJobTest {

    private static final String TICKET_KEY = "ASD-123";

    @InjectMocks
    private CheckTestCoverageForFeatureJob job = new CheckTestCoverageForFeatureJob();
    @Mock
    private CheckTestCoverageConfigResource config;
    @Mock
    private StartrekTicket startrekTicket;
    @Mock
    private Issues startrekIssues;
    @Mock
    private Links links;
    @Mock
    private CheckTestCoverageNotifications notifications;
    @Mock
    private GitHubHelper gitHubHelper;
    @Mock
    private TestpalmHelper testpalmHelper;
    @Mock
    private JobContext context;
    @Mock
    private JobActionsContext actionsContext;
    @Mock
    private JobProgressContext progressContext;

    @Before
    public void setUp() {
        when(context.actions()).thenReturn(actionsContext);
        when(context.progress()).thenReturn(progressContext);
        when(config.getAdditions()).thenReturn(10);
        when(startrekTicket.getKey()).thenReturn(TICKET_KEY);
    }

    private boolean shouldAutomatedTemplate(String type, Option<Object> scope, Option<Object> automation,
                                            ListF<String> tags, int changes) {
        mockShouldAutomated(type, scope, automation, tags, changes);
        return job.shouldAutomated(TICKET_KEY);
    }

    private void mockShouldAutomated(String type, Option<Object> scope, Option<Object> automation, ListF<String> tags,
                                     int changes) {

        IssueTypeRef issueTypeRef = mock(IssueTypeRef.class);
        when(issueTypeRef.getKey()).thenReturn(type);

        Issue issue = mock(Issue.class);
        when(issue.getO("testScope")).thenReturn(scope);
        when(issue.getO("autotesting")).thenReturn(automation);
        when(issue.getTags()).thenReturn(tags);
        when(issue.getType()).thenReturn(issueTypeRef);
        when(startrekIssues.get(TICKET_KEY)).thenReturn(issue);
        when(gitHubHelper.getTotalAdditions()).thenReturn(changes);
    }

    @Test
    public void shouldAutomatedBasic() {
        assertTrue(shouldAutomatedTemplate("task", empty(), empty(), Cf.list(), 100));
    }

    @Test
    public void shouldAutomatedWithNoScope() {
        assertFalse(shouldAutomatedTemplate("task", Option.of(Option.of("Нет")), empty(), Cf.list(), 100));
    }

    @Test
    public void shouldAutomatedAutotest() {
        assertFalse(shouldAutomatedTemplate("test", empty(), empty(), Cf.list(), 100));
    }

    @Test
    public void shouldAutomatedExp() {
        assertFalse(shouldAutomatedTemplate("task", empty(), empty(), Cf.list("custom-tag", "exp"), 100));
    }

    @Test
    public void shouldAutomatedSmallCode() {
        assertFalse(shouldAutomatedTemplate("task", empty(), empty(), Cf.list(), 1));
    }


    @Test
    public void shouldAutomatedWithExplicitNoFeature() {
        assertFalse(shouldAutomatedTemplate("task", empty(), Option.of(Option.of("Тестирование не требуется")),
            Cf.list(), 100));
    }

    @Test
    public void failByCases() throws Exception {
        mockShouldAutomated("task", empty(), empty(), Cf.list(), 100);
        when(testpalmHelper.getLinkedTestCasesCount(TICKET_KEY)).thenReturn(0);

        job.execute(context);

        verify(notifications).sendNoCasesEvent(any(), eq(TICKET_KEY));
        verify(actionsContext).failJob("Тесткейсы не найдены", SupportType.NONE);
    }

    @Test
    public void failByNoTasks() throws Exception {
        mockShouldAutomated("task", empty(), empty(), Cf.list(), 100);
        when(testpalmHelper.getLinkedTestCasesCount(TICKET_KEY)).thenReturn(2);
        when(startrekIssues.find(any(String.class))).thenReturn(Cf.emptyIterator());

        job.execute(context);

        verify(notifications).sendNoTestsEvent(any(), eq(TICKET_KEY));
        verify(actionsContext).failJob("Не найдено ни одного тикета на реализацию автотестов", SupportType.NONE);
    }

    @Test
    public void failByUnresolved() throws Exception {
        mockShouldAutomated("task", empty(), empty(), Cf.list(), 100);
        when(testpalmHelper.getLinkedTestCasesCount(TICKET_KEY)).thenReturn(2);

        Issue issue = mock(Issue.class);
        when(issue.getResolution()).thenReturn(empty());

        ListF<Issue> unresolvedList = Cf.list(issue);
        when(startrekIssues.find(any(String.class))).thenReturn(unresolvedList.iterator());

        job.execute(context);

        verify(notifications).sendUnresolvedEvent(any(), eq(TICKET_KEY), eq(unresolvedList));
        verify(actionsContext).failJob("Завершите работы по задачам автоматизации", SupportType.NONE);
    }

    @Test
    public void automated() throws Exception {
        mockShouldAutomated("task", empty(), empty(), Cf.list(), 100);
        when(testpalmHelper.getLinkedTestCasesCount(TICKET_KEY)).thenReturn(2);

        ResolutionRef resolutionRef = mock(ResolutionRef.class);
        Issue issue = mock(Issue.class);
        when(issue.getResolution()).thenReturn(Option.of(resolutionRef));
        ListF<Issue> automationTasks = Cf.list(issue);
        when(startrekIssues.find(any(String.class))).thenReturn(automationTasks.iterator());

        job.execute(context);

        verify(progressContext).update(any());
    }

    @Test
    public void skipAutomationCheck() throws Exception {
        mockShouldAutomated("test", empty(), empty(), Cf.list(), 100);
        job.execute(context);
        verify(progressContext).update(any());
    }
}
