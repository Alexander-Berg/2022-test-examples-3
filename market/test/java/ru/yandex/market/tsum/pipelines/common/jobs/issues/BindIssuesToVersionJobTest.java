package ru.yandex.market.tsum.pipelines.common.jobs.issues;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.common.resources.ConductorPackage;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.release.FixVersionService;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 11.01.18
 */
public class BindIssuesToVersionJobTest {
    private static final String VERSION = "42.42.42";
    private static final String USER = "user42";
    private static final String PIPE_ID = "pipeId";
    private static final String PROJECT_ID = "projectId";
    private static final String PIPE_LAUNCH_ID = "pipeLaunchId";

    private final List<ChangelogEntry> changelogEntryList = Arrays.asList(
        createChangelogEntry(11, "change11"),
        createChangelogEntry(12, "change12"),
        createChangelogEntry(13, "change13"),
        createChangelogEntry(21, "change21"),
        createChangelogEntry(22, "change22"),
        createChangelogEntry(23, "change23"),
        createChangelogEntry(24, "change24")
    );

    private Issue releaseIssue;
    private ReleaseIssueService releaseIssueService;
    private FixVersionService fixVersionService;
    private ReleaseDao releaseDao;
    private FixVersion fixVersion;
    private Issues startrekIssues;
    private JobInstanceBuilder<BindIssuesToVersionJob> sutBuilder;

    @Before
    public void setUp() {
        String releaseIssueKey = "releaseIssueKey";
        fixVersion = new FixVersion(42, "fixVersionName");
        fixVersionService = Mockito.mock(FixVersionService.class);
        releaseIssueService = Mockito.mock(ReleaseIssueService.class);
        releaseIssue = Mockito.mock(Issue.class);
        Mockito.when(releaseIssueService.getIssue(releaseIssueKey)).thenReturn(releaseIssue);

        startrekIssues = Mockito.mock(Issues.class);
        Mockito.when(startrekIssues.find(Mockito.anyString()))
            .thenReturn(new ArrayListF<Issue>(Collections.emptyList()).iterator());

        sutBuilder = JobInstanceBuilder.create(BindIssuesToVersionJob.class)
            .withBeans(fixVersionService, releaseIssueService, startrekIssues)
            .withResources(new ReleaseInfo(fixVersion, releaseIssueKey));

        releaseDao = Mockito.mock(ReleaseDao.class);
    }

    @Test
    public void testNeverCommentIssue() throws Exception {
        Mockito.when(releaseDao.getReleasesByPipeLaunchIds(Mockito.any())).thenReturn(Collections.emptyList());

        sutBuilder.create().execute(new TestTsumJobContext(null, releaseDao, "me"));

        Mockito.verify(releaseIssue, Mockito.never()).comment(Mockito.anyString());
    }

