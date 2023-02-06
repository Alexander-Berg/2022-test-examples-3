package ru.yandex.market.tsum.pipelines.whitemarket.jobs.release;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.FixVersionService;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueType;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Version;
import ru.yandex.startrek.client.model.VersionRef;

import static org.hamcrest.CoreMatchers.is;

@RunWith(MockitoJUnitRunner.class)
public class WhiteMarketReleaseChangelogJobTest {
    private final long versionId = 1;
    private final Version version = TestVersionBuilder.aVersion().withId(versionId).withName("2017.9.13").build();

    @InjectMocks
    private WhiteMarketReleaseChangelogJob job;

    @Mock
    private FixVersionService fixVersionService;
    @Mock
    private ReleaseInfo releaseInfo;
    @Mock
    private ReleaseIssueService releaseIssueService;

    private Component componentDesktop;
    private Component componentTouch;
    private IssueType issueTaskType;
    private IssueType issueTestType;
    private List<Issue> featureIssues;
    private VersionRef versionRef;

    @Captor
    private ArgumentCaptor<IssueUpdate> issueUpdate;

    @Before
    public void setUp() {
        componentDesktop = Mockito.mock(Component.class);
        Mockito.when(componentDesktop.getId()).thenReturn((long) 61463);

        componentTouch = Mockito.mock(Component.class);
        Mockito.when(componentTouch.getId()).thenReturn((long) 61464);

        issueTaskType = Mockito.mock(IssueType.class);
        Mockito.when(issueTaskType.getKey()).thenReturn("task");

        issueTestType = Mockito.mock(IssueType.class);
        Mockito.when(issueTestType.getKey()).thenReturn("test");

        versionRef = Mockito.mock(VersionRef.class);
        Mockito.when(versionRef.load()).thenReturn(version);
    }

    @Test
    public void shouldPutDesktopManagerTicketIntoDesktopManagerGroup() {
        final String issueKey = "Десктоп - менеджер";
        Issue issue1 = Mockito.mock(Issue.class);
        Mockito.when(issue1.getKey()).thenReturn(issueKey);
        Mockito.when(issue1.getComponents()).thenReturn(new ArrayListF<>(Collections.singletonList(componentDesktop)));
        Mockito.when(issue1.getTags()).thenReturn(new ArrayListF<>(Collections.singletonList("какой-то тег")));
        Mockito.when(issue1.getType()).thenReturn(issueTaskType);
        Mockito.when(issue1.getFixVersions()).thenReturn(new ArrayListF<>(Collections.singletonList(versionRef)));
        featureIssues = Collections.singletonList(issue1);

        Map<String, String> result = job.groupReleaseIssues(featureIssues);

        String targetTag = WhiteMarketReleaseChangelogJob.FeatureTypes.DESKTOP_MANAGER.getTag();
        Assert.assertThat(result.get(targetTag), is(issueKey));

        Assert.assertThat(result.keySet().stream().anyMatch(key -> !key.equals(targetTag)), is(true));
        Assert.assertThat(result.entrySet().stream()
            .allMatch(entry -> entry.getKey().equals(targetTag) ||
                entry.getValue().isEmpty()), is(true));
    }

    @Test
    public void shouldPutTicketWithEmptyFieldsInCommon() {
        final String issueKey = "Не заполненный тикет";
        Issue issue1 = Mockito.mock(Issue.class);
        Mockito.when(issue1.getKey()).thenReturn(issueKey);
        Mockito.when(issue1.getComponents()).thenReturn(new ArrayListF<>(Collections.emptyList()));
        featureIssues = Collections.singletonList(issue1);

        Map<String, String> result = job.groupReleaseIssues(featureIssues);

        String targetTag = WhiteMarketReleaseChangelogJob.FeatureTypes.COMMON.getTag();
        Assert.assertThat(result.get(targetTag), is(issueKey));

        Assert.assertThat(result.keySet().stream().anyMatch(key -> !key.equals(targetTag)), is(true));
        Assert.assertThat(result.entrySet().stream()
            .allMatch(entry -> entry.getKey().equals(targetTag) ||
                entry.getValue().isEmpty()), is(true));
    }

