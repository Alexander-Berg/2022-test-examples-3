package ru.yandex.market.tsum.tms.tasks.github;

import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.service.IssueService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.github.model.ExtendedPullRequest;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.PerCommitLaunchType;
import ru.yandex.market.tsum.entity.project.PerCommitSettingsEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 16.08.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class PerCommitStateCleanupCronTaskTest {
    private static final String REPOSITORY_ID = "market-infra/test";
    private static final String PROJECT_ID = "test";
    private static final String BRANCH_NAME = "develop";
    private static final String BRANCH_NAME_2 = "develop-2";
    private static final String OTHER_BRANCH_NAME = "other";
    private static final String PIPELINE_ID = "my-pipeline";
    private static final String PIPELINE_2_ID = "my-pipeline-2";

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private PerCommitBranchStateDao stateDao;

    @Mock
    private PerCommitBranchStateHistoryDao stateHistoryDao;

    @Mock
    private ProjectsDao projectsDao;

    private ProjectEntity projectEntity = new ProjectEntity();
    private ExtendedPullRequest pullRequest = new ExtendedPullRequest();

    @Before
    public void setup() {
        PerCommitSettingsEntity settings = new PerCommitSettingsEntity();
        settings.setPattern("(feature/.+)|(develop)");
        settings.setRepository(REPOSITORY_ID);
        settings.setPipeline(PIPELINE_ID);

        projectEntity.setId(PROJECT_ID);
        projectEntity.setPerCommitSettings(Collections.singletonList(settings));

        when(projectsDao.list())
            .thenReturn(Collections.singletonList(projectEntity));


        when(gitHubClient.getBranches(eq(REPOSITORY_ID)))
            .thenReturn(Collections.singletonList(new Branch(BRANCH_NAME)));

        initPullRequest();
    }

    private void initPullRequest() {
        pullRequest.setMergeCommitSha("m1");
        pullRequest.setId(1);
        pullRequest.setTitle("Test");

        PullRequestMarker base = new PullRequestMarker();
        base.setSha("b1");
        base.setRef(BRANCH_NAME);
        pullRequest.setBase(base);

        PullRequestMarker head = new PullRequestMarker();
        head.setSha("h1");
        head.setRef(OTHER_BRANCH_NAME);
        pullRequest.setHead(head);
    }

    @Test
    public void deletesWhenBranchIsDeleted() {
        PerCommitStateCleanupCronTask task = new PerCommitStateCleanupCronTask(
            gitHubClient, stateDao, stateHistoryDao, projectsDao
        );

        PerCommitBranchStateEntity actualState1 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitBranchStateEntity actualState2 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME_2)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitBranchStateEntity oldState = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME_2)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();
        oldState.setUpdated(
            Instant.now()
                .minus(PerCommitStateCleanupCronTask.STATE_LIFETIME_DAYS, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.SECONDS)
        );

        when(stateDao.getByProjectId(eq(projectEntity.getId())))
            .thenReturn(
                Arrays.asList(actualState1, actualState2, oldState)
            );

        task.execute(null);

        verify(stateDao, times(1)).delete(Mockito.any());
    }

    @Test
    public void deletesWhenBranchIsDeletedForPullRequests() {
        when(gitHubClient.getPullRequestsExtended(eq(REPOSITORY_ID), eq(IssueService.STATE_OPEN)))
            .thenReturn(Collections.singletonList(pullRequest));

        PerCommitSettingsEntity settings = new PerCommitSettingsEntity();
        settings.setPattern(BRANCH_NAME);
        settings.setRepository(REPOSITORY_ID);
        settings.setPipeline(PIPELINE_2_ID);
        settings.setLaunchType(PerCommitLaunchType.PULL_REQUEST);

        PerCommitStateCleanupCronTask task = new PerCommitStateCleanupCronTask(
            gitHubClient, stateDao, stateHistoryDao, projectsDao
        );

        PerCommitBranchStateEntity actualState1 =
            PerCommitBranchStateEntity.builder()
                .withProject(PROJECT_ID)
                .withLaunchSetting(null)
                .withRepository(REPOSITORY_ID)
                .withBranch(OTHER_BRANCH_NAME)
                .withMultitesting(false)
                .withPipelineId(PIPELINE_2_ID)
                .withPullRequestId(1)
                .withPullRequestNumber(1)
                .withStatusCheckName(null)
                .build();

        PerCommitBranchStateEntity actualState2 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME + "2")
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitBranchStateEntity oldState = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME + "2")
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();
        oldState.setUpdated(
            Instant.now()
                .minus(PerCommitStateCleanupCronTask.STATE_LIFETIME_DAYS, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.SECONDS)
        );

        projectEntity.setPerCommitSettings(Collections.singletonList(settings));
        when(stateDao.getByProjectId(eq(projectEntity.getId())))
            .thenReturn(
                Arrays.asList(actualState1, actualState2, oldState)
            );

        task.execute(null);

        verify(stateDao, times(1)).delete(Mockito.any());
    }

    @Test
    public void doNotDeleteWhenBranchFoundByAnotherPattern() {
        when(gitHubClient.getBranches(eq(REPOSITORY_ID)))
            .thenReturn(Arrays.asList(new Branch(BRANCH_NAME), new Branch(OTHER_BRANCH_NAME)));

        PerCommitSettingsEntity settings = new PerCommitSettingsEntity();
        settings.setPattern(OTHER_BRANCH_NAME);
        settings.setRepository(REPOSITORY_ID);
        settings.setPipeline(PIPELINE_2_ID);

        PerCommitStateCleanupCronTask task = new PerCommitStateCleanupCronTask(
            gitHubClient, stateDao, stateHistoryDao, projectsDao
        );

        List<PerCommitSettingsEntity> settingsList = new ArrayList<>(projectEntity.getPerCommitSettings());
        settingsList.add(settings);

        projectEntity.setPerCommitSettings(settingsList);

        PerCommitBranchStateEntity actualState = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(OTHER_BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_2_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitBranchStateEntity oldState = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(OTHER_BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_2_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();
        oldState.setUpdated(
            Instant.now()
                .minus(PerCommitStateCleanupCronTask.STATE_LIFETIME_DAYS, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.SECONDS)
        );


        when(stateDao.getByProjectId(eq(projectEntity.getId())))
            .thenReturn(Arrays.asList(actualState, oldState));

        task.execute(null);

        verify(stateDao, never()).delete(Mockito.any());
    }
}
