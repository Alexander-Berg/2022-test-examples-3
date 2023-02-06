package ru.yandex.market.tsum.pipelines.ott.jobs;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import junit.framework.Assert;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.tmatesoft.svn.core.SVNLogEntry;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.market.tsum.clients.abc.AbcApiClient;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.variables.JobVariablesProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipelines.ott.config.OttSoxValidationConfig;
import ru.yandex.market.tsum.pipelines.ott.resources.ReleaseBranchInfo;
import ru.yandex.market.tsum.pipelines.ott.resources.StartReleaseInfo;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.ott.jobs.OttSoxApprovesNotifications.BUILD_INFO_COMMENT_URL;
import static ru.yandex.market.tsum.pipelines.ott.jobs.OttSoxApprovesNotifications.MENTIONS;
import static ru.yandex.market.tsum.pipelines.ott.jobs.OttSoxApprovesNotifications.OTT_SOX_APPROVES_NOTIFICATION_EVENT_META;

@RunWith(MockitoJUnitRunner.class)
public class OttSoxApprovesValidationJobTest {
    private static final String RELEASE_COMMENT = "#release info\n" +
        "master: ~release_20200130_OTT-9586\n" +
        "develop: ~release_20200213_OTT-8539\n" +
        "revision: 6350836";

    private static final String RELEASE_COMMENT_NEW = "#release info\n" +
        "previous: ~release_20200130_OTT-9586\n" +
        "current: ~release_20200213_OTT-8539\n" +
        "revision: 6350836";

    private static final String BUILD_COMMENT = "#build info\n" +
        "branch: ~release_20200213_OTT-8539\n" +
        "revision: 6350836\n" +
        "sha256: dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2";

    private static final List<String> TRUSTED_LOGINS = List.of("trusted1", "trusted2", "trusted3");

    @InjectMocks
    private OttSoxApprovesValidationJob job = new OttSoxApprovesValidationJob();

    @Mock
    private JobContext jobContext;

    @Mock
    private JobProgressContext jobProgressContext;

    @Mock
    private JobProgressContextImpl.ProgressBuilder progressBuilder;

    @Mock
    private RootArcadiaClient rootArcadiaClient;

    @Mock
    private AbcApiClient abcApiClient;

    @Mock
    private StaffApiClient staffApiClient;

    @Spy
    private StartReleaseInfo startReleaseInfo = new StartReleaseInfo(
        "OTT-8539",
        false,
        false,
        List.of(),
        false,
        null
    );

    @Spy
    private OttSoxValidationConfig soxValidationConfig = new OttSoxValidationConfig(
        2,
        "teamcity",
        "qa",
        14,
        "path",
        false,
        "",
        "ott",
        "sox_officer",
        "pr_approver");

    @Spy
    private ReleaseBranchInfo releaseBranchInfo = new ReleaseBranchInfo(
        "/branches/ott/releases/release_20200213_OTT-8539",
        "/branches/ott/releases/release_20200130_OTT-9586",
        "release_20200213_OTT-8539",
        "OTT-8539",
        123456L
    );

    @Mock
    private Issues issues;

    @Mock
    private Issue releaseIssue;

    @Mock
    private Notificator notificator;

    @Mock
    private JobVariablesProvider jobVariablesProvider;

    private ArgumentCaptor<Function> builderCapture;

    private ArgumentCaptor<String> textCapture;

    private ArgumentCaptor<TaskState> stateCapture;

    private ArgumentCaptor<NotificationEvent> eventCapture;

    private ArgumentCaptor<Map<String, Object>> eventArgumentsCapture;

    @Before
    public void setUp() {
        textCapture = ArgumentCaptor.forClass(String.class);
        when(progressBuilder.setText(textCapture.capture())).thenReturn(progressBuilder);

        stateCapture = ArgumentCaptor.forClass(TaskState.class);
        when(progressBuilder.setTaskState(stateCapture.capture())).thenReturn(progressBuilder);

        builderCapture = ArgumentCaptor.forClass(Function.class);
        doNothing().when(jobProgressContext).update(builderCapture.capture());

        when(jobContext.progress()).thenReturn(jobProgressContext);

        when(issues.get(anyString())).thenReturn(releaseIssue);

        when(rootArcadiaClient.getHead(anyString())).thenReturn(new SVNLogEntry(null, 6350836, null, false));

        eventCapture = ArgumentCaptor.forClass(NotificationEvent.class);
        eventArgumentsCapture = ArgumentCaptor.forClass(Map.class);
        doNothing().when(notificator).notifyAboutEvent(eventCapture.capture(), eventArgumentsCapture.capture());

        when(jobContext.notifications()).thenReturn(notificator);

        when(jobContext.variables()).thenReturn(jobVariablesProvider);

        ReflectionTestUtils.setField(job, "startrekUrl", "https://st.yandex-team.ru");

        StaffPerson.PersonAccount fakeTelegramAccount = new StaffPerson.PersonAccount(
            StaffPerson.AccountType.TELEGRAM, "trusted1-telegram"
        );
        StaffPerson fakeStaffPerson = new StaffPerson(
            "trusted1", 0, null, Collections.singletonList(fakeTelegramAccount),
            null, null
        );
        when(staffApiClient.getPerson(eq("trusted1")))
            .thenReturn(Optional.of(fakeStaffPerson));

    }