    @Test
    public void shouldPutSeveralTicketsCorrectly() {
        final String issueKey1 = "Десктоп - менеджер";
        final String issueKey2 = "Тач - автотест";
        final String issueKey3 = "Десктоп и тач - автотест";

        Issue issue1 = Mockito.mock(Issue.class);
        Mockito.when(issue1.getKey()).thenReturn(issueKey1);
        Mockito.when(issue1.getComponents()).thenReturn(new ArrayListF<>(Collections.singletonList(componentDesktop)));
        Mockito.when(issue1.getTags()).thenReturn(new ArrayListF<>(Collections.singletonList("какой-то тег")));
        Mockito.when(issue1.getType()).thenReturn(issueTaskType);
        Mockito.when(issue1.getFixVersions()).thenReturn(new ArrayListF<>(Collections.singletonList(versionRef)));

        Issue issue2 = Mockito.mock(Issue.class);
        Mockito.when(issue2.getKey()).thenReturn(issueKey2);
        Mockito.when(issue2.getComponents()).thenReturn(new ArrayListF<>(Collections.singletonList(componentTouch)));
        Mockito.when(issue2.getTags()).thenReturn(new ArrayListF<>(Arrays.asList("какой-то тег", "автотест")));
        Mockito.when(issue2.getType()).thenReturn(issueTestType);
        Mockito.when(issue2.getFixVersions()).thenReturn(new ArrayListF<>(Collections.singletonList(versionRef)));

        Issue issue3 = Mockito.mock(Issue.class);
        Mockito.when(issue3.getKey()).thenReturn(issueKey3);
        Mockito.when(issue3.getComponents()).thenReturn(new ArrayListF<>(Arrays.asList(componentDesktop,
            componentTouch)));
        Mockito.when(issue3.getTags()).thenReturn(new ArrayListF<>(Arrays.asList("какой-то тег", "автотест")));
        Mockito.when(issue3.getType()).thenReturn(issueTestType);
        Mockito.when(issue3.getFixVersions()).thenReturn(new ArrayListF<>(Collections.singletonList(versionRef)));

        featureIssues = Arrays.asList(issue1, issue2, issue3);
        Map<String, String> result = job.groupReleaseIssues(featureIssues);

        String targetTag1 = WhiteMarketReleaseChangelogJob.FeatureTypes.DESKTOP_MANAGER.getTag();
        String targetTag2 = WhiteMarketReleaseChangelogJob.FeatureTypes.DESKTOP_AUTOTESTS.getTag();
        String targetTag3 = WhiteMarketReleaseChangelogJob.FeatureTypes.TOUCH_AUTOTESTS.getTag();

        Assert.assertThat(result.get(targetTag1), is(issueKey1));

        Assert.assertThat(result.get(targetTag2), is(issueKey3));

        Assert.assertThat(result.get(targetTag3), is(String.format("%s\n%s", issueKey2, issueKey3)));
    }

    @Test
    public void shouldChangeDescriptionProperly() {
        final String oldDescription = "Some text before block\n" +
            WhiteMarketReleaseChangelogJob.DESCRIPTION_START_TAG + "\n" +
            "----\n" +
            WhiteMarketReleaseChangelogJob.DESCRIPTION_END_TAG + "\n" +
            "Some text after block\n";

        final String changelog = "\ntest\n";

        final String expectedDescription = "Some text before block\n" +
            WhiteMarketReleaseChangelogJob.DESCRIPTION_START_TAG + "\n" +
            changelog + "\n" +
            WhiteMarketReleaseChangelogJob.DESCRIPTION_END_TAG + "\n" +
            "Some text after block\n";


        Issue issue1 = Mockito.mock(Issue.class);
        Mockito.when(issue1.getDescription()).thenReturn(Option.of(oldDescription));

        Mockito.when(releaseInfo.getFixVersion()).thenReturn(FixVersion.fromVersion(version));
        Mockito.when(fixVersionService.getVersion(version.getId())).thenReturn(version);
        Mockito.when(releaseIssueService.getReleaseIssue(version)).thenReturn(issue1);

        job.applyChangelogToReleaseIssue(changelog);

        Mockito.verify(issue1).update(issueUpdate.capture());
        ScalarUpdate<String> update = (ScalarUpdate) issueUpdate.getValue().getValues().getOrElse("description", null);
        Assert.assertNotNull(update);
        Assert.assertThat(update.getSet().getOrElse(""), is(expectedDescription));
    }

}
