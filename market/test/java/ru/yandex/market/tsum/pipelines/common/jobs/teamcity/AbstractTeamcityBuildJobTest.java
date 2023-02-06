package ru.yandex.market.tsum.pipelines.common.jobs.teamcity;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.clients.teamcity.TeamcityBuilder;
import ru.yandex.market.tsum.clients.teamcity.TeamcityClient;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 02.08.17
 */
public class AbstractTeamcityBuildJobTest {
    private static final String ARTIFACT_CONTENT = "artifact";

    private TeamcityClient client;
    private TeamcityBuildConfig config;
    private TestJobContext jobContext;

    @Before
    public void setUp() throws Exception {
        client = mock(TeamcityClient.class);
        config = TeamcityBuildConfig.builder()
            .withTimeoutMinutes(1)
            .withJobName("build_job_name")
            .withArtifactPath("/foo.props")
            .withParameter("teamcity.build.branch", "master")
            .build();
        jobContext = new TestJobContext();
    }

    @Test
    public void execute() throws Exception {
        BuildItem queueItem = queueItem();

        when(client.queueBuild(argThat(build -> Objects.equals(config.getJobName(), build.getBuildTypeId()))))
            .thenReturn(Futures.immediateFuture(queueItem));

        when(client.getBuildStatus(queueItem)).thenReturn(
            Futures.immediateFuture(queueItem()),
            Futures.immediateFuture(runningItem()),
            Futures.immediateFuture(successfulItem())
        );

        when(client.getArtifactContent(any(), eq(config.getArtifactPath())))
            .thenReturn(Futures.immediateFuture(ARTIFACT_CONTENT));

        TestJobContext testJobContext = new TestJobContext();
        JobState jobState = Mockito.mock(JobState.class);
        JobLaunch jobLaunch = Mockito.mock(JobLaunch.class);
        Mockito.when(jobState.getLastLaunch()).thenReturn(jobLaunch);
        testJobContext.setJobStateMock(jobState);
        Sut sut = createSut(config);
        sut.execute(testJobContext);

        Assert.assertEquals(ARTIFACT_CONTENT, testJobContext.getResource(Res1.class).getS());
    }

    @Test
    public void recover() throws Exception {
        BuildItem runningItem = runningItem();
        when(client.getBuild(eq(config.getJobName()), any(), any()))
            .thenReturn(Futures.immediateFuture(Collections.singletonList(runningItem)));

        when(client.getBuildStatus(runningItem)).thenReturn(
            Futures.immediateFuture(runningItem()),
            Futures.immediateFuture(successfulItem())
        );

        when(client.getArtifactContent(any(), eq(config.getArtifactPath())))
            .thenReturn(Futures.immediateFuture(ARTIFACT_CONTENT));

        Sut sut = createSut(config);
        sut.recover(jobContext);

        Assert.assertEquals(ARTIFACT_CONTENT, jobContext.getResource(Res1.class).getS());
    }

    private Sut createSut(TeamcityBuildConfig config) {
        Sut sut = new Sut(config);
        TeamcityBuilder teamcityBuilder = new TeamcityBuilder(client);
        sut.setTeamcityBuilder(teamcityBuilder);
        return sut;
    }

    private BuildItem runningItem() {
        BuildItem queueItem = new BuildItem();
        queueItem.setState(BuildItem.State.RUNNING);
        return queueItem;
    }

    private BuildItem queueItem() {
        BuildItem queueItem = new BuildItem();
        queueItem.setState(BuildItem.State.QUEUED);
        queueItem.setHref("dummy href");
        return queueItem;
    }

    private BuildItem successfulItem() {
        BuildItem queueItem = new BuildItem();
        queueItem.setState(BuildItem.State.FINISHED);
        queueItem.setStatus(BuildItem.Status.SUCCESS);
        return queueItem;
    }

    static class Sut extends AbstractTeamcityBuildJob implements JobExecutor {
        private final TeamcityBuildConfig teamcityConfig;

        Sut(TeamcityBuildConfig teamcityConfig) {
            this.teamcityConfig = teamcityConfig;
        }

        @Override
        protected TeamcityBuildConfig getTeamcityConfig(JobContext context) {
            return teamcityConfig;
        }

        @Override
        protected void onSuccess(JobContext context, BuildItem finishedQueueItem) {
            super.onSuccess(context, finishedQueueItem);

            String artifactContent = getTeamcityBuilder().getArtifact(finishedQueueItem, teamcityConfig);
            context.resources().produce(new Res1(artifactContent));
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("7ce3d42c-9099-4a6f-94cd-cfc3985e2fbe");
        }
    }
}