    @Test
    public void noReleaseInfoTest() throws Exception {
        when(releaseIssue.getComments()).thenReturn(Cf.emptyIterator());

        long approves = job.approvesPoller(jobContext, "", "", List.of()).call();
        Assert.assertEquals(-1L, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Release info not found", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.RUNNING, stateCapture.getValue().getStatus());
    }

    @Test
    public void wrongRevisionTest() throws Exception {
        when(rootArcadiaClient.getHead(anyString())).thenReturn(new SVNLogEntry(null, 100, null, false));

        ListF<Comment> comments = Cf.list(
            mockedComment(RELEASE_COMMENT, "qa"),
            mockedComment(BUILD_COMMENT, "teamcity")
        );
        when(releaseIssue.getComments()).thenReturn(comments.iterator());

        long approves = job.approvesPoller(jobContext, "", "", List.of()).call();
        Assert.assertEquals(-1L, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Release info not found", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.RUNNING, stateCapture.getValue().getStatus());
    }

    @Test
    public void noBuildInfoTest() throws Exception {
        ListF<Comment> comments = Cf.list(mockedComment(RELEASE_COMMENT, "qa"));
        when(releaseIssue.getComments()).thenReturn(comments.iterator());

        long approves = job.approvesPoller(jobContext, "", "", List.of()).call();
        Assert.assertEquals(-1L, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Build info not found", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.RUNNING, stateCapture.getValue().getStatus());
        Assert.assertEquals(0f, stateCapture.getValue().getProgress());
    }

    @Test
    public void noOkTest() throws Exception {
        ListF<Comment> comments = Cf.list(
            mockedComment(RELEASE_COMMENT, "qa"),
            mockedComment(BUILD_COMMENT, "teamcity")
        );
        when(releaseIssue.getComments()).thenReturn(comments.iterator());

        long approves = job.approvesPoller(jobContext, "", "", TRUSTED_LOGINS).call();
        Assert.assertEquals(0, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Approves: 0", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.RUNNING, stateCapture.getValue().getStatus());
        Assert.assertEquals(0f, stateCapture.getValue().getProgress());
    }

    @Test
    public void oneOkTest() throws Exception {
        ListF<Comment> comments = Cf.list(
            mockedComment(RELEASE_COMMENT, "qa"),
            mockedComment(BUILD_COMMENT, "teamcity"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "trusted1"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "trusted1"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "test")
        );
        when(releaseIssue.getComments()).thenReturn(comments.iterator());

        long approves = job.approvesPoller(jobContext, "", "", TRUSTED_LOGINS).call();
        Assert.assertEquals(1, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Approves: 1", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.RUNNING, stateCapture.getValue().getStatus());
        Assert.assertEquals(0.5f, stateCapture.getValue().getProgress());
    }

    @Test
    public void successTest() throws Exception {
        ListF<Comment> comments = Cf.list(
            mockedComment(RELEASE_COMMENT, "qa"),
            mockedComment(BUILD_COMMENT, "teamcity"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "trusted1"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "test"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "trusted2")
        );
        when(releaseIssue.getComments()).thenReturn(comments.iterator());

        long approves = job.approvesPoller(jobContext, "OTT-8539", "", TRUSTED_LOGINS).call();
        Assert.assertEquals(2, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Approves: 2", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.SUCCESSFUL, stateCapture.getValue().getStatus());
        Assert.assertEquals(1.0f, stateCapture.getValue().getProgress());

        NotificationEvent event = eventCapture.getValue();
        Map<String, Object> arguments = eventArgumentsCapture.getValue();
        Assert.assertEquals(OTT_SOX_APPROVES_NOTIFICATION_EVENT_META, event.getEventMeta());
        Assert.assertEquals(
            Set.of("@trusted1-telegram"),
            Set.of(((String) arguments.get(MENTIONS)).split(" "))
        );
        Assert.assertEquals("https://st.yandex-team.ru/OTT-8539#1590423166000", arguments.get(BUILD_INFO_COMMENT_URL));
    }

    @Test
    public void successTestNewNaming() throws Exception {
        ListF<Comment> comments = Cf.list(
            mockedComment(RELEASE_COMMENT_NEW, "qa"),
            mockedComment(BUILD_COMMENT, "teamcity"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "trusted1"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "test"),
            mockedComment("!SOXOK:dfa7e608eeab42726124fa2c106bcc7eebee5a8976148f0c43e6404ffd5d43d2", "trusted2")
        );
        when(releaseIssue.getComments()).thenReturn(comments.iterator());

        long approves = job.approvesPoller(jobContext, "", "", TRUSTED_LOGINS).call();
        Assert.assertEquals(2, approves);

        builderCapture.getValue().apply(progressBuilder);
        Assert.assertEquals("Approves: 2", textCapture.getValue());
        Assert.assertEquals(TaskState.TaskStatus.SUCCESSFUL, stateCapture.getValue().getStatus());
        Assert.assertEquals(1.0f, stateCapture.getValue().getProgress());
    }

    private static Comment mockedComment(String text, String createdBy) {
        UserRef userRef = mock(UserRef.class);
        when(userRef.getLogin()).thenReturn(createdBy);

        Comment mock = mock(Comment.class);
        when(mock.getText()).thenReturn(Option.of(text));
        when(mock.getUpdatedBy()).thenReturn(userRef);
        when(mock.getCreatedAt()).thenReturn(Instant.ofEpochMilli(1590423166340L));
        return mock;
    }
}
