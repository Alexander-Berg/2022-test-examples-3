package ru.yandex.market.tsum.pipelines.common.jobs.delivery;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.release.dao.GitHubSettings;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineStateDao;
import ru.yandex.market.tsum.test_data.TestProjectFactory;
import ru.yandex.market.tsum.test_data.TestRepositoryCommitFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.05.18
 */
public class ChangelogServiceTest {
    private static final String MACHINE_ID = "machine-id";
    private static final ObjectId PIPE_LAUNCH_ID = new ObjectId();
    private static final String PROJECT_ID = "some-project";
    private static final String PIPE_ID = "some-pipe";
    private static final Release.Builder RELEASE_BUILDER = Release.builder()
        .withProjectId(PROJECT_ID)
        .withPipeId(PIPE_ID);
    private static final Release RELEASE = RELEASE_BUILDER.withPipeLaunchId(PIPE_LAUNCH_ID.toString()).build();

    private static final List<ChangelogEntry> CHANGELOG_ENTRIES = Arrays.asList(
        changelogEntry(1, "1"),
        changelogEntry(2, "2"),
        changelogEntry(3, "3")
    );

    private static final ChangelogInfo CHANGELOG_INFO = new ChangelogInfo(CHANGELOG_ENTRIES);

    private DeliveryMachineStateDao deliveryMachineStateDao;
    private GitHubClient gitHubClient;
    private BitbucketClient bitbucketClient;
    private JobContext jobContext;
    private PipeLaunch pipeLaunch;
    private ReleaseDao releaseDao;
    private ProjectsDao projectsDao;

    private ChangelogService sut;

    @Before
    public void setUp() {
        ProjectEntity project = TestProjectFactory.project(PROJECT_ID, MACHINE_ID, PIPE_ID);
        GitHubSettings gitHubSettings = (GitHubSettings) (project.getDeliveryMachine(PIPE_ID).getVcsSettings().get());

        deliveryMachineStateDao = mock(DeliveryMachineStateDao.class);
        when(deliveryMachineStateDao.getStableRevision(MACHINE_ID)).thenReturn("1");

        gitHubClient = mock(GitHubClient.class);
        bitbucketClient = mock(BitbucketClient.class);

        RepositoryCommit firstCommit = gitHubCommit("1", 1);
        when(gitHubClient.getCommit(gitHubSettings.getRepositoryFullName(), firstCommit.getSha()))
            .thenReturn(firstCommit);

        RepositoryCommit secondCommit = gitHubCommit("2", 2);
        when(gitHubClient.getCommit(gitHubSettings.getRepositoryFullName(), secondCommit.getSha()))
            .thenReturn(secondCommit);

        pipeLaunch = mock(PipeLaunch.class);
        when(pipeLaunch.getId()).thenReturn(PIPE_LAUNCH_ID);
        when(pipeLaunch.getStageGroupId()).thenReturn(MACHINE_ID);
        when(pipeLaunch.isStaged()).thenReturn(true);
        when(pipeLaunch.getPipeId()).thenReturn(PIPE_ID);

        jobContext = mock(JobContext.class);
        when(jobContext.getPipeLaunch()).thenReturn(pipeLaunch);

        releaseDao = mock(ReleaseDao.class);
        when(releaseDao.getReleaseByPipeLaunchId(PIPE_LAUNCH_ID)).thenReturn(RELEASE);

        projectsDao = mock(ProjectsDao.class);
        when(projectsDao.get(PROJECT_ID)).thenReturn(project);

        sut = new ChangelogService(deliveryMachineStateDao, gitHubClient, bitbucketClient, releaseDao, projectsDao);
    }

    @Test
    public void shouldReturnTheSameChangelog_IfPipelineIsNotStaged() {
        when(pipeLaunch.isStaged()).thenReturn(false);
        when(pipeLaunch.getStageGroupId()).thenReturn(null);

        List<ChangelogInfo> actualChangelogInfoList = sut.getActualChangelog(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        Assert.assertEquals(
            CHANGELOG_ENTRIES,
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    @Test
    public void shouldReturnTheSameChangelog_IfThereIsNoStableRevision() {
        when(deliveryMachineStateDao.getStableRevision(MACHINE_ID)).thenReturn(null);

        List<ChangelogInfo> actualChangelogInfoList = sut.getActualChangelog(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        Assert.assertEquals(
            CHANGELOG_ENTRIES,
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    @Test
    public void gitHub_ShouldTakeCommitsThatAreLaterThanStableOne() {
        List<ChangelogInfo> actualChangelogInfoList = sut.getActualChangelog(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        Assert.assertEquals(
            CHANGELOG_ENTRIES.subList(1, CHANGELOG_ENTRIES.size()),
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    @Test
    public void arcadia_ShouldTakeCommitsThatAreLaterThanStableOne() {
        List<ChangelogInfo> actualChangelogInfoList = sut.getActualChangelog(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        Assert.assertEquals(
            CHANGELOG_ENTRIES.subList(1, CHANGELOG_ENTRIES.size()),
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    @Test
    public void getOptimisticActualChangelog_ShouldTakeChangelogFromStableRevision_IfNoPreviousRunningReleases() {
        List<ChangelogInfo> actualChangelogInfoList = sut.getChangelogStartingFromPreviousRunningRelease(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        when(releaseDao.getPreviousRunningRelease(RELEASE)).thenReturn(null);

        Assert.assertEquals(
            CHANGELOG_ENTRIES.subList(1, CHANGELOG_ENTRIES.size()),
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    @Test
    public void getOptimisticActualChangelog_GitHub_ShouldTakeChangelogFromLastRunningRelease_IfSuchExists() {
        when(releaseDao.getPreviousRunningRelease(RELEASE))
            .thenReturn(RELEASE_BUILDER.withCommit("2", Instant.ofEpochMilli(2)).build());

        List<ChangelogInfo> actualChangelogInfoList = sut.getChangelogStartingFromPreviousRunningRelease(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        Assert.assertEquals(
            CHANGELOG_ENTRIES.subList(2, CHANGELOG_ENTRIES.size()),
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    @Test
    public void getOptimisticActualChangelog_Arcadia_ShouldTakeChangelogFromLastRunningRelease_IfSuchExists() {
        when(releaseDao.getPreviousRunningRelease(RELEASE))
            .thenReturn(RELEASE_BUILDER.withCommit("2", Instant.ofEpochMilli(2)).build());

        List<ChangelogInfo> actualChangelogInfoList = sut.getChangelogStartingFromPreviousRunningRelease(
            pipeLaunch, Collections.singletonList(CHANGELOG_INFO)
        );

        Assert.assertEquals(
            CHANGELOG_ENTRIES.subList(2, CHANGELOG_ENTRIES.size()),
            actualChangelogInfoList.get(0).getChangelogEntries()
        );
    }

    private static ChangelogEntry changelogEntry(int timestampSeconds, String revision) {
        return new ChangelogEntry(revision, timestampSeconds, "change " + revision, "", "");
    }

    private RepositoryCommit gitHubCommit(String revision, int timestampSeconds) {
        return TestRepositoryCommitFactory.commit(
            revision, null, null, new Date(TimeUnit.SECONDS.toMillis(timestampSeconds))
        );
    }
}
