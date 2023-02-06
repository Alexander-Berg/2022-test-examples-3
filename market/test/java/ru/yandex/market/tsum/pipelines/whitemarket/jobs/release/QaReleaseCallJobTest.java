package ru.yandex.market.tsum.pipelines.whitemarket.jobs.release;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.staff.StaffPerson.PersonAccount;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEvent;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.common.resources.TicketsList;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.FixVersionService;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.misc.test.Assert;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.UserRef;
import ru.yandex.startrek.client.model.Version;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static ru.yandex.market.tsum.pipelines.whitemarket.jobs.release.QaReleaseCallJobNotifications.QA_RELEASE_HAS_TICKETS_EVENT;
import static ru.yandex.market.tsum.pipelines.whitemarket.jobs.release.QaReleaseCallJobNotifications.QA_RELEASE_WITHOUT_TICKETS_EVENT;

@RunWith(MockitoJUnitRunner.class)
public class QaReleaseCallJobTest {
    private final long versionId = 1;
    private final Version version = TestVersionBuilder.aVersion().withId(versionId).withName("2017.9.13").build();
    private final List<String> userLogins = List.of("Nice Tester", "Another Tester");
    private final TsumJobContext jobContext = new TestTsumJobContext(null);
    private final String startrekUrl = "https://st.yandex-team.ru";

    @Mock
    private QaReleaseCallJobConfig qaReleaseCallJobConfig;
    @Mock
    private StaffApiClient staffApiClient;
    @Mock
    private FixVersionService fixVersionService;
    @Mock
    private ReleaseIssueService releaseIssueService;
    @Mock
    private ReleaseInfo releaseInfo;
    @Mock
    private Session startrek;
    @InjectMocks
    private QaReleaseCallJob job;

    @Captor
    private ArgumentCaptor<List<String>> qaLogins;
    @Captor
    private ArgumentCaptor<Map<String, Object>> notificatonParams;
    @Captor
    private ArgumentCaptor<NotificationEvent> event;

    private final Map<String, List<String>> qaMockedIssueLists = new HashMap<>();
    private Component componentDesktop;
    private Component componentTouch;

    @Before
    public void setUp() {
        componentDesktop = Mockito.mock(Component.class);
        Mockito.when(componentDesktop.getName()).thenReturn("@Desktop");
        Mockito.when(componentDesktop.load()).thenReturn(componentDesktop);

        componentTouch = Mockito.mock(Component.class);
        Mockito.when(componentTouch.getName()).thenReturn("@Touch");
        Mockito.when(componentTouch.load()).thenReturn(componentTouch);

        FixVersion fixVersion = new FixVersion(versionId, "2017.9.13");
        Mockito.when(releaseInfo.getFixVersion()).thenReturn(fixVersion);
        Mockito.when(fixVersionService.getVersion(versionId)).thenReturn(version);
    }

