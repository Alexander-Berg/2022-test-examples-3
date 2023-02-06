package ru.yandex.market.tsum.pipelines.market_infra.jobs;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.teamcity.BuildAgent;
import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.clients.teamcity.BuildItemsPage;
import ru.yandex.market.tsum.clients.teamcity.TeamcityClient;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 29.01.18
 */
public final class TestAgentsRegression implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(TestAgentsRegression.class);

    private static final int MARKET_INFRA_PRESTABLE_POOL_ID = 190;
    private static final int MARKET_INFRA_STABLE_POOL_ID = 146;

    private static final Duration TEST_PERIOD = Duration.ofHours(6);
    private static final Duration POLL_INTERVAL = Duration.ofMinutes(5);

    private static final int NUMBER_OF_BUILDS_TO_CHECK = 50;
    private static final int ALLOWED_FAILURE_PERCENT_DEVIATION = 10; // 0..100

    @Autowired
    private TeamcityClient teamcity;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("1753430d-0236-4069-acce-efc7ea55c72d");
    }

    @Override
    public void execute(JobContext context) throws Exception {
        checkBuilds(context, Instant.now());
    }

    private void checkBuilds(JobContext context, Instant since) throws Exception {
        List<BuildAgent> stablePool = teamcity.getAgentPool(MARKET_INFRA_STABLE_POOL_ID).get().getBuildAgents();
        List<BuildAgent> prestablePool = teamcity.getAgentPool(MARKET_INFRA_PRESTABLE_POOL_ID).get().getBuildAgents();

        PoolsStatistics poolsStatistics = Poller
            .poll(() -> collectStatistics(context, stablePool, prestablePool, since))
            .canStopWhen(stats ->
                stats.prestable.total >= NUMBER_OF_BUILDS_TO_CHECK && stats.stable.total >= NUMBER_OF_BUILDS_TO_CHECK
            )
            .interval(POLL_INTERVAL.getSeconds(), TimeUnit.SECONDS)
            .timeout(TEST_PERIOD.getSeconds(), TimeUnit.SECONDS)
            .run();

        BuildsStatistics prestableStats = poolsStatistics.prestable;
        BuildsStatistics stableStats = poolsStatistics.stable;

        log.info("Poll completed. Collected statistic " +
                "about {} prestable builds, successful: {}, failed: {}({}%) and " +
                "about {} stable builds, successful: {}, failed: {}({}%).",
            prestableStats.total, prestableStats.successful, prestableStats.failed, prestableStats.failurePercent,
            stableStats.total, stableStats.successful, stableStats.failed, stableStats.failurePercent
        );

        Preconditions.checkArgument(
            poolsStatistics.prestable.total >= NUMBER_OF_BUILDS_TO_CHECK,
            "There are not enough prestable builds for statistic calculation");

        Preconditions.checkArgument(
            poolsStatistics.stable.total >= NUMBER_OF_BUILDS_TO_CHECK,
            "There are not enough stable builds for statistic calculation");

        context.progress().update(progress -> progress.setText(String.format("prestable/stable: %d%% <= %d%% + %d%%",
            prestableStats.failurePercent, stableStats.failurePercent, ALLOWED_FAILURE_PERCENT_DEVIATION)));

        Preconditions.checkArgument(
            prestableStats.failurePercent <= stableStats.failurePercent + ALLOWED_FAILURE_PERCENT_DEVIATION,
            String.format("Prestable agents have unacceptable rate of failed builds. " +
                    "Expected less then %d%%. Actual: %d%%.",
                stableStats.failurePercent + ALLOWED_FAILURE_PERCENT_DEVIATION, prestableStats.failurePercent));
    }

    private PoolsStatistics collectStatistics(JobContext context,
                                              List<BuildAgent> stableAgents,
                                              List<BuildAgent> prestableAgents,
                                              Instant since) throws Exception {
        BuildsStatistics prestableStats = collectBuildStatistics(prestableAgents, since);
        BuildsStatistics stableStats = collectBuildStatistics(stableAgents, since);

        log.info("Collected statistic about {} prestable builds, {}% failed and {} stable builds, {}% failed since {}",
            prestableStats.total, prestableStats.failurePercent, stableStats.total, stableStats.failurePercent, since);

        context.progress().update(progress -> progress.setText(String.format("prestbl:%d(%d%%), stbl:%d(%d%%)",
            prestableStats.total, prestableStats.failurePercent, stableStats.total, stableStats.failurePercent)));

        return new PoolsStatistics(prestableStats, stableStats);
    }


    @Override
    public void recover(JobContext context) throws Exception {
        JobLaunch lastLaunch = context.getJobState().getLastLaunch();

        if (lastLaunch == null || lastLaunch.getStatusHistory().isEmpty()) {
            log.info("No information about previous launch found");
            execute(context);
        } else {
            checkBuilds(context, lastLaunch.getStatusHistory().get(0).getDate().toInstant());
        }
    }

    private BuildsStatistics collectBuildStatistics(List<BuildAgent> buildAgents, Instant since) throws Exception {
        int successful = 0;
        int total = 0;

        for (BuildAgent buildAgent : buildAgents) {
            BuildItemsPage builds = teamcity.getBuilds(
                TeamcityClient.BuildLocator.newBuilder()
                    .withAgentName(buildAgent.getName())
                    .withSinceDate(since)
                    .build()
            ).get();

            do {
                total += builds.getBuilds().size();
                successful += builds.getBuilds().stream()
                    .filter(build -> build.getStatus() == BuildItem.Status.SUCCESS)
                    .count();

                builds = teamcity.getNextBuildItemsPage(builds).get();
            } while (builds != null && total < NUMBER_OF_BUILDS_TO_CHECK);
        }

        return new BuildsStatistics(total, successful);
    }

    private static class PoolsStatistics {
        private final BuildsStatistics prestable;
        private final BuildsStatistics stable;

        PoolsStatistics(BuildsStatistics prestable, BuildsStatistics stable) {
            this.prestable = prestable;
            this.stable = stable;
        }
    }

    private static class BuildsStatistics {
        private final int total;
        private final int failed;
        private final int successful;
        private final int failurePercent;

        private BuildsStatistics(int total, int successful) {
            this.total = total;
            this.successful = successful;
            this.failed = total - successful;
            this.failurePercent = toPercent(failed, total);
        }

        private int toPercent(int value, int total) {
            return total == 0 ? 0 : Math.round((float) value / total * 100.0f);
        }
    }
}
