package ru.yandex.market.tsum.pipelines.apps.jobs;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.egit.github.core.RepositoryCommitCompare;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.apps.BlueAppsPipelineUtils;
import ru.yandex.market.tsum.pipelines.apps.MobilePlatform;
import ru.yandex.market.tsum.pipelines.apps.jobs.notification.BuildForQaNotification;
import ru.yandex.market.tsum.pipelines.apps.resources.BaseBranchRef;
import ru.yandex.market.tsum.pipelines.apps.resources.NotificationsResource;
import ru.yandex.market.tsum.pipelines.apps.resources.YandexBetaResource;
import ru.yandex.market.tsum.pipelines.common.jobs.release.IssueStatus;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.UserRef;

@RunWith(MockitoJUnitRunner.class)
public class MobileEpicUpdateJobTest {

    private static final String TICKET_KEY = "BLUEMARKETAPPS-666";
    private static final String QA_ENGINEER_LOGIN = "qaEngineerLogin";
    private static final String TELEGRAM_LOGIN = "telegramLogin";
    private static final String DOWNLOAD_LINK = "DOWNLOAD_LINK";

    @Mock
    private YandexBetaResource yandexBetaResource;
    @Mock
    private StartrekTicket startrekTicket;
    @Mock
    private GithubRepo githubRepo;
    @Mock
    private BranchRef branch;
    @Mock
    private BaseBranchRef baseBranch;
    @Mock
    private StaffApiClient staffApiClient;
    @Mock
    private GitHubClient gitHubClient;
    @Mock
    private NotificationsResource notificationsResource;
    @Mock
    private Issues issues;
    @Mock
    private StatusRef issueStatus;
    @Mock
    private UserRef qaEngineer;
    @Mock
    private StaffPerson staffPerson;
    @Mock
    private RepositoryCommitCompare commitCompare;

    @InjectMocks
    private MobileEpicUpdateJob job;

    @Mock
    private Issue issue;

    @Before
    public void setUp() {
        StaffPerson.PersonAccount account = new StaffPerson.PersonAccount(StaffPerson.AccountType.TELEGRAM,
            TELEGRAM_LOGIN);

        Mockito.when(startrekTicket.getKey()).thenReturn(TICKET_KEY);
        Mockito.when(notificationsResource.isSendNotificationToQA()).thenReturn(true);
        Mockito.when(qaEngineer.getLogin()).thenReturn(QA_ENGINEER_LOGIN);
        Mockito.when(issue.getO(BlueAppsPipelineUtils.QA_ENGINEER_FIELD)).thenReturn(Option.of(Option.of(qaEngineer)));
        Mockito.when(issue.getTags()).thenReturn(new ArrayListF<>());
        Mockito.when(issue.getStatus()).thenReturn(issueStatus);
        Mockito.when(issueStatus.getKey()).thenReturn(IssueStatus.READY_FOR_TEST_APPS.getIssueKey());
        Mockito.when(issues.get(TICKET_KEY)).thenReturn(issue);
        Mockito.when(gitHubClient.compare(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(commitCompare);
        Mockito.when(commitCompare.getCommits()).thenReturn(Collections.emptyList());
        Mockito.when(yandexBetaResource.getPlatform()).thenReturn(MobilePlatform.ANDROID);
        Mockito.when(yandexBetaResource.getDownloadLink()).thenReturn(DOWNLOAD_LINK);
        Mockito.when(staffPerson.getAccounts()).thenReturn(Collections.singletonList(account));
        Mockito.when(staffApiClient.getPerson(QA_ENGINEER_LOGIN)).thenReturn(Optional.of(staffPerson));
    }

    @Test
    public void jobDoesNotCrashIfNotificationsResourceNoExists() throws Exception {
        job.notificationsResource = null;
        TestJobContext context = new TestJobContext(false);
        job.execute(context);
    }

    @Test
    public void jobDoesNotSendNotificationIfNotificationsResourceHasValueToNotSend() throws Exception {
        Mockito.when(notificationsResource.isSendNotificationToQA()).thenReturn(false);

        TestJobContext context = new TestJobContext(false);
        job.execute(context);

        Mockito.verify(context.notifications(), Mockito.never())
            .notifySpecificTargetsAboutEvent(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void jobDoesNotSendNotificationIfIssueNotFound() throws Exception {
        Mockito.when(issues.get(TICKET_KEY)).thenReturn(null);

        TestJobContext context = new TestJobContext(false);
        job.execute(context);

        Mockito.verify(context.notifications(), Mockito.never())
            .notifySpecificTargetsAboutEvent(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void jobDoesNotSendNotificationIfQaEngineerNotFound() throws Exception {
        Mockito.when(issue.getO(BlueAppsPipelineUtils.QA_ENGINEER_FIELD)).thenReturn(Option.empty());

        TestJobContext context = new TestJobContext(false);
        job.execute(context);

        Mockito.verify(context.notifications(), Mockito.never())
            .notifySpecificTargetsAboutEvent(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void jobDoesNotSendNotificationIfTelegramLoginNotFound() throws Exception {
        Mockito.when(staffPerson.getAccounts()).thenReturn(Collections.emptyList());

        TestJobContext context = new TestJobContext(false);
        job.execute(context);

        Mockito.verify(context.notifications(), Mockito.never())
            .notifySpecificTargetsAboutEvent(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void jobDoesNotCrashIfSendingNotificationThrowsSomeError() throws Exception {
        Mockito.when(staffApiClient.getPerson(Mockito.any())).thenThrow(ServiceException.class);

        TestJobContext context = new TestJobContext(false);
        job.execute(context);
    }

    @Test
    public void jobSendNotification() throws Exception {
        TestJobContext context = new TestJobContext(false);
        job.execute(context);

        Mockito.verify(context.notifications())
            .notifySpecificTargetsAboutEvent(
                Mockito.argThat(notificationEvent ->
                    notificationEvent.getEventMeta().equals(BuildForQaNotification.BUILD_EVENT_META)
                ),
                Mockito.any(),
                Mockito.argThat(notificationTargets ->
                    notificationTargets.getTelegramTargets()
                        .stream()
                        .anyMatch(target -> target.getTargetValue().equals(TELEGRAM_LOGIN))
                )
            );
    }
}