    @Test
    public void shouldSendNoTicketsNotficationIfThereAreNoTicketsInRelease() throws Exception {
        Mockito.when(releaseIssueService.getFeatureIssues(version)).thenReturn(Collections.emptyList());
        Mockito.when(qaReleaseCallJobConfig.getComponents()).thenReturn(Collections.emptyList());
        Mockito.when(qaReleaseCallJobConfig.getShouldSkipTestedTickets()).thenReturn(false);

        job.execute(jobContext);

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(event.capture(), notificatonParams.capture());
        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.RELEASE_INFO_ARGUMENT_NAME),
            is(releaseInfo));
        Assert.assertThat(event.getValue().getEventMeta(), is(QA_RELEASE_WITHOUT_TICKETS_EVENT));
    }

    @Test
    public void shouldSendNoTicketsNotficationIfNoTicketMatchesPlatform() throws Exception {
        prepareDefaultConfigAndTicketsInRelease();
        Mockito.when(qaReleaseCallJobConfig.getComponents()).thenReturn(List.of("@Touch", "@Common", "@Api"));
        Mockito.when(qaReleaseCallJobConfig.getShouldSkipTestedTickets()).thenReturn(false);

        job.execute(jobContext);

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(event.capture(), notificatonParams.capture());
        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.RELEASE_INFO_ARGUMENT_NAME),
            is(releaseInfo));
        Assert.assertThat(event.getValue().getEventMeta(), is(QA_RELEASE_WITHOUT_TICKETS_EVENT));
    }

    @Test
    public void shouldSendTicketNotificationFromRelease() throws Exception {
        prepareDefaultConfigAndTicketsInRelease();

        job.execute(jobContext);

        Mockito.verify(staffApiClient).getPersons(qaLogins.capture());
        Assert.assertThat(qaLogins.getValue(), is(userLogins));

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(event.capture(), notificatonParams.capture());
        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.RELEASE_INFO_ARGUMENT_NAME),
            is(releaseInfo));
        Assert.assertThat(event.getValue().getEventMeta(), is(QA_RELEASE_HAS_TICKETS_EVENT));
    }

    @Test
    public void shouldHaveCorrectMapPassedToNotificationFromRelease() throws Exception {
        ReflectionTestUtils.setField(job, "startrekUrl", startrekUrl);
        prepareDefaultConfigAndTicketsInRelease();

        job.execute(jobContext);

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(ArgumentMatchers.any(),
            notificatonParams.capture());
        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.QA_MAP_ARGUMENT_NAME),
            is(qaMockedIssueLists)
        );
    }

    @Test
    public void shouldFilterTicketsByPlatform() throws Exception {
        ReflectionTestUtils.setField(job, "startrekUrl", startrekUrl);

        List<UserRef> userRefs = prepareUsersAndStaff();
        List<Issue> issues = prepareDefaultIssues(userRefs);

        // Добавляем тикет, который должен быть отфильтрован
        Issue filteredIssue = prepareIssue(
            Mockito.mock(StatusRef.class),
            componentTouch,
            null,
            "@toBeRemoved"
        );
        issues.add(filteredIssue);
        qaMockedIssueLists.remove("@toBeRemoved");

        Mockito.when(releaseIssueService.getFeatureIssues(version)).thenReturn(new ArrayListF<>(issues));
        Mockito.when(qaReleaseCallJobConfig.getComponents()).thenReturn(List.of("@Desktop", "@Common"));
        Mockito.when(qaReleaseCallJobConfig.getShouldSkipTestedTickets()).thenReturn(false);

        job.execute(jobContext);

        Mockito.verify(staffApiClient).getPersons(qaLogins.capture());
        Assert.assertThat(qaLogins.getValue(), is(userLogins));

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(ArgumentMatchers.any(),
            notificatonParams.capture());
        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.QA_MAP_ARGUMENT_NAME),
            is(qaMockedIssueLists)
        );
    }

    @Test
    public void shouldFilterTestedAndClosedTickets() throws Exception {
        ReflectionTestUtils.setField(job, "startrekUrl", startrekUrl);

        prepareDefaultConfigAndTicketsInRelease();
        Mockito.when(qaReleaseCallJobConfig.getShouldSkipTestedTickets()).thenReturn(true);

        job.execute(jobContext);

        Mockito.verify(staffApiClient).getPersons(qaLogins.capture());
        Assert.assertThat(qaLogins.getValue(), is(userLogins.subList(0, 1)));

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(event.capture(), notificatonParams.capture());
        Assert.assertThat(event.getValue().getEventMeta(), is(QA_RELEASE_HAS_TICKETS_EVENT));

        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.QA_MAP_ARGUMENT_NAME),
            is(singletonMap("@niceTgTester", qaMockedIssueLists.get("@niceTgTester")))
        );
    }

    @Test
    public void shouldSendTicketNotificationAndCorrectMapFromTicketList() throws Exception {
        ReflectionTestUtils.setField(job, "startrekUrl", startrekUrl);

        List<TicketsList> ticketsLists = List.of(
            new TicketsList(List.of("TEST-1")),
            new TicketsList(List.of("TEST-2", "TEST-3"))
        );
        // Я не понял, можно ли это сделать средствами Mockito - гугл предлагал именно через рефлексию
        ReflectionTestUtils.setField(job, "tickets", ticketsLists);

        List<Issue> ticketIssues = prepareDefaultIssues(prepareUsersAndStaff());
        Issues issues = Mockito.mock(Issues.class);
        Mockito.when(startrek.issues()).thenReturn(issues);
        Mockito.when(issues.get("TEST-1")).thenReturn(ticketIssues.get(0));
        Mockito.when(issues.get("TEST-2")).thenReturn(ticketIssues.get(1));
        Mockito.when(issues.get("TEST-3")).thenReturn(ticketIssues.get(2));

        Mockito.when(qaReleaseCallJobConfig.getComponents()).thenReturn(List.of("@Desktop", "@Common"));
        Mockito.when(qaReleaseCallJobConfig.getShouldSkipTestedTickets()).thenReturn(false);

        job.execute(jobContext);

        Mockito.verify(jobContext.notifications()).notifyAboutEvent(event.capture(), notificatonParams.capture());
        Assert.assertThat(notificatonParams.getValue().get(QaReleaseCallJobNotifications.QA_MAP_ARGUMENT_NAME),
            is(qaMockedIssueLists));
        Assert.assertThat(event.getValue().getEventMeta(), is(QA_RELEASE_HAS_TICKETS_EVENT));
    }

    private List<UserRef> prepareUsersAndStaff() {
        // QA с аккаунтом Telegram
        UserRef userRef0 = Mockito.mock(UserRef.class);
        Mockito.when(userRef0.getLogin()).thenReturn(userLogins.get(0));
        StaffPerson staffPerson0 = Mockito.mock(StaffPerson.class);
        List<PersonAccount> accounts0 = List.of(
            new PersonAccount(StaffPerson.AccountType.TELEGRAM, "niceTgTester")
        );
        Mockito.when(staffPerson0.getAccounts()).thenReturn(accounts0);
        Mockito.when(staffPerson0.getLogin()).thenReturn(userLogins.get(0));

        // QA без аккаунта Telegram
        UserRef userRef1 = Mockito.mock(UserRef.class);
        Mockito.when(userRef1.getLogin()).thenReturn(userLogins.get(1));
        StaffPerson staffPerson1 = Mockito.mock(StaffPerson.class);
        List<PersonAccount> accounts1 = List.of(
            new PersonAccount(StaffPerson.AccountType.GITHUB, "niceGitTester")
        );
        Mockito.when(staffPerson1.getAccounts()).thenReturn(accounts1);
        Mockito.when(staffPerson1.getLogin()).thenReturn(userLogins.get(1));

        Mockito.when(staffApiClient.getPersons(userLogins)).thenReturn(List.of(staffPerson0, staffPerson1));
        Mockito.when(staffApiClient.getPersons(List.of(userLogins.get(0))))
            .thenReturn(List.of(staffPerson0));

        return List.of(userRef0, userRef1);
    }

    private List<Issue> prepareDefaultIssues(List<UserRef> userRefs) {
        List<Issue> issues = new ArrayList<>();
        StatusRef statusRefOpen = Mockito.mock(StatusRef.class);
        Mockito.when(statusRefOpen.getKey()).thenReturn("open");
        StatusRef statusRefTested = Mockito.mock(StatusRef.class);
        Mockito.when(statusRefTested.getKey()).thenReturn("tested");
        StatusRef statusRefClosed = Mockito.mock(StatusRef.class);
        Mockito.when(statusRefClosed.getKey()).thenReturn("closed");

        // Первый тикет. Есть QA, у которого есть Telegram, в статусе "open"
        issues.add(prepareIssue(statusRefOpen, componentDesktop, userRefs.get(0), "@niceTgTester"));

        // Второй тикет. Есть QA, у которого нет Telegram, в статусе "tested"
        issues.add(prepareIssue(statusRefTested, componentDesktop, userRefs.get(1), "@ Задачи без QA"));

        // Третий тикет. Нет поля QA, в статусе "closed"
        issues.add(prepareIssue(statusRefClosed, componentDesktop, null, "@ Задачи без QA"));

        return issues;
    }

    private Issue prepareIssue(StatusRef status, Component component, UserRef qaUserRef, String tgAccount) {
        Issue issue = Mockito.mock(Issue.class);
        Mockito.when(issue.getStatus()).thenReturn(status);
        Mockito.when(issue.getSummary()).thenReturn("Any summary");
        Mockito.when(issue.getKey()).thenReturn("TEST-42");
        Mockito.when(issue.getComponents()).thenReturn(new ArrayListF<>(Collections.singletonList(component)));
        Mockito.when(issue.getO("qaEngineer")).thenReturn(
            Option.ofNullable(qaUserRef == null ? null : Option.of(qaUserRef))
        );
        qaMockedIssueLists.computeIfAbsent(tgAccount, k -> new ArrayList<>()).add(
            String.format("[TEST-42](%s/TEST-42): Any summary", startrekUrl)
        );
        return issue;
    }

    private void prepareDefaultConfigAndTicketsInRelease() {
        List<Issue> issues = prepareDefaultIssues(prepareUsersAndStaff());
        Mockito.when(releaseIssueService.getFeatureIssues(version)).thenReturn(new ArrayListF<>(issues));
        Mockito.when(qaReleaseCallJobConfig.getComponents()).thenReturn(List.of("@Desktop", "@Common"));
        Mockito.when(qaReleaseCallJobConfig.getShouldSkipTestedTickets()).thenReturn(false);
    }
}
