package ru.yandex.market.tsum.pipelines.ott.jobs.testing;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.pollers.PollerOptions;
import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.clients.teamcity.TeamcityBuilder;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.definition.job.ExecutorInfo;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.AbstractTeamcityBuildJob;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.ott.resources.TeamCityBuildInfo;
import ru.yandex.market.tsum.release.dao.FinishCause;

@ExecutorInfo(
    title = "OTT TeamCity trunk build job",
    description = "Джоба для сборки trunk на TeamCity"
)
@Produces(single = TeamCityBuildInfo.class)
public class OttTeamCityBuildTrunkJob extends AbstractTeamcityBuildJob {
    private static final String BUILD_IMAGE_SHA = "build.image.sha";

    @WiredResource
    private TeamcityBuildConfig teamcityBuildConfig;

    @Autowired
    private TeamcityBuilder teamcityBuilder;

    @Override
    protected TeamcityBuildConfig getTeamcityConfig(JobContext context) {
        return TeamcityBuildConfig.builder(teamcityBuildConfig)
            .withVcsRevision(deliveryPipelineParams.getRevision())
            .build();
    }

    @Override
    protected void onSuccess(JobContext context, BuildItem finishedQueueItem) {
        TeamcityBuildConfig config = TeamcityBuildConfig.builder().withArtifactPath(BUILD_IMAGE_SHA).build();
        String buildSha = teamcityBuilder.getArtifact(finishedQueueItem, config);
        context.resources().produce(new TeamCityBuildInfo(finishedQueueItem.getNumber(), buildSha));
    }

    @Override
    protected void onFailure(JobContext context, BuildItem queueItem) {
        cancelRelease(context, "TeamCity build is failed");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        pullTrunkBuild(context);
    }

    @Override
    public void recover(JobContext context) throws Exception {
        pullTrunkBuild(context);
    }

    @Override
    public boolean interrupt(JobContext context, Thread executorThread) {
        return true;
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("a5098153-4773-4fd4-83be-0ecc84eaea79");
    }

    private void pullTrunkBuild(JobContext context) throws Exception {
        BuildItem trunkBuild = findTrunkBuild(context);
        pollAndProcessResult(context, trunkBuild);
    }

    private BuildItem findTrunkBuild(JobContext context) throws Exception {
        try {
            return Poller.pollOptional(
                    () -> teamcityBuilder.findBuildsByRevision(
                            teamcityBuildConfig.getJobName(),
                            String.valueOf(deliveryPipelineParams.getRevision())
                        )
                        .stream()
                        .max(Comparator.comparing(BuildItem::getNumber))
                ).pollerOptions(
                    PollerOptions.builder()
                        .timeout(teamcityBuildConfig.getTimeoutMinutes(), TimeUnit.MINUTES)
                        .interval(TeamcityBuilder.BUILD_POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                )
                .run()
                .get();
        } catch (TimeoutException e) {
            cancelRelease(context, "TeamCity build is not found");
            context.actions().failJob(
                String.format("Could not find trunk build for revision %s", deliveryPipelineParams.getRevision()),
                SupportType.NONE
            );
            throw e;
        }
    }

    private void cancelRelease(JobContext context, String reason) {
        if (context instanceof TsumJobContext) {
            ((TsumJobContext) context).release().cancel(
                FinishCause.cancelledManually("robot-ott-teamcity"),
                reason
            );
        }
    }
}