    @Test
    public void testParallelReleasesComment() throws Exception {
        List<Release> runningReleases = Arrays.asList(
            createRelease("12", "Релиз #12", "12", Instant.ofEpochSecond(12L)),
            createRelease("21", "Релиз #21", "21", Instant.ofEpochSecond(21L))
        );

        Mockito.when(releaseDao.getReleasesByPipeLaunchIds(Mockito.any())).thenReturn(runningReleases);

        sutBuilder
            .withResource(new DeliveryPipelineParams("23", "10", "10"))
            .withResource(new ChangelogInfo(
                changelogEntryList, changelogEntryList.subList(changelogEntryList.size() - 5,
                changelogEntryList.size() - 1)
            ))
            .create()
            .execute(new TestTsumJobContext(null, releaseDao, "me"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(releaseIssue, Mockito.times(1)).comment(captor.capture());

        Assert.assertEquals(
            "Изменения, попавшие в данный релиз:\n" +
                "* 23 (@user42) %%change23%%\n" +
                "* 22 (@user42) %%change22%%\n" +
                "\n" +
                "Изменения, ещё не попавшие в продакшн, едут в предыдущих релизах:\n" +
                "* ((http://tsum.ru/release/21 Релиз #21))\n" +
                "  * 13 (@user42) %%change13%%\n" +
                "\n" +
                "Если предыдущие релизы будут отменены, их коммиты будут выкачены данным релизом.\n" +
                "\n" +
                "<{Полный список изменений:\n" +
                "Изменения, попавшие в данный релиз:\n" +
                "* 24 (@user42) %%change24%%\n" +
                "* 23 (@user42) %%change23%%\n" +
                "* 22 (@user42) %%change22%%\n" +
                "\n" +
                "Изменения, ещё не попавшие в продакшн, едут в предыдущих релизах (или доедут в этом релизе):\n" +
                "* ((http://tsum.ru/release/21 Релиз #21))\n" +
                "  * 13 (@user42) %%change13%%\n" +
                "* ((http://tsum.ru/release/12 Релиз #12))\n" +
                "  * 11 (@user42) %%change11%%" +
                "\n" +
                "}>",
            captor.getValue()
        );
    }

    @Test
    public void testCommmentFromChangelogInfo() throws Exception {
        Mockito.when(releaseDao.getReleasesByPipeLaunchIds(Mockito.any())).thenReturn(Collections.emptyList());
        sutBuilder = sutBuilder.withResources(
            new ChangelogInfo(changelogEntryList.subList(0, 3)),
            new ChangelogInfo(changelogEntryList.subList(3, 6))
        );

        sutBuilder.create().execute(new TestTsumJobContext(null, releaseDao, "me"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(releaseIssue, Mockito.times(1)).comment(captor.capture());

        Assert.assertEquals(
            "Изменения:\n" +
                "* 11 (@user42) %%change11%%\n" +
                "* 12 (@user42) %%change12%%\n" +
                "* 13 (@user42) %%change13%%\n" +
                "* 21 (@user42) %%change21%%\n" +
                "* 22 (@user42) %%change22%%\n" +
                "* 23 (@user42) %%change23%%",
            captor.getValue()
        );
    }

    @Test
    public void testCommmentFromConductorPackage() throws Exception {
        Mockito.when(releaseDao.getReleasesByPipeLaunchIds(Mockito.any())).thenReturn(Collections.emptyList());
        String fullChangelog = "fullChange1\nfullChange2\nfullChange3";
        sutBuilder = sutBuilder.withResources(
            new ConductorPackage("package1", VERSION, fullChangelog),
            new ConductorPackage("package1", VERSION, "", changelogEntryList)
        );

        sutBuilder.create().execute(new TestTsumJobContext(null, releaseDao, "me"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(releaseIssue, Mockito.times(2)).comment(captor.capture());

        Assert.assertEquals(
            "Изменения:\n" +
                "* 11 (@user42) %%change11%%\n" +
                "* 12 (@user42) %%change12%%\n" +
                "* 13 (@user42) %%change13%%\n" +
                "* 21 (@user42) %%change21%%\n" +
                "* 22 (@user42) %%change22%%\n" +
                "* 23 (@user42) %%change23%%\n" +
                "* 24 (@user42) %%change24%%",
            captor.getAllValues().get(1)
        );

        Assert.assertEquals(
            "Изменения:\n" + fullChangelog,
            captor.getAllValues().get(0)
        );
    }

    @Test
    public void testRemovingEscapingCharsFromTicketName() throws Exception {
        Mockito.when(releaseDao.getReleasesByPipeLaunchIds(Mockito.any())).thenReturn(Collections.emptyList());
        sutBuilder = sutBuilder.withResources(
                new ChangelogInfo(List.of(
                        createChangelogEntry(11, "chan\"\"\"ge1~~1~~\\")
                ))
        );

        sutBuilder.create().execute(new TestTsumJobContext(null, releaseDao, "me"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(releaseIssue, Mockito.times(1)).comment(captor.capture());

        Assert.assertEquals(
                "Изменения:\n" +
                        "* 11 (@user42) %%chan\"ge1~~1%%",
                captor.getValue()
        );
    }

    private ChangelogEntry createChangelogEntry(int revision, String change) {
        return new ChangelogEntry(Integer.toString(revision), revision, change, USER, null);
    }

    private Release createRelease(String id, String title, String revision, Instant commitDate) {
        return Release.builder()
            .withId(id)
            .withProjectId(PROJECT_ID)
            .withPipeId(PIPE_ID)
            .withPipeLaunchId(PIPE_LAUNCH_ID)
            .withTitle(title)
            .withCommit(revision, commitDate)
            .build();
    }
}
