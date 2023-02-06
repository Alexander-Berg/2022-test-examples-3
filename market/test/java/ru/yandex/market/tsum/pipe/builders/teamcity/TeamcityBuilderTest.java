package ru.yandex.market.tsum.pipe.builders.teamcity;

import java.util.Collections;
import java.util.Objects;

import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.clients.teamcity.TeamcityBuild;
import ru.yandex.market.tsum.clients.teamcity.TeamcityBuilder;
import ru.yandex.market.tsum.clients.teamcity.TeamcityClient;
import ru.yandex.market.tsum.clients.teamcity.VcsChange;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildConfig;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.01.18
 */
public class TeamcityBuilderTest {
    private static final int VCS_CHANGE_ID = 42;
    private static final String VCS_REVISION = "e6bfa14c550c3065fb7508f0cc434e0c182db816";
    private static final String BUILD_TYPE_ID = "MarketInfra_Build";

    private final TeamcityClient teamcityClientMock = Mockito.mock(TeamcityClient.class);
    private final TeamcityBuilder teamcityBuilder = new TeamcityBuilder(teamcityClientMock);

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void enqueueBuildUsingVcsRevision() throws Exception {

        JobContext jobContextMock = Mockito.mock(JobContext.class);
        JobProgressContext jobProgress = Mockito.mock(JobProgressContext.class);
        Mockito.when(jobContextMock.progress()).thenReturn(jobProgress);

        // arrange
        Mockito.when(teamcityClientMock.getVcsChange(BUILD_TYPE_ID, VCS_REVISION))
            .thenReturn(Futures.immediateFuture(new VcsChange(VCS_CHANGE_ID, VCS_REVISION, null, null)));

        BuildItem buildItem = Mockito.mock(BuildItem.class);
        Mockito.when(buildItem.getHref()).thenReturn("http://teamcity/some/build");
        Mockito.when(
            teamcityClientMock.queueBuild(argThat(this::matches))
        ).thenReturn(Futures.immediateFuture(buildItem));

        TeamcityBuildConfig config = TeamcityBuildConfig.builder()
            .withJobName(BUILD_TYPE_ID)
            .withVcsRevision(VCS_REVISION)
            .build();

        // act
        teamcityBuilder.enqueueBuild(config, jobContextMock);

        // assert
        Mockito.verify(teamcityClientMock).queueBuild(argThat(this::matches));
    }

    private boolean matches(TeamcityBuild build) {
        return Objects.equals(build.getBuildTypeId(), BUILD_TYPE_ID) &&
            Objects.equals(build.getParameters(), Collections.emptyMap()) &&
            Objects.equals(build.getTags(), Collections.emptyList()) &&
            Objects.equals(build.getVcsChangeId(), VCS_CHANGE_ID);
    }
}
